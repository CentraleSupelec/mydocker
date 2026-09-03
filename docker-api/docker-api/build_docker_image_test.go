package main

import (
	"context"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"testing"
)

type testingBuildImageClient struct {
	mock.Mock
	dockerImageBuilderDockerClient
}

func (t *testingBuildImageClient) Info(ctx context.Context) (types.Info, error) {
	args := t.Called(ctx)
	return args.Get(0).(types.Info), args.Error(1)
}

func (t *testingBuildImageClient) NodeInspectWithRaw(ctx context.Context, nodeID string) (swarm.Node, []byte, error) {
	args := t.Called(ctx, nodeID)
	return args.Get(0).(swarm.Node), args.Get(1).([]byte), args.Error(2)
}

type testingBuildDockerImageDockerUtils struct {
	mock.Mock
	DockerUtilsInterface
}

type BuildDockerImageTestSuite struct {
	suite.Suite
	mocks []*mock.Call
}

func (suite *BuildDockerImageTestSuite) SetupTest() {
	suite.mocks = []*mock.Call{}
}

func (suite *BuildDockerImageTestSuite) AfterTest() {
	for _, m := range suite.mocks {
		m.Unset()
	}
}

func (suite *BuildDockerImageTestSuite) TestBuildConstraint() {
	stubClient := new(testingBuildImageClient)
	suite.mocks = append(
		suite.mocks,
		stubClient.On("Info", mock.Anything).Return(types.Info{
			ID:    "NodeId",
			Swarm: swarm.Info{NodeID: "SwarmNodeId"},
		}, nil).Times(1),
		stubClient.On("NodeInspectWithRaw", mock.Anything, mock.MatchedBy(func(nodeId string) bool {
			return nodeId == "SwarmNodeId"
		})).Return(swarm.Node{
			Spec: swarm.NodeSpec{
				Annotations: swarm.Annotations{
					Labels: map[string]string{"volume_backend": "rbd"},
				},
			},
		}, []byte{}, nil).Times(1),
	)
	ctx := context.Background()
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)
	actual, err := builder.buildConstraint(ctx)
	assert.Equal(suite.T(), "node.labels.volume_backend==rbd", actual)
	assert.Nil(suite.T(), err)
	stubClient.AssertExpectations(suite.T())
}

func (suite *BuildDockerImageTestSuite) TestEmptyBuildConstraint() {
	stubClient := new(testingBuildImageClient)
	suite.mocks = append(
		suite.mocks,
		stubClient.On("Info", mock.Anything).Return(types.Info{
			ID:    "NodeId",
			Swarm: swarm.Info{NodeID: "SwarmNodeId"},
		}, nil).Times(1),
		stubClient.On("NodeInspectWithRaw", mock.Anything, mock.MatchedBy(func(nodeId string) bool {
			return nodeId == "SwarmNodeId"
		})).Return(swarm.Node{}, []byte{}, nil).Times(1),
	)
	ctx := context.Background()
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)
	actual, err := builder.buildConstraint(ctx)
	assert.Equal(suite.T(), "", actual)
	assert.NotNil(suite.T(), err)
	stubClient.AssertExpectations(suite.T())
}

func TestBuildDockerImageTestSuite(t *testing.T) {
	suite.Run(t, new(BuildDockerImageTestSuite))
}
