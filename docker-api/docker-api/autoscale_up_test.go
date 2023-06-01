package main

import (
	tasksTypes "github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"golang.org/x/net/context"
	"sync"
	"testing"
)

type testingAutoscaleUpClient struct {
	mock.Mock
	deployUtilsDockerClient
}

func (t *testingAutoscaleUpClient) TaskList(ctx context.Context, options tasksTypes.TaskListOptions) ([]swarm.Task, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Task), args.Error(1)
}

type testingAutoscaleUpDockerUtils struct {
	mock.Mock
	DeployUtilsInterface
}

func (t *testingAutoscaleUpDockerUtils) launchTerraform(terraformConfig *TerraformConfig, callback deployCallback, removeContainerOnError bool) error {
	args := t.Called(terraformConfig, callback, removeContainerOnError)
	return args.Error(0)
}

type testingAutoscaleUpAutoscalingUtils struct {
	mock.Mock
}

func (t *testingAutoscaleUpAutoscalingUtils) buildConfigFromExistingInfra() (*TerraformConfig, error) {
	args := t.Called()
	return args.Get(0).(*TerraformConfig), args.Error(1)
}

type AutoscaleUpTestSuite struct {
	suite.Suite
	mocks []*mock.Call
}

func (suite *AutoscaleUpTestSuite) SetupTest() {
	suite.mocks = []*mock.Call{}
}

func (suite *AutoscaleUpTestSuite) AfterTest() {
	for _, m := range suite.mocks {
		m.Unset()
	}
}

func (suite *AutoscaleUpTestSuite) TestOneScaledNoNewNoError() {
	/*
		Si 1 autoscalé + 0 missing + pas d'erreur => pas de provision
	*/
	stubClient := new(testingAutoscaleUpClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStateAccepted},
		},
	}, nil))

	stubDockerUtils := new(testingAutoscaleUpDockerUtils)

	stubAutoscalingUtils := new(testingAutoscaleUpAutoscalingUtils)
	stubAutoscalingUtils.On("buildConfigFromExistingInfra").Return(&TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}, nil)

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleUpService := newScaleUpService(
		stubClient,
		log.NewEntry(logger),
		&sync.Mutex{},
		&ScaleUpConfig{
			Lock: sync.Mutex{},
			ScaleUpOwners: map[string]scaleUpOwner{
				"my-owner": {
					InstanceType:      "t1-45",
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  0,
					Regions: []ScalingRegion{{
						ImageId: "imageId",
						Region:  "GRA0",
					}},
				},
			},
		},
		stubDockerUtils,
		stubAutoscalingUtils,
	)
	err := scaleUpService.createNodes(map[string]int64{"my-owner": 1}, map[string]int64{"my-owner": 1})
	if err != nil {
		suite.T().Error(err)
	}
	stubDockerUtils.AssertExpectations(suite.T())
}

func (suite *AutoscaleUpTestSuite) TestOneScaledOneNewNoError() {
	/*
		Si 1 autoscalé + 1 missing + pas d'erreur => provision + 1 NamedWorker de plus
	*/
	stubClient := new(testingAutoscaleUpClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStateAccepted},
		},
	}, nil))

	stubDockerUtils := new(testingAutoscaleUpDockerUtils)
	stubDockerUtils.On("launchTerraform", mock.MatchedBy(func(tfConfig *TerraformConfig) bool {
		return len(tfConfig.NamedWorkers) == 2
	}), mock.Anything, mock.Anything).Return(nil).Times(1)

	stubAutoscalingUtils := new(testingAutoscaleUpAutoscalingUtils)
	stubAutoscalingUtils.On("buildConfigFromExistingInfra").Return(&TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}, nil)

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleUpService := newScaleUpService(
		stubClient,
		log.NewEntry(logger),
		&sync.Mutex{},
		&ScaleUpConfig{
			Lock: sync.Mutex{},
			ScaleUpOwners: map[string]scaleUpOwner{
				"my-owner": {
					InstanceType:      "t1-45",
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  0,
					Regions: []ScalingRegion{{
						ImageId: "imageId",
						Region:  "GRA0",
					}},
				},
			},
		},
		stubDockerUtils,
		stubAutoscalingUtils,
	)
	err := scaleUpService.createNodes(map[string]int64{"my-owner": 2}, map[string]int64{"my-owner": 1})
	if err != nil {
		suite.T().Error(err)
	}
	stubDockerUtils.AssertExpectations(suite.T())
}

func (suite *AutoscaleUpTestSuite) TestOneScaledNoNewError() {
	/*
		Si 1 autoscalé + 0 missing + erreur => provision + autant de NamedWorker
	*/
	stubClient := new(testingAutoscaleUpClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStateAccepted},
		},
	}, nil))

	stubDockerUtils := new(testingAutoscaleUpDockerUtils)
	stubDockerUtils.On("launchTerraform", mock.MatchedBy(func(tfConfig *TerraformConfig) bool {
		return len(tfConfig.NamedWorkers) == 1
	}), mock.Anything, mock.Anything).Return(nil).Times(1)

	stubAutoscalingUtils := new(testingAutoscaleUpAutoscalingUtils)
	stubAutoscalingUtils.On("buildConfigFromExistingInfra").Return(&TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}, nil)

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleUpService := newScaleUpService(
		stubClient,
		log.NewEntry(logger),
		&sync.Mutex{},
		&ScaleUpConfig{
			Lock: sync.Mutex{},
			ScaleUpOwners: map[string]scaleUpOwner{
				"my-owner": {
					InstanceType:      "t1-45",
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  0,
					Regions: []ScalingRegion{{
						ImageId: "imageId",
						Region:  "GRA0",
					}},
				},
			},
		},
		stubDockerUtils,
		stubAutoscalingUtils,
	)
	scaleUpService.scaleUpErroredLock.Lock()
	*scaleUpService.scaleUpErrored = true
	scaleUpService.scaleUpErroredLock.Unlock()
	err := scaleUpService.createNodes(map[string]int64{"my-owner": 1}, map[string]int64{"my-owner": 1})
	if err != nil {
		suite.T().Error(err)
	}
	stubDockerUtils.AssertExpectations(suite.T())
}

func (suite *AutoscaleUpTestSuite) TestNoScaledNoNewNoError() {
	/*
		Si 0 autoscalé + 0 missing + pas d'erreur => pas de provision
	*/
	stubClient := new(testingAutoscaleUpClient)
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{}, nil))

	stubDockerUtils := new(testingAutoscaleUpDockerUtils)

	stubAutoscalingUtils := new(testingAutoscaleUpAutoscalingUtils)
	stubAutoscalingUtils.On("buildConfigFromExistingInfra").Return(&TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{},
	}, nil)

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleUpService := newScaleUpService(
		stubClient,
		log.NewEntry(logger),
		&sync.Mutex{},
		&ScaleUpConfig{
			Lock: sync.Mutex{},
			ScaleUpOwners: map[string]scaleUpOwner{
				"my-owner": {
					InstanceType:      "t1-45",
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  0,
					Regions: []ScalingRegion{{
						ImageId: "imageId",
						Region:  "GRA0",
					}},
				},
			},
		},
		stubDockerUtils,
		stubAutoscalingUtils,
	)
	scaleUpService.scaleUpErroredLock.Lock()
	*scaleUpService.scaleUpErrored = true
	scaleUpService.scaleUpErroredLock.Unlock()
	err := scaleUpService.createNodes(map[string]int64{"my-owner": 0}, map[string]int64{"my-owner": 0})
	if err != nil {
		suite.T().Error(err)
	}
	stubDockerUtils.AssertExpectations(suite.T())
}

func TestAutoscaleUpTestSuite(t *testing.T) {
	suite.Run(t, new(AutoscaleUpTestSuite))
}
