package main

import (
	"context"
	"errors"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	volumeTypes "github.com/docker/docker/api/types/volume"
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

func (t *testingBuildImageClient) VolumeCreate(ctx context.Context, options volumeTypes.VolumeCreateBody) (types.Volume, error) {
	args := t.Called(ctx, options)
	return args.Get(0).(types.Volume), args.Error(1)
}

func (t *testingBuildImageClient) VolumeRemove(ctx context.Context, volumeID string, force bool) error {
	args := t.Called(ctx, volumeID, force)
	return args.Error(0)
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

// A VolumeCreate that reports an error may still have created the volume: dockerd applies an HTTP
// client timeout to volume driver calls and stops waiting, while the driver runs to completion.
// Since buildId is unique per build, no retry ever reuses the name, so without cleanup on this path
// each timed-out build leaks its 5 GB image for good.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRemovesVolumeWhenCreateFails() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	suite.mocks = append(
		suite.mocks,
		stubClient.On("VolumeCreate", mock.Anything, mock.MatchedBy(func(options volumeTypes.VolumeCreateBody) bool {
			return options.Name == "volume_build_build-42"
		})).Return(types.Volume{}, createErr).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, "volume_build_build-42", true).Return(nil).Times(1),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	path, err := builder.prepareBuildFiles("build-42", nil, "FROM scratch", "#!/bin/sh\n")

	// The original create error must be what the caller sees; the cleanup must not mask it.
	assert.Equal(suite.T(), createErr, err)
	assert.Equal(suite.T(), "", path)
	// The point of the test: the volume that may exist despite the error is removed.
	stubClient.AssertExpectations(suite.T())
}

// A cleanup that itself fails must be logged and swallowed, never substituted for the real error.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesReturnsCreateErrorWhenCleanupFails() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("create failed")
	suite.mocks = append(
		suite.mocks,
		stubClient.On("VolumeCreate", mock.Anything, mock.Anything).
			Return(types.Volume{}, createErr).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, mock.Anything, true).
			Return(errors.New("remove failed too")).Times(1),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	_, err := builder.prepareBuildFiles("build-43", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	stubClient.AssertExpectations(suite.T())
}

func TestBuildDockerImageTestSuite(t *testing.T) {
	suite.Run(t, new(BuildDockerImageTestSuite))
}
