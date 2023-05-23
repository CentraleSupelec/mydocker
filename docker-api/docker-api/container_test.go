package main

import (
	"context"
	log "github.com/sirupsen/logrus"
	"testing"

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
	go portsWorker(ports, cli)

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
	here, id, _, _, _, _, err := exist("test-test", responsePorts, cli)
	assert.Nil(t, err)
	assert.True(t, here)

	err = deleteService(id, cli)
	assert.Nil(t, err)

	here, id, _, _, _, _, err = exist("test-test", responsePorts, cli)
	assert.False(t, here)
	assert.Nil(t, err)
}
