package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/Workiva/go-datastructures/queue"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// fakeDaemon answers the Docker API calls getOrCreateContainer makes: one ready
// worker node, no existing service, and serviceCreate for the service creation.
func fakeDaemon(t *testing.T, nodes http.HandlerFunc, serviceCreate http.HandlerFunc) *client.Client {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch {
		case strings.HasSuffix(r.URL.Path, "/nodes"):
			nodes(w, r)
		case strings.HasSuffix(r.URL.Path, "/services/create"):
			serviceCreate(w, r)
		case strings.HasSuffix(r.URL.Path, "/services"):
			writeJson(w, http.StatusOK, []swarm.Service{})
		default:
			writeJson(w, http.StatusNotFound, types.ErrorResponse{Message: "unexpected " + r.URL.Path})
		}
	}))
	t.Cleanup(server.Close)
	cli, err := client.NewClientWithOpts(
		client.WithHost("tcp://"+strings.TrimPrefix(server.URL, "http://")),
		client.WithVersion("1.44"),
	)
	require.NoError(t, err)
	return cli
}

func writeJson(w http.ResponseWriter, status int, body interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}

func readyWorkerNodes(w http.ResponseWriter, r *http.Request) {
	writeJson(w, http.StatusOK, []swarm.Node{{
		Spec:   swarm.NodeSpec{Availability: swarm.NodeAvailabilityActive},
		Status: swarm.NodeStatus{State: swarm.NodeStateReady, Addr: "10.0.0.1"},
	}})
}

func failWith(status int, message string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		writeJson(w, status, types.ErrorResponse{Message: message})
	}
}

func startContainer(t *testing.T, cli *client.Client, ports *queue.Queue, request *pb.ContainerRequest) *pb.ContainerResponse {
	previousConfig := c
	c = config{}
	t.Cleanup(func() { c = previousConfig })

	in, out := make(chan *pb.ContainerRequest, 1), make(chan *pb.ContainerResponse, 1)
	createRbdImageIn, createRbdImageOut := make(chan CreateRbdImageWorkerRequest, 1), make(chan CreateRbdImageWorkerResponse, 1)
	defer close(in)
	go getOrCreateContainer(in, out, cli, ports, createRbdImageIn, createRbdImageOut, 0)

	in <- request
	select {
	case response := <-out:
		return response
	case <-time.After(10 * time.Second):
		t.Fatal("no response for the container request")
		return nil
	}
}

func availablePorts(t *testing.T) *queue.Queue {
	ports := queue.New(1)
	require.NoError(t, ports.Put(uint32(10000)))
	return ports
}

func containerRequest(image string) *pb.ContainerRequest {
	return &pb.ContainerRequest{
		UserID:   "user",
		CourseID: "course",
		ImageID:  image,
		Metadata: &pb.Metadata{},
		Options:  &pb.ContainerRequestOptions{},
		Ports:    []*pb.RequestPort{{PortToMap: 8888}},
	}
}

func assertStartError(t *testing.T, response *pb.ContainerResponse, detail string) {
	assert.Equal(t, "user", response.GetUserID())
	assert.Equal(t, "course", response.GetCourseID())
	assert.Equal(t, CONTAINER_START_ERROR, response.GetError())
	assert.Contains(t, response.GetErrorParams()[CONTAINER_START_ERROR_DETAIL_KEY], detail)
}

func TestGetOrCreateContainerInvalidImage(t *testing.T) {
	cli := fakeDaemon(t, readyWorkerNodes, failWith(http.StatusBadRequest, "invalid reference format"))

	response := startContainer(t, cli, availablePorts(t), containerRequest("Not A Valid:Image"))

	assertStartError(t, response, "invalid reference format")
	assert.Equal(t, "Not A Valid:Image", response.GetImageID())
}

func TestGetOrCreateContainerServiceCreateFailure(t *testing.T) {
	cli := fakeDaemon(t, readyWorkerNodes, failWith(http.StatusInternalServerError, "network caddy not found"))

	response := startContainer(t, cli, availablePorts(t), containerRequest("alpine:3.22.2"))

	assertStartError(t, response, "network caddy not found")
}

func TestGetOrCreateContainerNodeListFailure(t *testing.T) {
	cli := fakeDaemon(t, failWith(http.StatusServiceUnavailable, "this node is not a swarm manager"), failWith(http.StatusInternalServerError, "not reached"))

	response := startContainer(t, cli, availablePorts(t), containerRequest("alpine:3.22.2"))

	assertStartError(t, response, "this node is not a swarm manager")
}

func TestGetOrCreateContainerNoPortAvailable(t *testing.T) {
	cli := fakeDaemon(t, readyWorkerNodes, failWith(http.StatusInternalServerError, "not reached"))
	ports := queue.New(1)
	ports.Dispose()

	response := startContainer(t, cli, ports, containerRequest("alpine:3.22.2"))

	assertStartError(t, response, queue.ErrDisposed.Error())
}

func TestGetOrCreateContainerVisibleErrorUnchanged(t *testing.T) {
	cli := fakeDaemon(t, readyWorkerNodes, failWith(http.StatusInternalServerError, "not reached"))
	request := containerRequest("alpine:3.22.2")
	request.Options = &pb.ContainerRequestOptions{UseStudentVolume: true, StorageBackend: pb.StorageBackend_LOCAL}

	response := startContainer(t, cli, availablePorts(t), request)

	assert.Equal(t, "student-volume.local-storage", response.GetError())
	assert.Empty(t, response.GetErrorParams())
}

func TestGetOrCreateContainerSuccessHasNoError(t *testing.T) {
	cli := fakeDaemon(t, readyWorkerNodes, func(w http.ResponseWriter, r *http.Request) {
		writeJson(w, http.StatusCreated, types.ServiceCreateResponse{ID: "service-id"})
	})

	response := startContainer(t, cli, availablePorts(t), containerRequest("alpine:3.22.2"))

	assert.Empty(t, response.GetError())
	assert.Nil(t, response.GetErrorParams())
	assert.Equal(t, "10.0.0.1", response.GetIpAddress())
	assert.Equal(t, uint32(10000), response.GetPorts()[0].GetMapTo())
}
