package main

import (
	"context"
	"fmt"
	"github.com/docker/docker/api/types"
	containerTypes "github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"io"
	"io/ioutil"
)

type dockerUtilsDockerClient interface {
	ImagePull(ctx context.Context, ref string, options types.ImagePullOptions) (io.ReadCloser, error)
	ContainerWait(ctx context.Context, container string, condition containerTypes.WaitCondition) (<-chan containerTypes.ContainerWaitOKBody, <-chan error)
	ServiceList(ctx context.Context, options types.ServiceListOptions) ([]swarm.Service, error)
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
}

type DockerUtilsInterface interface {
	pullImage(imageName string, requireCredential bool) error
	waitForContainer(containerID string) error
	findService(ctx context.Context, userId string, courseId string) (*swarm.Service, error)
	getRunningTaskNodeId(ctx context.Context, userId string, courseId string) (string, error)
}

type DockerUtils struct {
	dockerClient dockerUtilsDockerClient
}

func NewDockerUtils(dockerClient dockerUtilsDockerClient) *DockerUtils {
	return &DockerUtils{
		dockerClient: dockerClient,
	}
}

func (d *DockerUtils) pullImage(imageName string, requireCredential bool) error {
	imagePullOptions := types.ImagePullOptions{}
	if requireCredential {
		authString := buildRegistryCredential(imageName)
		imagePullOptions.RegistryAuth = authString
	}
	reader, err := d.dockerClient.ImagePull(context.TODO(), imageName, imagePullOptions)
	if err != nil {
		return err
	}
	_, _ = ioutil.ReadAll(reader)
	err = reader.Close()
	if err != nil {
		return err
	}
	return nil
}

func (d *DockerUtils) waitForContainer(containerID string) error {
	statusCh, errCh := d.dockerClient.ContainerWait(context.TODO(), containerID, containerTypes.WaitConditionNotRunning)
	select {
	case err := <-errCh:
		if err != nil {
			return err
		}
	case <-statusCh:
	}
	return nil
}

func (d *DockerUtils) findService(ctx context.Context, userId string, courseId string) (*swarm.Service, error) {
	name := createContainerName(userId, courseId)
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("name", name)
	services, err := d.dockerClient.ServiceList(ctx, types.ServiceListOptions{Filters: filtersArgs})
	if err != nil {
		return nil, err
	}
	switch len(services) {
	case 0:
		return nil, fmt.Errorf("cannot find service with name %s", name)
	case 1:
		return &services[0], nil
	default:
		return nil, fmt.Errorf("multiple service have the same name %s", name)
	}
}

func (d *DockerUtils) getRunningTaskNodeId(ctx context.Context, userId string, courseId string) (string, error) {
	serviceName := createContainerName(userId, courseId)
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("service", serviceName)
	filtersArgs.Add("desired-state", "running")
	tasks, err := d.dockerClient.TaskList(ctx, types.TaskListOptions{Filters: filtersArgs})
	if err != nil {
		return "", err
	}
	states := map[string]string{}
	for _, task := range tasks {
		switch task.Status.State {
		case
			swarm.TaskStateAccepted,
			swarm.TaskStateReady,
			swarm.TaskStatePreparing,
			swarm.TaskStateStarting,
			swarm.TaskStateRunning:
			return task.NodeID, nil
		default:
			states[task.ID] = string(task.Status.State)
		}
	}
	return "", fmt.Errorf("among %d tasks for service %s, none found with a success state. States : %v", len(tasks), serviceName, states)
}
