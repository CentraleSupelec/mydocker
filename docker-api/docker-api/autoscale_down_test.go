package main

import (
	tasksTypes "github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"golang.org/x/net/context"
	"sync"
	"testing"
)

type autoscaleDownTestingClient struct {
	mock.Mock
	scaleDownDockerClient
}

func (t *autoscaleDownTestingClient) NodeList(ctx context.Context, options tasksTypes.NodeListOptions) ([]swarm.Node, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Node), args.Error(1)
}

func (t *autoscaleDownTestingClient) NodeRemove(context.Context, string, tasksTypes.NodeRemoveOptions) error {
	panic("implement me")
}

func (t *autoscaleDownTestingClient) TaskList(ctx context.Context, options tasksTypes.TaskListOptions) ([]swarm.Task, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Task), args.Error(1)
}

type AutoscaleDownTestSuite struct {
	suite.Suite
	mocks []*mock.Call
}

func (suite *AutoscaleDownTestSuite) SetupTest() {
	c.ScaleDownRemoveNonEmpty = false
	suite.mocks = []*mock.Call{}
}

func (suite *AutoscaleDownTestSuite) AfterTest() {
	for _, m := range suite.mocks {
		m.Unset()
	}
}

func (suite *AutoscaleDownTestSuite) TestZeroManualOneScaledWithServiceZeroIdle() {
	/*
		Si 0 manuel + 1 autoscalé, avec service sur le autoscalé, 0 idle demandé => pas de suppression
	*/
	stubClient := new(autoscaleDownTestingClient)
	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{
		{
			ID:          "autoscaled-node-1",
			Description: swarm.NodeDescription{Hostname: "autoscaled-node-1"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
	}, nil))
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStatePending},
		},
	}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleDownService := newScaleDownService(
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
					Regions:           []ScalingRegion{},
				},
			},
		},
		nil,
		&AutoscalingUtils{},
	)
	tfConfig := &TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)

	if err != nil {
		suite.T().Error(err)
	}

	assert.Equal(suite.T(), 0, len(nodesToDelete))
}

func (suite *AutoscaleDownTestSuite) TestOneManualZeroScaledZeroIdle() {
	/*
		Si 1 manuel + 0 autoscalé, sans service, 0 idle demandé => pas de suppression
	*/
	stubClient := new(autoscaleDownTestingClient)
	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{
		{
			ID:          "manual-node",
			Description: swarm.NodeDescription{Hostname: "autoscaled-node-1"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
	}, nil))
	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "manual-node"
	})).Return([]swarm.Task{}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleDownService := newScaleDownService(
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
					Regions:           []ScalingRegion{},
				},
			},
		},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)

	if err != nil {
		suite.T().Error(err)
	}

	assert.Equal(suite.T(), 0, len(nodesToDelete))
}

func (suite *AutoscaleDownTestSuite) TestOneManualWithServiceOneScaledZeroIdle() {
	/*
		Si 1 manuel + 1 autoscalé, avec service sur le manuel, 0 idle demandé => suppression du autoscalé
	*/
	stubClient := new(autoscaleDownTestingClient)

	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{
		{
			ID:          "manual-node",
			Description: swarm.NodeDescription{Hostname: "manual-node"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
		{
			ID:          "autoscaled-node-1",
			Description: swarm.NodeDescription{Hostname: "autoscaled-node-1"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
	}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "manual-node"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStatePending},
		},
	}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleDownService := newScaleDownService(
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
					ManualNodesCount:  1,
					Regions:           []ScalingRegion{},
				},
			},
		},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)

	if err != nil {
		suite.T().Error(err)
	}

	assert.Equal(suite.T(), 1, len(nodesToDelete))
	assert.Equal(suite.T(), "autoscaled-NODE-1", nodesToDelete[0])
}

func (suite *AutoscaleDownTestSuite) TestOneManualOneScaledWithServiceZeroIdleDefaultConfig() {
	/*
		Si 1 manuel + 1 autoscalé, avec service sur le scaled, 0 idle demandé, config par défaut => pas de suppression
	*/
	stubClient := new(autoscaleDownTestingClient)

	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{
		{
			ID:          "manual-node",
			Description: swarm.NodeDescription{Hostname: "manual-node"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
		{
			ID:          "autoscaled-node-1",
			Description: swarm.NodeDescription{Hostname: "autoscaled-node-1"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
	}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "manual-node"
	})).Return([]swarm.Task{}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStatePending},
		},
	}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleDownService := newScaleDownService(
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
					ManualNodesCount:  1,
					Regions:           []ScalingRegion{},
				},
			},
		},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)

	if err != nil {
		suite.T().Error(err)
	}

	assert.Equal(suite.T(), 0, len(nodesToDelete))
}

func (suite *AutoscaleDownTestSuite) TestOneManualOneScaledWithServiceZeroIdleForcedRemoval() {
	/*
		Si 1 manuel + 1 autoscalé, avec service sur le scaled, 0 idle demandé, config de removal => suppression du scaled
	*/
	c.ScaleDownRemoveNonEmpty = true
	stubClient := new(autoscaleDownTestingClient)

	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{
		{
			ID:          "manual-node",
			Description: swarm.NodeDescription{Hostname: "manual-node"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
		{
			ID:          "autoscaled-node-1",
			Description: swarm.NodeDescription{Hostname: "autoscaled-node-1"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
	}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "manual-node"
	})).Return([]swarm.Task{}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{
		{
			Status: swarm.TaskStatus{State: swarm.TaskStatePending},
		},
	}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleDownService := newScaleDownService(
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
					ManualNodesCount:  1,
					Regions:           []ScalingRegion{},
				},
			},
		},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)

	if err != nil {
		suite.T().Error(err)
	}

	assert.Equal(suite.T(), 1, len(nodesToDelete))
	assert.Equal(suite.T(), "autoscaled-NODE-1", nodesToDelete[0])
}

func (suite *AutoscaleDownTestSuite) TestOneManualOneScaledZeroIdle() {
	/*
		Si 1 manuel + 1 autoscalé, sans service, 0 idle demandé => suppression du autoscalé
	*/
	c.ScaleDownRemoveNonEmpty = true
	stubClient := new(autoscaleDownTestingClient)

	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{
		{
			ID:          "manual-node",
			Description: swarm.NodeDescription{Hostname: "manual-node"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
		{
			ID:          "autoscaled-node-1",
			Description: swarm.NodeDescription{Hostname: "autoscaled-node-1"},
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{Labels: map[string]string{
					"owner": "my-owner",
				}},
			},
			Status: swarm.NodeStatus{
				State: swarm.NodeStateReady,
			},
		},
	}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "manual-node"
	})).Return([]swarm.Task{}, nil))

	suite.mocks = append(suite.mocks, stubClient.On("TaskList", mock.Anything, mock.MatchedBy(func(options tasksTypes.TaskListOptions) bool {
		return options.Filters.Get("node")[0] == "autoscaled-node-1"
	})).Return([]swarm.Task{}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)

	scaleDownService := newScaleDownService(
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
					ManualNodesCount:  1,
					Regions:           []ScalingRegion{},
				},
			},
		},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{
		InstancePrefix:   "",
		CephServiceName:  "",
		Env:              "",
		NbWorkerByRegion: nil,
		StateName:        "",
		NamedWorkers:     map[string]TerraformNamedWorker{"autoscaled-NODE-1": {Name: "autoscaled-NODE-1", Owner: "my-owner"}},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)

	if err != nil {
		suite.T().Error(err)
	}

	assert.Equal(suite.T(), 1, len(nodesToDelete))
	assert.Equal(suite.T(), "autoscaled-NODE-1", nodesToDelete[0])
}

func TestAutoscaleDownTestSuite(t *testing.T) {
	suite.Run(t, new(AutoscaleDownTestSuite))
}
