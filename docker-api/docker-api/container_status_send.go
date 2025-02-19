package main

import (
	"context"
	"fmt"
	"regexp"
	"strings"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
)

type containerStatusDockerClient interface {
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
}

type containerStatusService struct {
	dockerClient      containerStatusDockerClient
	logger            *log.Entry
	containersToWatch map[string]bool
	out               chan<- *pb.ContainerStatusResponse
}

func newContainerStatusService(dockerClient containerStatusDockerClient, logger *log.Entry, containersToWatch map[string]bool, out chan<- *pb.ContainerStatusResponse) containerStatusService {
	return containerStatusService{
		dockerClient:      dockerClient,
		logger:            logger,
		containersToWatch: containersToWatch,
		out:               out,
	}
}

func setupContainerStatusCron(out chan<- *pb.ContainerStatusResponse, containersToWatch map[string]bool, dockerClient containerStatusDockerClient, cronScheduler *gocron.Scheduler) error {
	tag := "containerStatus"
	logger := log.WithFields(log.Fields{"service": tag})
	service := newContainerStatusService(dockerClient, logger, containersToWatch, out)
	_, err := cronScheduler.Every(c.ContainerStatusInterval).Tag(tag).Do(service.sendContainerStatus)
	if err != nil {
		return err
	}
	return nil
}

func (s *containerStatusService) sendContainerStatus() {
	for containerName := range s.containersToWatch {
		s.logger.Debugf("Checking status for container: %s", containerName)

		tasks, err := s.dockerClient.TaskList(context.TODO(), types.TaskListOptions{Filters: filters.NewArgs(filters.KeyValuePair{Key: "service", Value: containerName})})
		if err != nil {
			s.logger.Errorf("Error fetching task list for container %s: %v", containerName, err)
			if matched, _ := regexp.MatchString(`service .* not found`, err.Error()); matched {
				delete(s.containersToWatch, containerName)
			}
			continue
		}
		s.logger.Debugf("Fetched %d tasks for container %s", len(tasks), containerName)
		failed := 0
		var desiredTask *swarm.Task
		var tasksByDesiredState = make(map[swarm.TaskState][]*swarm.Task)
		for _, task := range tasks {
			tasksByDesiredState[task.DesiredState] = append(tasksByDesiredState[task.DesiredState], &task)
			if task.Status.State == swarm.TaskStateFailed {
				failed += 1
			}
		}
		if len(tasksByDesiredState[swarm.TaskStateRunning]) > 0 {
			desiredTask = tasksByDesiredState[swarm.TaskStateRunning][0]
		} else if len(tasksByDesiredState[swarm.TaskStateReady]) > 0 {
			desiredTask = tasksByDesiredState[swarm.TaskStateReady][0]
		} else if len(tasksByDesiredState[swarm.TaskStateShutdown]) > 0 {
			desiredTask = tasksByDesiredState[swarm.TaskStateShutdown][0]
		}
		s.logger.Infof("Container %s has %d failed tasks", containerName, failed)
		if desiredTask == nil {
			s.logger.Errorf("No task found for container %s", containerName)
			continue
		}
		containerInfo, err := parseContainerName(containerName)
		if err != nil {
			s.logger.Errorf("Error parsing container name %s: %v", containerName, err)
			continue
		}
		errorMessage := ""
		if desiredTask.Status.Err == "" {
			if desiredTask.Status.State == swarm.TaskStateComplete {
				errorMessage = "service has completed"
			}
			if failed > 0 {
				errorMessage = fmt.Sprintf("service has %d failed tasks and is unstable", failed)
			}
		} else {
			errorMessage = desiredTask.Status.Err
		}
		s.logger.Infof("Container %s status: %s, error: %s", containerName, desiredTask.Status.State, errorMessage)

		response := &pb.ContainerStatusResponse{
			UserID:   containerInfo.userId,
			CourseID: containerInfo.courseId,
			IsAdmin:  containerInfo.isAdmin,
			State: pb.ContainerStatusResponse_State(
				// Use uppercase because `new` is a reserved keyword in java but `NEW` is not
				pb.ContainerStatusResponse_State_value[strings.ToUpper(string(desiredTask.Status.State))]),
			ErrorMessage: errorMessage,
		}
		s.out <- response
	}
}
