package main

import (
	"context"
	"testing"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

type testingScheduledServiceCleanerClient struct {
	mock.Mock
}

func newTestingScheduledServiceCleanerClient(t *testing.T) *testingScheduledServiceCleanerClient {
	t.Helper()
	client := new(testingScheduledServiceCleanerClient)
	client.Test(t)
	t.Cleanup(func() { client.AssertExpectations(t) })
	return client
}

func (c *testingScheduledServiceCleanerClient) ServiceList(ctx context.Context, options types.ServiceListOptions) ([]swarm.Service, error) {
	args := c.Called(ctx, options)
	return args.Get(0).([]swarm.Service), args.Error(1)
}

func (c *testingScheduledServiceCleanerClient) ServiceRemove(ctx context.Context, serviceID string) error {
	args := c.Called(ctx, serviceID)
	return args.Error(0)
}

func TestServiceWithoutDeletionTimeIsNotRemoved(t *testing.T) {
	client := newTestingScheduledServiceCleanerClient(t)
	client.On("ServiceList", mock.Anything, mock.Anything).Return([]swarm.Service{{
		ID: "lab-without-deletion-time",
		Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Labels: map[string]string{
			"deleteAfter": "true",
		}}},
	}}, nil)

	findServiceToDeleteAndDoIt(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

func TestServiceWithMalformedDeletionTimeIsNotRemoved(t *testing.T) {
	client := newTestingScheduledServiceCleanerClient(t)
	client.On("ServiceList", mock.Anything, mock.Anything).Return([]swarm.Service{{
		ID: "lab-with-malformed-deletion-time",
		Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Labels: map[string]string{
			"deleteAfter":  "true",
			"deletionTime": "not-a-timestamp",
		}}},
	}}, nil)

	findServiceToDeleteAndDoIt(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

// A lab still inside its lease must survive every tick until that lease expires. This is the
// case that protects a running session, and the one a refactor of the comparison would break.
func TestServiceBeforeItsDeletionTimeIsKept(t *testing.T) {
	client := newTestingScheduledServiceCleanerClient(t)
	client.On("ServiceList", mock.Anything, mock.Anything).Return([]swarm.Service{{
		ID: "running-lab",
		Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Labels: map[string]string{
			"deleteAfter":  "true",
			"deletionTime": "99999999999",
		}}},
	}}, nil)

	findServiceToDeleteAndDoIt(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

// A listing failure must stop the sweep rather than fall through into the loop. The list carries
// an expired service on purpose: returned empty, this passes without the return too, because an
// empty loop removes nothing either way. A real client returns no services with its error, so the
// payload is synthetic; the control flow is the subject.
func TestServiceListFailureStopsTheSweep(t *testing.T) {
	client := newTestingScheduledServiceCleanerClient(t)
	client.On("ServiceList", mock.Anything, mock.Anything).Return([]swarm.Service{{
		ID: "expired-lab-in-a-failed-listing",
		Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Labels: map[string]string{
			"deleteAfter":  "true",
			"deletionTime": "1",
		}}},
	}}, assert.AnError)

	findServiceToDeleteAndDoIt(client)

	client.AssertNotCalled(t, "ServiceRemove", mock.Anything, mock.Anything)
}

func TestServicePastItsDeletionTimeIsRemoved(t *testing.T) {
	client := newTestingScheduledServiceCleanerClient(t)
	client.On("ServiceList", mock.Anything, mock.Anything).Return([]swarm.Service{{
		ID: "expired-lab",
		Spec: swarm.ServiceSpec{Annotations: swarm.Annotations{Labels: map[string]string{
			"deleteAfter":  "true",
			"deletionTime": "1",
		}}},
	}}, nil)
	client.On("ServiceRemove", mock.Anything, "expired-lab").Return(nil)

	findServiceToDeleteAndDoIt(client)

	client.AssertCalled(t, "ServiceRemove", mock.Anything, "expired-lab")
}
