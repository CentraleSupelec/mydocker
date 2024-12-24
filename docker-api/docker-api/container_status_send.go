package main

import (
	"context"
	"fmt"
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
	for containerName, _ := range s.containersToWatch {
		s.logger.Debugf("Checking status for container: %s", containerName)

		tasks, err := s.dockerClient.TaskList(context.TODO(), types.TaskListOptions{Filters: filters.NewArgs(filters.KeyValuePair{Key: "service", Value: containerName})})
		if err != nil {
			s.logger.Errorf("Error fetching task list for container %s: %v", containerName, err)
			continue
		}
		s.logger.Debugf("Fetched %d tasks for container %s", len(tasks), containerName)
		failed := 0
		var desiredTask *swarm.Task
		for _, task := range tasks {
			if task.Status.State == swarm.TaskStateFailed {
				failed += 1
			}
			if task.DesiredState == swarm.TaskStateRunning || task.DesiredState == swarm.TaskStateReady {
				if desiredTask != nil {
					s.logger.Errorf("Multiple tasks running for service %s", containerName)
				}
				desiredTask = &task
			}
		}
		s.logger.Infof("Container %s has %d failed tasks", containerName, failed)

		containerInfo, err := parseContainerName(containerName)
		if err != nil {
			s.logger.Errorf("Error parsing container name %s: %v", containerName, err)
			continue
		}
		errorMessage := ""
		if failed > 0 && desiredTask.Status.Err == "" {
			errorMessage = fmt.Sprintf("service has %d failed tasks and is unstable", failed)
		} else {
			errorMessage = desiredTask.Status.Err
		}
		s.logger.Infof("Container %s status: %s, error: %s", containerName, desiredTask.Status.State, errorMessage)

		response := &pb.ContainerStatusResponse{
			UserID:   containerInfo.userId,
			CourseID: containerInfo.courseId,
			IsAdmin:  containerInfo.isAdmin,
			State: pb.ContainerStatusResponse_State(
				pb.ContainerStatusResponse_State_value[strings.ToUpper(string(desiredTask.Status.State))]),
			ErrorMessage: errorMessage,
		}
		s.out <- response
	}
}
