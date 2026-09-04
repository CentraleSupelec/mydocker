package main

import (
	"context"
	"errors"
	"github.com/docker/docker/api/types"
	containerTypes "github.com/docker/docker/api/types/container"
	networkTypes "github.com/docker/docker/api/types/network"
	"github.com/docker/docker/api/types/swarm"
	volumeTypes "github.com/docker/docker/api/types/volume"
	"github.com/docker/docker/errdefs"
	specs "github.com/opencontainers/image-spec/specs-go/v1"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"strings"
	"testing"
	"time"
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

func (t *testingBuildImageClient) VolumeCreate(
	ctx context.Context, options volumeTypes.VolumeCreateBody,
) (types.Volume, error) {
	args := t.Called(ctx, options)
	return args.Get(0).(types.Volume), args.Error(1)
}

func (t *testingBuildImageClient) VolumeRemove(ctx context.Context, volumeID string, force bool) error {
	args := t.Called(ctx, volumeID, force)
	return args.Error(0)
}

func (t *testingBuildImageClient) VolumeInspect(ctx context.Context, volumeID string) (types.Volume, error) {
	args := t.Called(ctx, volumeID)
	return args.Get(0).(types.Volume), args.Error(1)
}

func (t *testingBuildImageClient) ContainerCreate(
	ctx context.Context,
	config *containerTypes.Config,
	hostConfig *containerTypes.HostConfig,
	networkingConfig *networkTypes.NetworkingConfig,
	platform *specs.Platform,
	containerName string,
) (containerTypes.ContainerCreateCreatedBody, error) {
	args := t.Called(ctx, config, hostConfig, networkingConfig, platform, containerName)
	return args.Get(0).(containerTypes.ContainerCreateCreatedBody), args.Error(1)
}

func (t *testingBuildImageClient) ContainerRemove(
	ctx context.Context, container string, options types.ContainerRemoveOptions,
) error {
	args := t.Called(ctx, container, options)
	return args.Error(0)
}

func (t *testingBuildDockerImageDockerUtils) pullImage(imageName string, requireCredential bool) error {
	args := t.Called(imageName, requireCredential)
	return args.Error(0)
}

// captureCreatedVolumeName records the name prepareBuildFiles generated. The name now carries a
// random per-attempt suffix, so tests match on the prefix and read the exact value back from here.
func captureCreatedVolumeName(stub *testingBuildImageClient, createResult error) (*string, *mock.Call) {
	name := new(string)
	call := stub.On("VolumeCreate", mock.Anything, mock.Anything).
		Run(func(args mock.Arguments) {
			options := args.Get(1).(volumeTypes.VolumeCreateBody)
			*name = options.Name
		}).
		Return(types.Volume{}, createResult).Times(1)
	return name, call
}

// hasBuildVolumePrefix matches the generated name without knowing its random suffix.
func hasBuildVolumePrefix(buildId string) interface{} {
	return mock.MatchedBy(func(volumeName string) bool {
		return strings.HasPrefix(volumeName, "volume_build_"+buildId+"_")
	})
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
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRemovesVolumeWhenCreateFails() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	createdName, createCall := captureCreatedVolumeName(stubClient, createErr)
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-42")).
			Return(types.Volume{}, nil).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-42"), true).
			Return(nil).Times(1),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	path, err := builder.prepareBuildFiles("build-42", nil, "FROM scratch", "#!/bin/sh\n")

	// The original create error must be what the caller sees; the cleanup must not mask it.
	assert.Equal(suite.T(), createErr, err)
	assert.Equal(suite.T(), "", path)
	// The name must actually carry a per-attempt suffix, otherwise cleanup has no ownership basis.
	assert.Regexp(suite.T(), `^volume_build_build-42_[0-9a-f]{16}$`, *createdName)
	stubClient.AssertExpectations(suite.T())
}

// The regression that killed the previous label-based approach. dockerd persists volume metadata
// only after the driver call returns, so a volume left behind by a timed-out create can be
// rediscovered carrying NO labels. Cleanup must still remove it: ownership comes from the unique
// name, and a label check here would read empty labels as another attempt's volume and leak it.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRemovesVolumeWithNoLabels() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	_, createCall := captureCreatedVolumeName(stubClient, createErr)
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-44")).
			Return(types.Volume{Labels: nil}, nil).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-44"), true).
			Return(nil).Times(1),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	_, err := builder.prepareBuildFiles("build-44", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	stubClient.AssertExpectations(suite.T())
}

// Two attempts sharing a buildId must not share a volume name, which is what makes cleanup safe
// without an ownership check.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesNamesAreUniquePerAttempt() {
	firstStub := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	firstName, firstCall := captureCreatedVolumeName(firstStub, createErr)
	secondStub := new(testingBuildImageClient)
	secondName, secondCall := captureCreatedVolumeName(secondStub, createErr)
	suite.mocks = append(
		suite.mocks,
		firstCall,
		firstStub.On("VolumeInspect", mock.Anything, mock.Anything).
			Return(types.Volume{}, errdefs.NotFound(errors.New("no such volume"))),
		secondCall,
		secondStub.On("VolumeInspect", mock.Anything, mock.Anything).
			Return(types.Volume{}, errdefs.NotFound(errors.New("no such volume"))),
	)

	// Both volumes are never found, so cleanup runs to the end of its budget twice: the budget has
	// to shrink too, or this spins for the full wall-clock duration.
	defer shortCleanupBudget()()

	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	_, _ = newDockerImageBuilder(firstStub, stubDockerUtils).
		prepareBuildFiles("same-id", nil, "FROM scratch", "#!/bin/sh\n")
	_, _ = newDockerImageBuilder(secondStub, stubDockerUtils).
		prepareBuildFiles("same-id", nil, "FROM scratch", "#!/bin/sh\n")

	assert.NotEqual(suite.T(), *firstName, *secondName)
}

// The driver can finish creating the volume after dockerd has given up waiting, so a single
// immediate remove would see not-found and leave the image leaked. Cleanup polls instead.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRemovesVolumeThatAppearsLate() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	_, createCall := captureCreatedVolumeName(stubClient, createErr)

	defer shortCleanupBudget()()

	suite.mocks = append(
		suite.mocks,
		createCall,
		// First look: the driver has not finished creating it yet.
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-45")).
			Return(types.Volume{}, errdefs.NotFound(errors.New("no such volume"))).Once(),
		// Second look: it has appeared.
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-45")).
			Return(types.Volume{}, nil).Once(),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-45"), true).
			Return(nil).Times(1),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	_, err := builder.prepareBuildFiles("build-45", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	stubClient.AssertExpectations(suite.T())
}

// A remove that fails once must be retried inside the same budget, not abandoned: it commonly fails
// because the driver is still busy with the create it never acknowledged, which clears on its own.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRetriesRemoveAfterTransientFailure() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	_, createCall := captureCreatedVolumeName(stubClient, createErr)

	defer shortCleanupBudget()()

	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-46")).
			Return(types.Volume{}, nil).Twice(),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-46"), true).
			Return(errors.New("device or resource busy")).Once(),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-46"), true).
			Return(nil).Once(),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	_, err := builder.prepareBuildFiles("build-46", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	// Two removes: the transient failure then the success. AssertExpectations enforces both.
	stubClient.AssertExpectations(suite.T())
}

// shortCleanupBudget shrinks the shared rollback budget so exhaustion paths finish instantly, and
// restores it afterwards.
func shortCleanupBudget() func() {
	previousBudget, previousInterval := volumeCleanupBudget, volumeCleanupInterval
	volumeCleanupBudget = 20 * time.Millisecond
	volumeCleanupInterval = time.Millisecond
	return func() {
		volumeCleanupBudget, volumeCleanupInterval = previousBudget, previousInterval
	}
}

// A transient inspect failure is not not-found, and must be retried rather than abandoning cleanup:
// the daemon can be too busy to answer for the same reason the create timed out.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRetriesTransientInspectFailure() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("Client.Timeout exceeded while awaiting headers")
	_, createCall := captureCreatedVolumeName(stubClient, createErr)
	defer shortCleanupBudget()()

	suite.mocks = append(
		suite.mocks,
		createCall,
		// Not a not-found: a real failure to answer.
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-47")).
			Return(types.Volume{}, errors.New("daemon busy")).Once(),
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-47")).
			Return(types.Volume{}, nil).Once(),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-47"), true).
			Return(nil).Times(1),
	)
	builder := newDockerImageBuilder(stubClient, new(testingBuildDockerImageDockerUtils))

	_, err := builder.prepareBuildFiles("build-47", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	stubClient.AssertExpectations(suite.T())
}

// A ContainerCreate that times out returns no ID, but the daemon may have created the container
// anyway. Rollback must still remove it - by name - and only then the volume, because a surviving
// container holds the volume busy.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRollsBackContainerCreatedDespiteError() {
	stubClient := new(testingBuildImageClient)
	createdName, createCall := captureCreatedVolumeName(stubClient, nil)
	defer shortCleanupBudget()()

	containerErr := errors.New("Client.Timeout exceeded while awaiting headers")
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("ContainerCreate", mock.Anything, mock.Anything, mock.Anything, mock.Anything,
			mock.Anything, mock.MatchedBy(func(name string) bool {
				return strings.HasPrefix(name, "copy_volume_build-48_")
			})).
			Return(containerTypes.ContainerCreateCreatedBody{}, containerErr).Times(1),
		// Removed by NAME, and only on the second look: after a timed-out create the first
		// not-found proves nothing, because the daemon may still be materialising the container.
		// Accepting that first answer would abandon a container that then holds the volume.
		stubClient.On("ContainerRemove", mock.Anything, mock.MatchedBy(func(ref string) bool {
			return strings.HasPrefix(ref, "copy_volume_build-48_")
		}), mock.Anything).Return(errdefs.NotFound(errors.New("no such container"))).Once(),
		stubClient.On("ContainerRemove", mock.Anything, mock.MatchedBy(func(ref string) bool {
			return strings.HasPrefix(ref, "copy_volume_build-48_")
		}), mock.Anything).Return(nil).Once(),
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-48")).
			Return(types.Volume{}, nil).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-48"), true).
			Return(nil).Times(1),
	)
	stubUtils := new(testingBuildDockerImageDockerUtils)
	stubUtils.On("pullImage", SAVE_IMAGE, false).Return(nil)
	builder := newDockerImageBuilder(stubClient, stubUtils)

	volumeName, err := builder.prepareBuildFiles("build-48", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), containerErr, err)
	assert.Equal(suite.T(), "", volumeName)
	assert.NotEmpty(suite.T(), *createdName)
	stubClient.AssertExpectations(suite.T())
}

// A definitive daemon rejection - bad parameter, missing image, refused credential - cannot become a
// container later, so treating it as uncertain would burn the whole cleanup budget and then retain
// the volume, leaking it in what is the most common failure of all. The volume must be removed.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesRemovesVolumeOnDefinitiveContainerRejection() {
	stubClient := new(testingBuildImageClient)
	_, createCall := captureCreatedVolumeName(stubClient, nil)
	defer shortCleanupBudget()()

	rejectionErr := errdefs.InvalidParameter(errors.New("no such image: save-image:latest"))
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("ContainerCreate", mock.Anything, mock.Anything, mock.Anything, mock.Anything,
			mock.Anything, mock.Anything).
			Return(containerTypes.ContainerCreateCreatedBody{}, rejectionErr).Times(1),
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-50")).
			Return(types.Volume{}, nil).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-50"), true).
			Return(nil).Times(1),
	)
	stubUtils := new(testingBuildDockerImageDockerUtils)
	stubUtils.On("pullImage", SAVE_IMAGE, false).Return(nil)
	builder := newDockerImageBuilder(stubClient, stubUtils)

	_, err := builder.prepareBuildFiles("build-50", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), rejectionErr, err)
	// No container can exist, so none should be chased for the length of the budget.
	stubClient.AssertNotCalled(suite.T(), "ContainerRemove", mock.Anything, mock.Anything, mock.Anything)
	stubClient.AssertExpectations(suite.T())
}

// Forbidden is NOT proof of absence: Docker authorization plugins authorize in two phases, and a
// response-phase denial returns 403 after the create handler has already run, so the container may
// exist. It must therefore be chased like any uncertain create - a first not-found settles nothing.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesTreatsForbiddenAsUncertain() {
	stubClient := new(testingBuildImageClient)
	_, createCall := captureCreatedVolumeName(stubClient, nil)
	defer shortCleanupBudget()()

	forbiddenErr := errdefs.Forbidden(errors.New("authorization denied by plugin"))
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("ContainerCreate", mock.Anything, mock.Anything, mock.Anything, mock.Anything,
			mock.Anything, mock.Anything).
			Return(containerTypes.ContainerCreateCreatedBody{}, forbiddenErr).Times(1),
		// First look proves nothing, because the create outcome was uncertain.
		stubClient.On("ContainerRemove", mock.Anything, mock.Anything, mock.Anything).
			Return(errdefs.NotFound(errors.New("no such container"))).Once(),
		// It appeared, and is removed.
		stubClient.On("ContainerRemove", mock.Anything, mock.Anything, mock.Anything).
			Return(nil).Once(),
		stubClient.On("VolumeInspect", mock.Anything, hasBuildVolumePrefix("build-51")).
			Return(types.Volume{}, nil).Times(1),
		stubClient.On("VolumeRemove", mock.Anything, hasBuildVolumePrefix("build-51"), true).
			Return(nil).Times(1),
	)
	stubUtils := new(testingBuildDockerImageDockerUtils)
	stubUtils.On("pullImage", SAVE_IMAGE, false).Return(nil)
	builder := newDockerImageBuilder(stubClient, stubUtils)

	_, err := builder.prepareBuildFiles("build-51", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), forbiddenErr, err)
	stubClient.AssertExpectations(suite.T())
}

// If the container cannot be removed it still holds the volume, so attempting the volume removal is
// pointless: assert we do not try, and that the failure is not silent.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesSkipsVolumeRemovalWhenContainerSurvives() {
	stubClient := new(testingBuildImageClient)
	_, createCall := captureCreatedVolumeName(stubClient, nil)
	defer shortCleanupBudget()()

	removeAttempts := 0
	containerErr := errors.New("Client.Timeout exceeded while awaiting headers")
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("ContainerCreate", mock.Anything, mock.Anything, mock.Anything, mock.Anything,
			mock.Anything, mock.Anything).
			Return(containerTypes.ContainerCreateCreatedBody{}, containerErr).Times(1),
		// Never succeeds: budget exhausts. Counted below, because a test that tolerates a single
		// attempt would pass whether or not the retry loop exists.
		stubClient.On("ContainerRemove", mock.Anything, mock.Anything, mock.Anything).
			Run(func(mock.Arguments) { removeAttempts++ }).
			Return(errors.New("device or resource busy")),
	)
	stubUtils := new(testingBuildDockerImageDockerUtils)
	stubUtils.On("pullImage", SAVE_IMAGE, false).Return(nil)
	builder := newDockerImageBuilder(stubClient, stubUtils)

	_, err := builder.prepareBuildFiles("build-49", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), containerErr, err)
	assert.Greater(suite.T(), removeAttempts, 1, "removal must be retried, not attempted once")
	stubClient.AssertNotCalled(suite.T(), "VolumeRemove", mock.Anything, mock.Anything, mock.Anything)
	stubClient.AssertExpectations(suite.T())
}

// A cleanup that never succeeds must exhaust its budget quietly and still surface the create error,
// never substitute its own. No call counts here on purpose: cleanup retries a failing remove, so
// pinning the expectations to one call would assert the absence of the retry rather than the
// behaviour under test.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesReturnsCreateErrorWhenCleanupFails() {
	stubClient := new(testingBuildImageClient)
	createErr := errors.New("create failed")
	_, createCall := captureCreatedVolumeName(stubClient, createErr)
	// Shortens the BUDGET as well as the interval. Shrinking only the interval left the 150s
	// wall-clock deadline in place, so this test span a tight retry loop for the full duration.
	defer shortCleanupBudget()()

	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("VolumeInspect", mock.Anything, mock.Anything).
			Return(types.Volume{}, nil),
		stubClient.On("VolumeRemove", mock.Anything, mock.Anything, true).
			Return(errors.New("remove failed too")),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	_, err := builder.prepareBuildFiles("build-43", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	stubClient.AssertExpectations(suite.T())
}

// A conflict on VolumeCreate proves only that the name is taken, not that the volume is ours. The
// name carries this attempt's token, so a conflict means the token was not unique and the volume
// belongs to someone else - very likely a 5 GB rbd image another build is using. Cleanup must not
// inspect it and must not remove it. Regression test for the foreign-volume deletion finding.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesLeavesForeignVolumeAlone() {
	stubClient := new(testingBuildImageClient)
	createErr := errdefs.Conflict(errors.New("volume already exists"))
	_, createCall := captureCreatedVolumeName(stubClient, createErr)
	// No VolumeInspect and no VolumeRemove are registered: the mock fails the test if either is
	// called, which is exactly the assertion.
	suite.mocks = append(suite.mocks, createCall)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	path, err := builder.prepareBuildFiles("build-50", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	assert.Equal(suite.T(), "", path)
	// Asserted explicitly rather than relying on an unregistered call panicking: this is the
	// whole point of the test, so it should not depend on mock internals.
	stubClient.AssertNotCalled(suite.T(), "VolumeInspect", mock.Anything, mock.Anything)
	stubClient.AssertNotCalled(suite.T(), "VolumeRemove", mock.Anything, mock.Anything, mock.Anything)
	stubClient.AssertExpectations(suite.T())
}

// A definitive rejection - a missing volume driver surfaces as not-found, which is the most likely
// failure here - cannot turn into a volume later. Polling for one anyway spent the whole cleanup
// budget holding a worker-pool slot and delayed the error the caller was waiting on. Cleanup must
// not even look.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesSkipsCleanupWhenCreateWasRejected() {
	stubClient := new(testingBuildImageClient)
	createErr := errdefs.NotFound(errors.New("error looking up volume plugin"))
	_, createCall := captureCreatedVolumeName(stubClient, createErr)
	suite.mocks = append(suite.mocks, createCall)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)
	// Budget shortened even though this test asserts the poll never starts: AssertNotCalled below
	// is the real assertion, and leaving the real 150s budget in place would make a regression
	// stall CI for 150 seconds rather than fail fast.
	defer shortCleanupBudget()()

	path, err := builder.prepareBuildFiles("build-51", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), createErr, err)
	assert.Equal(suite.T(), "", path)
	stubClient.AssertNotCalled(suite.T(), "VolumeInspect", mock.Anything, mock.Anything)
	stubClient.AssertNotCalled(suite.T(), "VolumeRemove", mock.Anything, mock.Anything, mock.Anything)
	stubClient.AssertExpectations(suite.T())
}

// A conflict on ContainerCreate proves only that the container name is taken. Force-removing it
// would destroy a third party's container, and the same collision makes the volume's ownership
// unprovable, so neither may be touched. Regression test for the foreign-container deletion
// finding.
func (suite *BuildDockerImageTestSuite) TestPrepareBuildFilesLeavesForeignCopyContainerAlone() {
	stubClient := new(testingBuildImageClient)
	_, createCall := captureCreatedVolumeName(stubClient, nil)
	containerErr := errdefs.Conflict(errors.New("container name already in use"))
	// Neither ContainerRemove nor VolumeRemove is registered, so either call fails the test.
	suite.mocks = append(
		suite.mocks,
		createCall,
		stubClient.On("ContainerCreate", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).
			Return(containerTypes.ContainerCreateCreatedBody{}, containerErr).Times(1),
	)
	stubDockerUtils := new(testingBuildDockerImageDockerUtils)
	// prepareBuildFiles pulls the copy image before creating the container, so this test is the
	// first to get that far and has to stub it.
	suite.mocks = append(
		suite.mocks,
		stubDockerUtils.On("pullImage", SAVE_IMAGE, false).Return(nil).Times(1),
	)
	builder := newDockerImageBuilder(stubClient, stubDockerUtils)

	path, err := builder.prepareBuildFiles("build-52", nil, "FROM scratch", "#!/bin/sh\n")

	assert.Equal(suite.T(), containerErr, err)
	assert.Equal(suite.T(), "", path)
	// Neither the foreign container nor the volume whose ownership it casts into doubt may be
	// touched. Asserted explicitly, not left to mock internals.
	stubClient.AssertNotCalled(suite.T(), "ContainerRemove", mock.Anything, mock.Anything, mock.Anything)
	stubClient.AssertNotCalled(suite.T(), "VolumeInspect", mock.Anything, mock.Anything)
	stubClient.AssertNotCalled(suite.T(), "VolumeRemove", mock.Anything, mock.Anything, mock.Anything)
	stubClient.AssertExpectations(suite.T())
}

func TestBuildDockerImageTestSuite(t *testing.T) {
	suite.Run(t, new(BuildDockerImageTestSuite))
}
