package main

import (
	"bytes"
	"context"
	"encoding/binary"
	"errors"
	"io"
	"testing"
	"time"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/pkg/stdcopy"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"
)

type testingLogCollectorClient struct {
	mock.Mock
	logCollectorDockerClient
}

func (t *testingLogCollectorClient) ServiceList(
	ctx context.Context, options types.ServiceListOptions,
) ([]swarm.Service, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Service), args.Error(1)
}

func (t *testingLogCollectorClient) TaskList(
	ctx context.Context, options types.TaskListOptions,
) ([]swarm.Task, error) {
	args := t.Called(ctx, options)
	return args.Get(0).([]swarm.Task), args.Error(1)
}

func (t *testingLogCollectorClient) TaskLogs(
	ctx context.Context, taskID string, options types.ContainerLogsOptions,
) (io.ReadCloser, error) {
	args := t.Called(ctx, taskID, options)
	reader, _ := args.Get(0).(io.ReadCloser)
	return reader, args.Error(1)
}

func (t *testingLogCollectorClient) NodeInspectWithRaw(
	ctx context.Context, nodeID string,
) (swarm.Node, []byte, error) {
	args := t.Called(ctx, nodeID)
	return args.Get(0).(swarm.Node), nil, args.Error(1)
}

// dockerStream frames text the way the Docker daemon does, so the tests read
// the same bytes the SDK would hand us.
func dockerStream(lines ...string) io.ReadCloser {
	buffer := new(bytes.Buffer)
	for _, line := range lines {
		payload := []byte(line)
		header := make([]byte, 8)
		header[0] = byte(stdcopy.Stdout)
		binary.BigEndian.PutUint32(header[4:], uint32(len(payload)))
		buffer.Write(header)
		buffer.Write(payload)
	}
	return io.NopCloser(buffer)
}

func node(id string, hostname string) swarm.Node {
	node := swarm.Node{}
	node.ID = id
	node.Description.Hostname = hostname
	return node
}

func task(id string, slot int, nodeID string, createdAt time.Time) swarm.Task {
	return swarm.Task{
		ID:     id,
		Slot:   slot,
		NodeID: nodeID,
		Meta:   swarm.Meta{CreatedAt: createdAt},
	}
}

type GetLogsTestSuite struct {
	suite.Suite
	client  *testingLogCollectorClient
	service swarm.Service
}

func (s *GetLogsTestSuite) SetupTest() {
	c = config{LogsTimestamps: false, LogsDetails: false, LogsTailLines: 2000}

	s.service = swarm.Service{}
	s.service.ID = "service-1"
	s.service.Spec.Name = "42-7"
	s.service.Spec.TaskTemplate.ContainerSpec = &swarm.ContainerSpec{
		Image: "harbor.example.org/mydocker/lab:1.2.3",
	}

	s.client = new(testingLogCollectorClient)
	s.client.On("ServiceList", mock.Anything, mock.Anything).
		Return([]swarm.Service{s.service}, nil)
	s.client.On("NodeInspectWithRaw", mock.Anything, "node-a").
		Return(node("node-a", "worker-01"), nil)
	s.client.On("NodeInspectWithRaw", mock.Anything, "node-b").
		Return(node("node-b", "worker-02"), nil)
}

func (s *GetLogsTestSuite) TestReturnsNameAndImageUntouched() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{task("t1", 1, "node-a", base)}, nil)
	s.client.On("TaskLogs", mock.Anything, "t1", mock.Anything).
		Return(dockerStream("hello\n"), nil)

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	assert.Equal(s.T(), "42-7", response.GetName())
	// The registry host stays on the wire; hiding it is the front end's job.
	assert.Equal(s.T(), "harbor.example.org/mydocker/lab:1.2.3", response.GetImage())
}

func (s *GetLogsTestSuite) TestOrdersBySlotThenCreationTime() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		task("t-slot2", 2, "node-b", base.Add(time.Minute)),
		task("t-slot1-retry", 1, "node-a", base.Add(time.Hour)),
		task("t-slot1-first", 1, "node-a", base),
	}, nil)
	for _, id := range []string{"t-slot2", "t-slot1-retry", "t-slot1-first"} {
		s.client.On("TaskLogs", mock.Anything, id, mock.Anything).
			Return(dockerStream("output\n"), nil)
	}

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	require.Len(s.T(), response.GetLogs(), 3)
	assert.Equal(s.T(), "t-slot1-first", response.GetLogs()[0].GetTaskID())
	assert.Equal(s.T(), "t-slot1-retry", response.GetLogs()[1].GetTaskID())
	assert.Equal(s.T(), "t-slot2", response.GetLogs()[2].GetTaskID())
}

func (s *GetLogsTestSuite) TestKeepsARestartAsItsOwnEntry() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		task("t-first", 1, "node-a", base),
		task("t-second", 1, "node-b", base.Add(time.Minute)),
	}, nil)
	s.client.On("TaskLogs", mock.Anything, "t-first", mock.Anything).
		Return(dockerStream("crashed\n"), nil)
	s.client.On("TaskLogs", mock.Anything, "t-second", mock.Anything).
		Return(dockerStream("recovered\n"), nil)

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	require.Len(s.T(), response.GetLogs(), 2)
	assert.Equal(s.T(), "crashed", response.GetLogs()[0].GetLogs())
	assert.Equal(s.T(), "worker-01", response.GetLogs()[0].GetNode())
	assert.Equal(s.T(), "recovered", response.GetLogs()[1].GetLogs())
	assert.Equal(s.T(), "worker-02", response.GetLogs()[1].GetNode())
	assert.Equal(s.T(), uint32(1), response.GetLogs()[0].GetSlot())
}

func (s *GetLogsTestSuite) TestSkipsTasksWithoutOutputAndKeepsFailedOnesThatHaveIt() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		task("t-silent", 1, "node-a", base),
		task("t-failed", 2, "node-a", base),
	}, nil)
	s.client.On("TaskLogs", mock.Anything, "t-silent", mock.Anything).
		Return(dockerStream(), nil)
	s.client.On("TaskLogs", mock.Anything, "t-failed", mock.Anything).
		Return(dockerStream("panic: it broke\n"), nil)

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	require.Len(s.T(), response.GetLogs(), 1)
	assert.Equal(s.T(), "t-failed", response.GetLogs()[0].GetTaskID())
}

func (s *GetLogsTestSuite) TestOneUnreadableTaskDoesNotLoseTheOthers() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		task("t-broken", 1, "node-a", base),
		task("t-fine", 2, "node-a", base),
	}, nil)
	s.client.On("TaskLogs", mock.Anything, "t-broken", mock.Anything).
		Return(nil, errors.New("no such task"))
	s.client.On("TaskLogs", mock.Anything, "t-fine", mock.Anything).
		Return(dockerStream("still here\n"), nil)

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	require.Len(s.T(), response.GetLogs(), 1)
	assert.Equal(s.T(), "t-fine", response.GetLogs()[0].GetTaskID())
}

func (s *GetLogsTestSuite) TestTruncatesToTheTailAndSaysSo() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{task("t1", 1, "node-a", base)}, nil)
	s.client.On("TaskLogs", mock.Anything, "t1", mock.Anything).
		Return(dockerStream("first\n", "second\n", "third\n"), nil)

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2)

	require.NoError(s.T(), err)
	require.Len(s.T(), response.GetLogs(), 1)
	assert.Equal(s.T(), "second\nthird", response.GetLogs()[0].GetLogs())
	assert.True(s.T(), response.GetLogs()[0].GetTruncated())
}

func (s *GetLogsTestSuite) TestUntruncatedOutputIsNotFlagged() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{task("t1", 1, "node-a", base)}, nil)
	s.client.On("TaskLogs", mock.Anything, "t1", mock.Anything).
		Return(dockerStream("only line\n"), nil)

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	assert.False(s.T(), response.GetLogs()[0].GetTruncated())
}

func (s *GetLogsTestSuite) TestFallsBackToTheNodeIdWhenInspectFails() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{task("t1", 1, "node-missing", base)}, nil)
	s.client.On("TaskLogs", mock.Anything, "t1", mock.Anything).
		Return(dockerStream("output\n"), nil)
	s.client.On("NodeInspectWithRaw", mock.Anything, "node-missing").
		Return(swarm.Node{}, errors.New("node is gone"))

	response, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	assert.Equal(s.T(), "node-missing", response.GetLogs()[0].GetNode())
}

func (s *GetLogsTestSuite) TestInspectsEachNodeOnceForSeveralTasks() {
	base := time.Now()
	s.client.On("TaskList", mock.Anything, mock.Anything).Return([]swarm.Task{
		task("t1", 1, "node-a", base),
		task("t2", 2, "node-a", base),
		task("t3", 3, "node-a", base),
	}, nil)
	for _, id := range []string{"t1", "t2", "t3"} {
		s.client.On("TaskLogs", mock.Anything, id, mock.Anything).
			Return(dockerStream("output\n"), nil)
	}

	_, err := collectLogs(context.Background(), s.client, "42", "7", 2000)

	require.NoError(s.T(), err)
	s.client.AssertNumberOfCalls(s.T(), "NodeInspectWithRaw", 1)
}

func TestGetLogsTestSuite(t *testing.T) {
	suite.Run(t, new(GetLogsTestSuite))
}

func TestReadTaskLogsKeepsShortLines(t *testing.T) {
	// The reader this replaces dropped every line of eight bytes or fewer.
	logs, truncated, err := readTaskLogs(dockerStream("ok\n", "done\n", "x\n"), 0)

	require.NoError(t, err)
	assert.Equal(t, "ok\ndone\nx", logs)
	assert.False(t, truncated)
}

func TestReadTaskLogsKeepsALineWithNoTrailingNewline(t *testing.T) {
	logs, _, err := readTaskLogs(dockerStream("first\n", "second without newline"), 0)

	require.NoError(t, err)
	assert.Equal(t, "first\nsecond without newline", logs)
}

func TestReadTaskLogsHandlesAFrameSplitAcrossLines(t *testing.T) {
	logs, _, err := readTaskLogs(dockerStream("one\ntwo\nthree\n"), 0)

	require.NoError(t, err)
	assert.Equal(t, "one\ntwo\nthree", logs)
}

func TestReadTaskLogsReturnsEmptyForNoOutput(t *testing.T) {
	logs, truncated, err := readTaskLogs(dockerStream(), 0)

	require.NoError(t, err)
	assert.Equal(t, "", logs)
	assert.False(t, truncated)
}
