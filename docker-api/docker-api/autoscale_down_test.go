package main

import (
	"sync"
	"testing"
	"time"

	tasksTypes "github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"golang.org/x/net/context"
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
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  0,
					InstancesRegions: []InstanceRegions{{
						InstanceType: "t1-45",
						Regions:      []ScalingRegion{},
					}},
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
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  0,
					InstancesRegions: []InstanceRegions{{
						InstanceType: "t1-45",
						Regions:      []ScalingRegion{},
					}}},
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
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  1,
					InstancesRegions: []InstanceRegions{{
						InstanceType: "t1-45",
						Regions:      []ScalingRegion{},
					}}},
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
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  1,
					InstancesRegions: []InstanceRegions{{
						InstanceType: "t1-45",
						Regions:      []ScalingRegion{},
					}}},
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
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  1,
					InstancesRegions: []InstanceRegions{{
						InstanceType: "t1-45",
						Regions:      []ScalingRegion{},
					}}},
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
					MinIdleNodesCount: 0,
					MaxNodesCount:     3,
					ManualNodesCount:  1,
					InstancesRegions: []InstanceRegions{{
						InstanceType: "t1-45",
						Regions:      []ScalingRegion{},
					}}},
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

func (suite *AutoscaleDownTestSuite) TestTerraformNodeAbsentFromSwarmRemovedAfterGracePeriod() {
	/*
		1 noeud dans terraform, absent de Swarm, 0 idle demandé => conservé pendant le délai de grâce, supprimé ensuite
	*/
	c.ScaleDownAbsentGracePeriod = "15m"
	stubClient := new(autoscaleDownTestingClient)
	suite.mocks = append(suite.mocks, stubClient.On("NodeList", mock.Anything, mock.Anything).Return([]swarm.Node{}, nil))

	logger := log.New()
	logger.SetLevel(log.DebugLevel)
	scaleDownService := newScaleDownService(
		stubClient,
		log.NewEntry(logger),
		&sync.Mutex{},
		&ScaleUpConfig{
			Lock: sync.Mutex{},
			ScaleUpOwners: map[string]scaleUpOwner{
				"my-owner": {MinIdleNodesCount: 0, MaxNodesCount: 3},
			},
		},
		nil,
		nil,
	)
	now := time.Date(2026, 10, 15, 12, 0, 0, 0, time.UTC)
	scaleDownService.now = func() time.Time { return now }
	tfConfig := &TerraformConfig{
		NamedWorkers: map[string]TerraformNamedWorker{"orphan-node": {Name: "orphan-node", Owner: "my-owner"}},
	}

	nodesToDelete, err := scaleDownService.findNodesToDelete(tfConfig)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), 0, len(nodesToDelete))

	now = now.Add(14 * time.Minute)
	nodesToDelete, err = scaleDownService.findNodesToDelete(tfConfig)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), 0, len(nodesToDelete))

	now = now.Add(2 * time.Minute)
	nodesToDelete, err = scaleDownService.findNodesToDelete(tfConfig)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), []string{"orphan-node"}, nodesToDelete)
}

// fakeSwarmClient serves a node list the test can change between runs.
type fakeSwarmClient struct {
	scaleDownDockerClient
	nodes   []swarm.Node
	removed []string
}

func (f *fakeSwarmClient) NodeList(context.Context, tasksTypes.NodeListOptions) ([]swarm.Node, error) {
	return f.nodes, nil
}

func (f *fakeSwarmClient) TaskList(context.Context, tasksTypes.TaskListOptions) ([]swarm.Task, error) {
	return nil, nil
}

func (f *fakeSwarmClient) NodeRemove(_ context.Context, nodeID string, _ tasksTypes.NodeRemoveOptions) error {
	f.removed = append(f.removed, nodeID)
	return nil
}

type fakeDeployUtils struct {
	DeployUtilsInterface
	launched int
}

func (f *fakeDeployUtils) launchTerraform(*TerraformConfig, deployCallback, bool) error {
	f.launched++
	return nil
}

func readySwarmNode(name, owner string) swarm.Node {
	return swarm.Node{
		ID:          name,
		Description: swarm.NodeDescription{Hostname: name},
		Spec:        swarm.NodeSpec{Annotations: swarm.Annotations{Labels: map[string]string{"owner": owner}}},
		Status:      swarm.NodeStatus{State: swarm.NodeStateReady},
	}
}

func newAbsentTestService(client scaleDownDockerClient, minIdle int64, deployUtils DeployUtilsInterface) scaleDownService {
	logger := log.New()
	logger.SetLevel(log.DebugLevel)
	return newScaleDownService(
		client,
		log.NewEntry(logger),
		&sync.Mutex{},
		&ScaleUpConfig{
			Lock: sync.Mutex{},
			ScaleUpOwners: map[string]scaleUpOwner{
				"my-owner": {MinIdleNodesCount: minIdle, MaxNodesCount: 3},
			},
		},
		deployUtils,
		nil,
	)
}

func (suite *AutoscaleDownTestSuite) TestAbsentNodeRemovedEvenWhenMinIdleWouldKeepIt() {
	c.ScaleDownAbsentGracePeriod = "15m"
	svc := newAbsentTestService(&fakeSwarmClient{}, 1, nil)
	now := time.Date(2026, 10, 15, 12, 0, 0, 0, time.UTC)
	svc.now = func() time.Time { return now }
	tfConfig := &TerraformConfig{
		NamedWorkers: map[string]TerraformNamedWorker{"orphan-node": {Name: "orphan-node", Owner: "my-owner"}},
	}

	_, err := svc.findNodesToDelete(tfConfig)
	assert.NoError(suite.T(), err)
	now = now.Add(16 * time.Minute)
	nodesToDelete, err := svc.findNodesToDelete(tfConfig)

	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), []string{"orphan-node"}, nodesToDelete)
}

func (suite *AutoscaleDownTestSuite) TestAbsentNodeTimerResetsWhenNodeJoinsSwarm() {
	c.ScaleDownAbsentGracePeriod = "15m"
	client := &fakeSwarmClient{}
	svc := newAbsentTestService(client, 1, nil)
	now := time.Date(2026, 10, 15, 12, 0, 0, 0, time.UTC)
	svc.now = func() time.Time { return now }
	tfConfig := &TerraformConfig{
		NamedWorkers: map[string]TerraformNamedWorker{"slow-node": {Name: "slow-node", Owner: "my-owner"}},
	}

	_, _ = svc.findNodesToDelete(tfConfig) // absent: timer starts
	now = now.Add(10 * time.Minute)
	client.nodes = []swarm.Node{readySwarmNode("slow-node", "my-owner")}
	_, _ = svc.findNodesToDelete(tfConfig) // joined: timer cleared
	assert.NotContains(suite.T(), svc.absentSince, "slow-node")

	now = now.Add(time.Minute)
	client.nodes = nil
	_, _ = svc.findNodesToDelete(tfConfig) // absent again: timer restarts
	now = now.Add(10 * time.Minute)
	nodesToDelete, err := svc.findNodesToDelete(tfConfig)

	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), 0, len(nodesToDelete), "10 minutes since the restart is under the grace period")
}

func (suite *AutoscaleDownTestSuite) TestDeleteNodesSkipsSwarmRemovalOfAbsentNode() {
	client := &fakeSwarmClient{nodes: []swarm.Node{readySwarmNode("real-node", "my-owner")}}
	deployUtils := &fakeDeployUtils{}
	svc := newAbsentTestService(client, 0, deployUtils)
	svc.swarmNodes = map[string]*NodeSummary{"real-node": {ID: "real-node", Owner: "my-owner"}}
	tfConfig := &TerraformConfig{
		NamedWorkers: map[string]TerraformNamedWorker{
			"orphan-node": {Name: "orphan-node", Owner: "my-owner"},
			"real-node":   {Name: "real-node", Owner: "my-owner"},
		},
	}

	err := svc.deleteNodes(tfConfig, []string{"orphan-node", "real-node"})

	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), 1, deployUtils.launched)
	assert.Equal(suite.T(), []string{"real-node"}, client.removed)
}

func (suite *AutoscaleDownTestSuite) TestScaleDownAbsentGracePeriodParsing() {
	defer func() { c.ScaleDownAbsentGracePeriod = "" }()
	for value, expected := range map[string]time.Duration{
		"":     15 * time.Minute,
		"30m":  30 * time.Minute,
		"90s":  90 * time.Second,
		"junk": 15 * time.Minute,
		"0s":   15 * time.Minute,
		"-5m":  15 * time.Minute,
	} {
		c.ScaleDownAbsentGracePeriod = value
		assert.Equal(suite.T(), expected, scaleDownAbsentGracePeriod(), "value %q", value)
	}
}

func TestAutoscaleDownTestSuite(t *testing.T) {
	suite.Run(t, new(AutoscaleDownTestSuite))
}
