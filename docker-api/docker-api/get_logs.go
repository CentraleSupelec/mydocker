package main

import (
	"context"
	"io"
	"sort"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	log "github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// logCollectorDockerClient is the slice of the Docker API that collecting logs
// needs. It exists so the collection rules can be tested without a daemon.
type logCollectorDockerClient interface {
	ServiceList(ctx context.Context, options types.ServiceListOptions) ([]swarm.Service, error)
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
	TaskLogs(ctx context.Context, taskID string, options types.ContainerLogsOptions) (io.ReadCloser, error)
	NodeInspectWithRaw(ctx context.Context, nodeID string) (swarm.Node, []byte, error)
}

func (s *server) GetLogs(ctx context.Context, request *pb.LogRequest) (*pb.LogResponse, error) {
	return collectLogs(
		ctx, s.dockerClient, request.GetUserID(), request.GetCourseID(), c.LogsTailLines, c.LogsMaxTasks,
	)
}

func collectLogs(
	ctx context.Context,
	dockerClient logCollectorDockerClient,
	userID string,
	courseID string,
	tailLines int,
	maxTasks int,
) (*pb.LogResponse, error) {
	service, err := findServiceFor(ctx, dockerClient, userID, courseID)
	if err != nil {
		return nil, err
	}

	taskFilters := filters.NewArgs()
	taskFilters.Add("service", service.ID)
	tasks, err := dockerClient.TaskList(ctx, types.TaskListOptions{Filters: taskFilters})
	if err != nil {
		return nil, err
	}

	// Ascending by slot, then by creation time, so a restart of the same slot
	// reads oldest attempt first. The order is fixed here rather than in the
	// back end or the front end, because a repeated field preserves it.
	sort.SliceStable(tasks, func(i, j int) bool {
		if tasks[i].Slot != tasks[j].Slot {
			return tasks[i].Slot < tasks[j].Slot
		}
		return tasks[i].CreatedAt.Before(tasks[j].CreatedAt)
	})

	// One request reads one log stream per task, so the number of tasks bounds
	// its latency. Swarm keeps terminated tasks per slot, and an environment that
	// has crash-looped can hold many, so the oldest are dropped and counted
	// rather than read.
	omitted := 0
	if maxTasks > 0 && len(tasks) > maxTasks {
		omitted = len(tasks) - maxTasks
		tasks = tasks[omitted:]
	}

	nodeNames := make(map[string]string)
	taskLogs := make([]*pb.TaskLog, 0, len(tasks))

	for _, task := range tasks {
		entry := &pb.TaskLog{
			TaskID:    task.ID,
			Slot:      uint32(task.Slot),
			Node:      resolveNodeName(ctx, dockerClient, task, nodeNames),
			CreatedAt: timestamppb.New(task.CreatedAt),
		}

		reader, err := dockerClient.TaskLogs(ctx, task.ID, types.ContainerLogsOptions{
			ShowStdout: true,
			ShowStderr: true,
			Follow:     false,
			Timestamps: c.LogsTimestamps,
			Details:    c.LogsDetails,
		})
		if err != nil {
			// Reported rather than skipped: "we could not read this" and "this
			// printed nothing" are different answers.
			log.Warnf("failed to read logs of task %s: %v", task.ID, err)
			entry.ReadError = err.Error()
			taskLogs = append(taskLogs, entry)
			continue
		}

		logs, truncated, err := readTaskLogs(reader, tailLines)
		_ = reader.Close()
		if err != nil {
			log.Warnf("failed to decode logs of task %s: %v", task.ID, err)
			entry.ReadError = err.Error()
			taskLogs = append(taskLogs, entry)
			continue
		}

		// A task that printed nothing is noise in a modal; a failed task with
		// output is usually the reason the modal was opened.
		if logs == "" {
			continue
		}

		entry.Logs = logs
		entry.Truncated = truncated
		taskLogs = append(taskLogs, entry)
	}

	return &pb.LogResponse{
		Name:         service.Spec.Name,
		Image:        service.Spec.TaskTemplate.ContainerSpec.Image,
		Logs:         taskLogs,
		OmittedTasks: uint32(omitted),
	}, nil
}

// resolveNodeName falls back to the node id, which is still more use than an
// empty header, and caches per call because several tasks share a node.
func resolveNodeName(
	ctx context.Context,
	dockerClient logCollectorDockerClient,
	task swarm.Task,
	cache map[string]string,
) string {
	if task.NodeID == "" {
		return ""
	}
	if name, cached := cache[task.NodeID]; cached {
		return name
	}

	name := task.NodeID
	if node, _, err := dockerClient.NodeInspectWithRaw(ctx, task.NodeID); err == nil {
		if node.Description.Hostname != "" {
			name = node.Description.Hostname
		} else if node.Spec.Name != "" {
			name = node.Spec.Name
		}
	}
	cache[task.NodeID] = name
	return name
}
