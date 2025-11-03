package main

import (
	"context"
	"fmt"
	"sync"
	"testing"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	tasksTypes "github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type containerStatusSendTestClient struct {
	mock.Mock
	containerStatusDockerClient
}

func (c *containerStatusSendTestClient) TaskList(ctx context.Context, options tasksTypes.TaskListOptions) ([]swarm.Task, error) {
	args := c.Called(ctx, options)
	return args.Get(0).([]swarm.Task), args.Error(1)
}

type ContainerStatusSendTestSuite struct {
	suite.Suite
	mocks []*mock.Call
}

func (suite *ContainerStatusSendTestSuite) SetupTest() {
	suite.mocks = []*mock.Call{}
	print("ContainerStatusSendTestSuite")
}

func (suite *ContainerStatusSendTestSuite) AfterTest() {
	for _, m := range suite.mocks {
		m.Unset()
	}
}

func (suite *ContainerStatusSendTestSuite) TestContainerStatusIsFetchedSuccessfully() {
	stubClient := new(containerStatusSendTestClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("service")[0] == "test-container"
	})).Return([]swarm.Task{
		{
			DesiredState: swarm.TaskStateRunning,
			Status:       swarm.TaskStatus{State: swarm.TaskStateRunning},
		},
	}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	out := make(chan *pb.ContainerStatusResponse, 1)
	containersToWatchMutex := sync.RWMutex{}
	service := newContainerStatusService(stubClient, log.NewEntry(logger), map[string]bool{"test-container": true}, &containersToWatchMutex, &out)
	service.sendContainerStatus()

	response := <-out
	assert.Equal(suite.T(), pb.ContainerStatusResponse_RUNNING, response.State)
	assert.Empty(suite.T(), response.ErrorMessage)
}

func (suite *ContainerStatusSendTestSuite) TestContainerStatusFetchFails() {
	stubClient := new(containerStatusSendTestClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{}, fmt.Errorf("service test-container not found")))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	out := make(chan *pb.ContainerStatusResponse, 1)
	containersToWatchMutex := sync.RWMutex{}
	service := newContainerStatusService(stubClient, log.NewEntry(logger), map[string]bool{"test-container": true}, &containersToWatchMutex, &out)
	service.sendContainerStatus()

	assert.Empty(suite.T(), out)
	assert.Empty(suite.T(), service.containersToWatch)
}

func (suite *ContainerStatusSendTestSuite) TestContainerStatusHasFailedTasks() {
	stubClient := new(containerStatusSendTestClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		{
			DesiredState: swarm.TaskStateShutdown,
			Status:       swarm.TaskStatus{State: swarm.TaskStateFailed},
		},
		{
			DesiredState: swarm.TaskStateReady,
			Status:       swarm.TaskStatus{State: swarm.TaskStatePreparing},
		},
	}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	out := make(chan *pb.ContainerStatusResponse, 1)
	containersToWatchMutex := sync.RWMutex{}
	service := newContainerStatusService(stubClient, log.NewEntry(logger), map[string]bool{"test-container": true}, &containersToWatchMutex, &out)
	service.sendContainerStatus()

	response := <-out
	assert.Equal(suite.T(), pb.ContainerStatusResponse_PREPARING, response.State)
	assert.Contains(suite.T(), response.ErrorMessage, "unstable")
}

func (suite *ContainerStatusSendTestSuite) TestContainerStatusNoTasksFound() {
	stubClient := new(containerStatusSendTestClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	out := make(chan *pb.ContainerStatusResponse, 1)
	containersToWatchMutex := sync.RWMutex{}
	service := newContainerStatusService(stubClient, log.NewEntry(logger), map[string]bool{"test-container": true}, &containersToWatchMutex, &out)
	service.sendContainerStatus()

	assert.Empty(suite.T(), out)
}

func (suite *ContainerStatusSendTestSuite) TestContainerStatusCompletedTask() {
	stubClient := new(containerStatusSendTestClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		{
			DesiredState: swarm.TaskStateShutdown,
			Status:       swarm.TaskStatus{State: swarm.TaskStateComplete},
		},
	}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	out := make(chan *pb.ContainerStatusResponse, 1)
	containersToWatchMutex := sync.RWMutex{}
	service := newContainerStatusService(stubClient, log.NewEntry(logger), map[string]bool{"test-container": true}, &containersToWatchMutex, &out)
	service.sendContainerStatus()

	response := <-out
	assert.Equal(suite.T(), pb.ContainerStatusResponse_COMPLETE, response.State)
	assert.Contains(suite.T(), response.ErrorMessage, "completed")
}

func (suite *ContainerStatusSendTestSuite) TestConcurrentAccessContainersToWatch() {
	stubClient := new(containerStatusSendTestClient)
	logger := log.New()
	logger.SetLevel(log.DebugLevel)
	out := make(chan *pb.ContainerStatusResponse, 10)
	containersToWatchMutex := sync.RWMutex{}
	service := &containerStatusService{
		dockerClient:           stubClient,
		logger:                 log.NewEntry(logger),
		containersToWatch:      map[string]bool{},
		out:                    &out,
		containersToWatchMutex: &containersToWatchMutex,
	}

	// Simule 10 goroutines qui ajoutent/suppriment et 5 qui lisent la map
	done := make(chan struct{})
	for i := 0; i < 10; i++ {
		go func(idx int) {
			name := fmt.Sprintf("container-%d", idx)
			for j := 0; j < 100; j++ {
				service.containersToWatchMutex.Lock()
				service.containersToWatch[name] = true
				delete(service.containersToWatch, name)
				service.containersToWatchMutex.Unlock()
			}
			done <- struct{}{}
		}(i)
	}
	for i := 0; i < 5; i++ {
		go func() {
			for j := 0; j < 100; j++ {
				service.containersToWatchMutex.RLock()
				_ = len(service.containersToWatch)
				for k := range service.containersToWatch {
					_ = k
				}
				service.containersToWatchMutex.RUnlock()
			}
			done <- struct{}{}
		}()
	}
	// Attend la fin de toutes les goroutines
	for i := 0; i < 15; i++ {
		<-done
	}
	// Vérifie qu'aucune panique ou erreur de concurrence n'est survenue
	service.containersToWatchMutex.RLock()
	assert.Equal(suite.T(), 0, len(service.containersToWatch), "Map should be empty after concurrent access")
	service.containersToWatchMutex.RUnlock()
}

func TestContainerStatusSendTestSuite(t *testing.T) {
	suite.Run(t, new(ContainerStatusSendTestSuite))
}
