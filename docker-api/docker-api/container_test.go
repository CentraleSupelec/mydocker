package main

import (
	"context"
	"os"
	"os/exec"
	"path/filepath"
	"testing"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/mount"
	"github.com/docker/docker/api/types/swarm"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"

	"github.com/Workiva/go-datastructures/queue"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/client"
	"github.com/stretchr/testify/assert"
)

func TestGetOrCreateContainer(t *testing.T) {
	cli, err := client.NewClientWithOpts(client.WithVersion("1.39"))
	if err != nil {
		t.Skipf("Skipping test because Docker client is not available : %v", err)
		return
	}
	info, err := cli.Info(context.TODO())
	if err != nil {
		t.Skipf("Skipping test because Docker info is not available : %v", err)
		return
	}
	if info.Swarm.ControlAvailable != true {
		t.Skip("Skipping test because node is not a swarm manager")
		return
	}
	c = config{
		PortThreshold: 75,
		PortSize:      150,
		PortMin:       10000,
		PortMax:       15000,
	}
	log.SetLevel(log.DebugLevel)
	in, out := make(chan *pb.ContainerRequest, c.Worker), make(chan *pb.ContainerResponse, c.Worker)
	createRbdImageIn, createRbdImageOut := make(chan CreateRbdImageWorkerRequest, c.Worker), make(chan CreateRbdImageWorkerResponse, c.Worker)

	if err != nil {
		t.Error(err)
	}

	ports := queue.New(c.PortSize)
	ctx := context.Background()
	go portsWorker(ctx, ports, cli)

	go getOrCreateContainer(in, out, cli, ports, createRbdImageIn, createRbdImageOut, 0)

	r := &pb.ContainerRequest{
		UserID:   "test",
		CourseID: "test",
		ImageID:  "rastasheep/ubuntu-sshd:latest",
		Metadata: &pb.Metadata{},
		Options:  &pb.ContainerRequestOptions{},
		Ports: []*pb.RequestPort{
			{
				PortToMap: 8888,
			},
		},
	}

	in <- r

	select {
	case data := <-out:
		assert.Equal(t, data.GetPorts()[0].GetMapTo(), uint32(10000))
		t.Log(data.IpAddress)
	}

	responsePorts := map[uint32]*pb.ResponsePort{
		uint32(8888): {MapTo: 10000, PortToMap: 8888},
	}
	here, id, _, _, _, _, _, err := exist("test-test", responsePorts, cli)
	assert.Nil(t, err)
	assert.True(t, here)

	err = deleteService(id, cli)
	assert.Nil(t, err)

	here, id, _, _, _, _, _, err = exist("test-test", responsePorts, cli)
	assert.False(t, here)
	assert.Nil(t, err)
}

type testingCreateStudentVolumeDockerClient struct {
	mock.Mock
	createStudentVolumeDockerClient
}

func (t *testingCreateStudentVolumeDockerClient) ServiceList(ctx context.Context, options types.ServiceListOptions) ([]swarm.Service, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Service), args.Error(1)
}

type testingDockerExistenceCheckClientlient struct {
	mock.Mock
	dockerExistenceCheckClient
}

func (t *testingDockerExistenceCheckClientlient) TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Task), args.Error(1)
}

type ContainerTestSuite struct {
	suite.Suite
	mocks []*mock.Call
}

func (suite *ContainerTestSuite) SetupTest() {
	suite.mocks = []*mock.Call{}
}

func (suite *ContainerTestSuite) AfterTest() {
	for _, m := range suite.mocks {
		m.Unset()
	}
}

func (suite *ContainerTestSuite) TestCanCreateStudentVolumeNoService() {
	stubClient := new(testingCreateStudentVolumeDockerClient)
	suite.mocks = append(suite.mocks,
		stubClient.On(
			"ServiceList", mock.Anything, mock.Anything,
		).Return(make([]swarm.Service, 0), nil).Times(1),
	)
	result, err := canCreateStudentVolume(stubClient, &pb.ContainerRequest{
		UserID: "1",
		Options: &pb.ContainerRequestOptions{
			StorageBackend: pb.StorageBackend_RBD,
		},
	})
	suite.Nil(err)
	suite.Equal(true, result)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestShouldBeReplaced() {
	stubClient := new(testingDockerExistenceCheckClientlient)
	matcher := func(serviceName string) func(options types.TaskListOptions) bool {
		return func(options types.TaskListOptions) bool {
			return findStringInListCaseInsensitive(serviceName, options.Filters.Get("service"))
		}
	}
	suite.mocks = append(suite.mocks, stubClient.
		On(
			"TaskList", mock.Anything, mock.MatchedBy(matcher("startingService")),
		).
		Return([]swarm.Task{
			{
				Status: swarm.TaskStatus{State: swarm.TaskStateStarting},
			},
		}, nil))
	suite.mocks = append(suite.mocks, stubClient.
		On(
			"TaskList", mock.Anything, mock.MatchedBy(matcher("errorService")),
		).
		Return([]swarm.Task{
			{
				Status: swarm.TaskStatus{State: swarm.TaskStateStarting},
			},
			{
				Status: swarm.TaskStatus{State: swarm.TaskStateFailed},
			},
		}, nil),
	)
	suite.mocks = append(suite.mocks, stubClient.
		On(
			"TaskList", mock.Anything, mock.MatchedBy(matcher("completedService")),
		).
		Return([]swarm.Task{
			{
				Status: swarm.TaskStatus{State: swarm.TaskStateComplete},
			},
		}, nil),
	)

	runningService := swarm.Service{Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Name: "runningService"}}, ServiceStatus: &swarm.ServiceStatus{
		RunningTasks: 1,
		DesiredTasks: 1,
	}}
	result, err := shouldServiceBeReplaced(runningService, stubClient)
	suite.False(result)
	suite.Nil(err)
	startingService := swarm.Service{Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Name: "startingService"}}, ServiceStatus: &swarm.ServiceStatus{
		RunningTasks: 0,
		DesiredTasks: 1,
	}}
	result, err = shouldServiceBeReplaced(startingService, stubClient)
	suite.False(result)
	suite.Nil(err)
	errorService := swarm.Service{Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Name: "errorService"}}, ServiceStatus: &swarm.ServiceStatus{
		RunningTasks: 0,
		DesiredTasks: 1,
	}}
	result, err = shouldServiceBeReplaced(errorService, stubClient)
	suite.True(result)
	suite.Nil(err)
	completedService := swarm.Service{Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Name: "completedService"}}, ServiceStatus: &swarm.ServiceStatus{
		RunningTasks: 0,
		DesiredTasks: 1,
	}}
	result, err = shouldServiceBeReplaced(completedService, stubClient)
	suite.True(result)
	suite.Nil(err)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestCanCreateStudentVolumeServiceNoMount() {
	stubClient := new(testingCreateStudentVolumeDockerClient)
	suite.mocks = append(suite.mocks,
		stubClient.On(
			"ServiceList", mock.Anything, mock.Anything,
		).Return([]swarm.Service{
			{
				ID: "serviceID",
				Spec: swarm.ServiceSpec{
					TaskTemplate: swarm.TaskSpec{
						ContainerSpec: &swarm.ContainerSpec{
							Mounts: []mount.Mount{
								{
									Source: "another-source",
								},
							},
						},
					},
				},
			},
		}, nil).Times(1),
	)
	result, err := canCreateStudentVolume(stubClient, &pb.ContainerRequest{
		UserID: "1",
		Options: &pb.ContainerRequestOptions{
			StorageBackend: pb.StorageBackend_RBD,
		},
	})
	suite.Nil(err)
	suite.Equal(true, result)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestCanCreateStudentVolumeServiceMount() {
	stubClient := new(testingCreateStudentVolumeDockerClient)
	suite.mocks = append(suite.mocks,
		stubClient.On(
			"ServiceList", mock.Anything, mock.Anything,
		).Return([]swarm.Service{
			{
				ID: "serviceID",
				Spec: swarm.ServiceSpec{
					TaskTemplate: swarm.TaskSpec{
						ContainerSpec: &swarm.ContainerSpec{
							Mounts: []mount.Mount{
								{
									Source: "student-1",
								},
							},
						},
					},
				},
			},
		}, nil).Times(1),
	)
	result, err := canCreateStudentVolume(stubClient, &pb.ContainerRequest{
		UserID: "1",
		Options: &pb.ContainerRequestOptions{
			StorageBackend: pb.StorageBackend_RBD,
		},
	})
	suite.NotNil(err)
	suite.Equal(false, result)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestCanCreateStudentVolumeNFS() {
	stubClient := new(testingCreateStudentVolumeDockerClient)
	stubClient.AssertNotCalled(suite.T(), "ServiceList", mock.Anything, mock.Anything)
	result, err := canCreateStudentVolume(stubClient, &pb.ContainerRequest{
		UserID: "1",
		Options: &pb.ContainerRequestOptions{
			StorageBackend: pb.StorageBackend_NFS,
		},
	})
	suite.Nil(err)
	suite.Equal(true, result)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestCanCreateStudentVolumeLocal() {
	stubClient := new(testingCreateStudentVolumeDockerClient)
	stubClient.AssertNotCalled(suite.T(), "ServiceList", mock.Anything, mock.Anything)
	result, err := canCreateStudentVolume(stubClient, &pb.ContainerRequest{
		UserID: "1",
		Options: &pb.ContainerRequestOptions{
			StorageBackend: pb.StorageBackend_LOCAL,
		},
	})
	suite.NotNil(err)
	suite.Equal(false, result)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestCanCreateStudentVolumeUnknown() {
	stubClient := new(testingCreateStudentVolumeDockerClient)
	stubClient.AssertNotCalled(suite.T(), "ServiceList", mock.Anything, mock.Anything)
	result, err := canCreateStudentVolume(stubClient, &pb.ContainerRequest{
		UserID: "1",
		Options: &pb.ContainerRequestOptions{
			StorageBackend: 3,
		},
	})
	suite.NotNil(err)
	suite.Equal(false, result)
	stubClient.AssertExpectations(suite.T())
}

func (suite *ContainerTestSuite) TestBuildSmartChmodCommandsNonRecursive() {
	tmpDir, err := os.MkdirTemp("", "chmodtest-nr-*")
	suite.Require().NoError(err)
	defer os.RemoveAll(tmpDir)

	jupyterDir := filepath.Join(tmpDir, ".local", "share", "jupyter")
	testDir := filepath.Join(jupyterDir, "test")
	suite.Require().NoError(os.MkdirAll(testDir, 0o555))

	files := map[string]os.FileMode{
		filepath.Join(jupyterDir, "exec.py"):    0o555,
		filepath.Join(jupyterDir, "no_exec.py"): 0o444,
		filepath.Join(testDir, "exec.py"):       0o555,
		filepath.Join(testDir, "no_exec.py"):    0o444,
	}
	for path, mode := range files {
		suite.Require().NoError(os.WriteFile(path, []byte("print('hi')"), mode))
	}

	mode := uint32(777)
	pattern := "*/.local/share/jupyter"

	cmds := buildChmodCommandsToApplyExecuteOnlyToFolders(tmpDir, pattern, mode, false)
	for _, cmd := range cmds {
		suite.Require().NoError(exec.Command("bash", "-c", cmd).Run(), cmd)
	}

	info, err := os.Stat(jupyterDir)
	suite.Require().NoError(err)
	suite.Equal(os.FileMode(0o777), info.Mode().Perm(), "jupyter dir should be 777")

	info, _ = os.Stat(filepath.Join(jupyterDir, "exec.py"))
	suite.Equal(os.FileMode(0o555), info.Mode().Perm(), "exec.py unchanged in non-recursive")
	info, _ = os.Stat(filepath.Join(jupyterDir, "no_exec.py"))
	suite.Equal(os.FileMode(0o444), info.Mode().Perm(), "no_exec.py unchanged in non-recursive")

	info, _ = os.Stat(testDir)
	suite.Equal(os.FileMode(0o555), info.Mode().Perm(), "testDir unchanged in non-recursive")
}

func (suite *ContainerTestSuite) TestBuildSmartChmodCommandsRecursive() {
	tmpDir, err := os.MkdirTemp("", "chmodtest-r-*")
	suite.Require().NoError(err)
	defer os.RemoveAll(tmpDir)

	jupyterDir := filepath.Join(tmpDir, ".local", "share", "jupyter")
	testDir := filepath.Join(jupyterDir, "test")
	suite.Require().NoError(os.MkdirAll(testDir, 0o555))

	files := map[string]os.FileMode{
		filepath.Join(jupyterDir, "exec.py"):    0o555,
		filepath.Join(jupyterDir, "no_exec.py"): 0o444,
		filepath.Join(testDir, "exec.py"):       0o555,
		filepath.Join(testDir, "no_exec.py"):    0o444,
	}
	for path, mode := range files {
		suite.Require().NoError(os.WriteFile(path, []byte("print('hi')"), mode))
	}

	mode := uint32(777)
	pattern := "*/.local/share/jupyter"

	cmds := buildChmodCommandsToApplyExecuteOnlyToFolders(tmpDir, pattern, mode, true)
	for _, cmd := range cmds {
		suite.Require().NoError(exec.Command("bash", "-c", cmd).Run(), cmd)
	}

	checks := map[string]os.FileMode{
		jupyterDir:                              0o777,
		filepath.Join(jupyterDir, "exec.py"):    0o777,
		filepath.Join(jupyterDir, "no_exec.py"): 0o666,
		testDir:                                 0o777,
		filepath.Join(testDir, "exec.py"):       0o777,
		filepath.Join(testDir, "no_exec.py"):    0o666,
	}

	for path, expectedPerm := range checks {
		info, err := os.Stat(path)
		suite.Require().NoError(err)
		suite.Equal(expectedPerm, info.Mode().Perm(), "path: %s", path)
	}
}

func (suite *ContainerTestSuite) TestBuildSmartChmodCommandsWithFolderPatternNonRecursive() {
	tmpDir, err := os.MkdirTemp("", "chmodtest2-nr-*")
	suite.Require().NoError(err)
	defer os.RemoveAll(tmpDir)

	parentDir := filepath.Join(tmpDir, "folder")
	testDir := filepath.Join(parentDir, "test")
	innerFolder := filepath.Join(testDir, "folder")

	for _, dir := range []string{parentDir, testDir, innerFolder} {
		suite.Require().NoError(os.MkdirAll(dir, 0o555))
	}

	files := map[string]os.FileMode{
		filepath.Join(parentDir, "exec.py"):      0o555,
		filepath.Join(parentDir, "no_exec.py"):   0o444,
		filepath.Join(testDir, "exec.py"):        0o555,
		filepath.Join(testDir, "no_exec.py"):     0o444,
		filepath.Join(innerFolder, "exec.py"):    0o555,
		filepath.Join(innerFolder, "no_exec.py"): 0o444,
	}
	for path, mode := range files {
		suite.Require().NoError(os.WriteFile(path, []byte("print('hi')"), mode))
	}

	mode := uint32(777)
	pattern := "*/folder"

	cmds := buildChmodCommandsToApplyExecuteOnlyToFolders(tmpDir, pattern, mode, false)
	for _, cmd := range cmds {
		suite.Require().NoError(exec.Command("bash", "-c", cmd).Run(), cmd)
	}

	info, _ := os.Stat(parentDir)
	suite.Equal(os.FileMode(0o777), info.Mode().Perm(), "parentDir should be 777")

	info, _ = os.Stat(testDir)
	suite.Equal(os.FileMode(0o555), info.Mode().Perm(), "testDir unchanged in non-recursive")

	info, _ = os.Stat(innerFolder)
	suite.Equal(os.FileMode(0o777), info.Mode().Perm(), "innerFolder should be 777")

	for path, expected := range files {
		info, _ := os.Stat(path)
		suite.Equal(expected, info.Mode().Perm(), "file unchanged in non-recursive: %s", path)
	}
}

func (suite *ContainerTestSuite) TestBuildSmartChmodCommandsWithFolderPatternRecursive() {
	tmpDir, err := os.MkdirTemp("", "chmodtest2-r-*")
	suite.Require().NoError(err)
	defer os.RemoveAll(tmpDir)

	parentDir := filepath.Join(tmpDir, "folder")
	testDir := filepath.Join(parentDir, "test")
	innerFolder := filepath.Join(testDir, "folder")

	for _, dir := range []string{parentDir, testDir, innerFolder} {
		suite.Require().NoError(os.MkdirAll(dir, 0o555))
	}

	files := map[string]os.FileMode{
		filepath.Join(parentDir, "exec.py"):      0o555,
		filepath.Join(parentDir, "no_exec.py"):   0o444,
		filepath.Join(testDir, "exec.py"):        0o555,
		filepath.Join(testDir, "no_exec.py"):     0o444,
		filepath.Join(innerFolder, "exec.py"):    0o555,
		filepath.Join(innerFolder, "no_exec.py"): 0o444,
	}
	for path, mode := range files {
		suite.Require().NoError(os.WriteFile(path, []byte("print('hi')"), mode))
	}

	mode := uint32(777)
	pattern := "*/folder"

	cmds := buildChmodCommandsToApplyExecuteOnlyToFolders(tmpDir, pattern, mode, true)
	for _, cmd := range cmds {
		suite.Require().NoError(exec.Command("bash", "-c", cmd).Run(), cmd)
	}

	checks := map[string]os.FileMode{
		parentDir:                                0o777,
		filepath.Join(parentDir, "exec.py"):      0o777,
		filepath.Join(parentDir, "no_exec.py"):   0o666,
		testDir:                                  0o777,
		filepath.Join(testDir, "exec.py"):        0o777,
		filepath.Join(testDir, "no_exec.py"):     0o666,
		innerFolder:                              0o777,
		filepath.Join(innerFolder, "exec.py"):    0o777,
		filepath.Join(innerFolder, "no_exec.py"): 0o666,
	}

	for path, expected := range checks {
		info, err := os.Stat(path)
		suite.Require().NoError(err)
		suite.Equal(expected, info.Mode().Perm(), "path: %s", path)
	}
}

func TestContainerSuite(t *testing.T) {
	suite.Run(t, new(ContainerTestSuite))
}
