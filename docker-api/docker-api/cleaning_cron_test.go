package main

import (
	"context"
	"testing"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

type testingReaperClient struct {
	mock.Mock
}

func newTestingReaperClient(t *testing.T) *testingReaperClient {
	t.Helper()
	client := new(testingReaperClient)
	client.Test(t)
	t.Cleanup(func() { client.AssertExpectations(t) })
	return client
}

func (c *testingReaperClient) TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error) {
	args := c.Called(ctx, options)
	return args.Get(0).([]swarm.Task), args.Error(1)
}

func (c *testingReaperClient) ServiceInspectWithRaw(ctx context.Context, serviceID string, options types.ServiceInspectOptions) (swarm.Service, []byte, error) {
	args := c.Called(ctx, serviceID, options)
	return args.Get(0).(swarm.Service), nil, args.Error(1)
}

func (c *testingReaperClient) ServiceRemove(ctx context.Context, serviceID string) error {
	args := c.Called(ctx, serviceID)
	return args.Error(0)
}

func completedTask(serviceID string) swarm.Task {
	return swarm.Task{
		ID:        serviceID + "-task",
		ServiceID: serviceID,
		Status:    swarm.TaskStatus{State: swarm.TaskStateComplete},
	}
}

func serviceWithLabels(name string, labels map[string]string) swarm.Service {
	return swarm.Service{
		Spec: swarm.ServiceSpec{
			Annotations: swarm.Annotations{Name: name, Labels: labels},
		},
	}
}

// A finished one-shot job is what this reaper exists for: its task cannot be removed on its
// own, so the service goes.
func TestCompletedOneShotServiceIsRemoved(t *testing.T) {
	client := newTestingReaperClient(t)
	client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{completedTask("build-1")}, nil)
	client.On("ServiceInspectWithRaw", mock.Anything, "build-1", mock.Anything).
		Return(serviceWithLabels("build-1", map[string]string{oneShotLabel: "true"}), nil)
	client.On("ServiceRemove", mock.Anything, "build-1").Return(nil)

	reapCompletedOneShotServices(client)

	client.AssertCalled(t, "ServiceRemove", mock.Anything, "build-1")
}

// The regression this fix exists for. On 2026-09-16 a reboot ended the caddy, postgres and
// frontend tasks with exit 0, and the unscoped version of this function removed all three
// services 22 seconds after boot. A clean exit is not a reason to delete a service.
func TestCompletedTaskOnALongRunningServiceIsLeftAlone(t *testing.T) {
	client := newTestingReaperClient(t)
	client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{completedTask("caddy")}, nil)
	client.On("ServiceInspectWithRaw", mock.Anything, "caddy", mock.Anything).
		Return(serviceWithLabels("mydocker_caddy", nil), nil)

	reapCompletedOneShotServices(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

// Student labs carry deleteAfter/deletionTime and belong to the other sweep, which honours
// the deletion time. Reaping them here would cut a session short the moment a lab container
// exited cleanly.
func TestCompletedLabServiceIsLeftToTheDeletionTimeSweep(t *testing.T) {
	client := newTestingReaperClient(t)
	client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{completedTask("42-7")}, nil)
	client.On("ServiceInspectWithRaw", mock.Anything, "42-7", mock.Anything).
		Return(serviceWithLabels("42-7", map[string]string{
			"deleteAfter":  "true",
			"deletionTime": "99999999999",
		}), nil)

	reapCompletedOneShotServices(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

// A task that failed is not a completed job, and must not be inspected or removed. This is
// what kept the defect invisible on the production clusters: their containers are killed, so
// their tasks end failed rather than complete.
func TestFailedTaskIsIgnored(t *testing.T) {
	client := newTestingReaperClient(t)
	failed := completedTask("build-2")
	failed.Status.State = swarm.TaskStateFailed
	client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{failed}, nil)

	reapCompletedOneShotServices(client)

	client.AssertNotCalled(t, "ServiceInspectWithRaw", mock.Anything, mock.Anything, mock.Anything)
	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

// A task whose service has already gone is nothing to clean up, and must not take anything
// else with it.
func TestUninspectableServiceIsSkipped(t *testing.T) {
	client := newTestingReaperClient(t)
	client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{completedTask("gone")}, nil)
	client.On("ServiceInspectWithRaw", mock.Anything, "gone", mock.Anything).
		Return(swarm.Service{}, assert.AnError)

	reapCompletedOneShotServices(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

// A listing failure must stop the sweep rather than fall through into the loop. The list
// carries a reapable one-shot on purpose: returned empty, this test passes against the
// unfixed code too, because an empty loop removes nothing either way. A real client returns
// no tasks with its error, so the payload is synthetic; the control flow is the subject.
func TestTaskListFailureStopsTheSweep(t *testing.T) {
	client := newTestingReaperClient(t)
	client.On("TaskList", mock.Anything, mock.Anything).
		Return([]swarm.Task{completedTask("build-3")}, assert.AnError)

	reapCompletedOneShotServices(client)

	client.AssertNotCalled(t, "ServiceInspectWithRaw", mock.Anything, mock.Anything, mock.Anything)
	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}
