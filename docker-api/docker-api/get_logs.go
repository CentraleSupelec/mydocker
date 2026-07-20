package main

import (
	"context"
	"strings"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
)

func (s *server) GetLogs(ctx context.Context, request *pb.LogRequest) (*pb.LogResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)
	service, err := dockerUtils.findService(ctx, request.GetUserID(), request.GetCourseID())
	if err != nil {
		return nil, err
	}

	taskFilters := filters.NewArgs()
	taskFilters.Add("service", service.ID)
	tasks, err := s.dockerClient.TaskList(ctx, types.TaskListOptions{Filters: taskFilters})
	if err != nil {
		return nil, err
	}

	nodeLogsMap := make(map[string]string)
	nodeNameCache := make(map[string]string)

	for _, task := range tasks {
		if task.NodeID == "" {
			continue
		}

		nodeName, cached := nodeNameCache[task.NodeID]
		if !cached {
			node, _, err := s.dockerClient.NodeInspectWithRaw(ctx, task.NodeID)
			if err != nil {
				nodeName = task.NodeID
			} else {
				nodeName = node.Description.Hostname
				if nodeName == "" {
					nodeName = node.Spec.Name
				}
				if nodeName == "" {
					nodeName = task.NodeID
				}
			}
			nodeNameCache[task.NodeID] = nodeName
		}

		reader, err := s.dockerClient.TaskLogs(ctx, task.ID, types.ContainerLogsOptions{
			ShowStdout: true,
			ShowStderr: true,
			Follow:     false,
			Timestamps: c.LogsTimestamps,
			Details:    c.LogsDetails,
		})
		if err != nil {
			continue
		}

		taskLogs, err := readLogs(reader)
		reader.Close()
		if err != nil {
			continue
		}
		taskLogs = strings.TrimSpace(taskLogs)

		if taskLogs == "" {
			continue
		}

		if existingLogs, exists := nodeLogsMap[nodeName]; exists && existingLogs != "" {
			nodeLogsMap[nodeName] = existingLogs + "\n--- Task Failover / Restart ---\n" + taskLogs
		} else {
			nodeLogsMap[nodeName] = taskLogs
		}
	}

	return &pb.LogResponse{Logs: nodeLogsMap, Name: service.Spec.Name, Image: service.Spec.TaskTemplate.ContainerSpec.Image}, nil
}
