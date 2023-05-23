package main

import (
	"context"
	"testing"

	"github.com/Workiva/go-datastructures/queue"
	"github.com/docker/docker/client"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestPortsWorker(t *testing.T) {
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
	if err != nil {
		log.Panic(err)
	}
	ports := queue.New(c.PortSize)
	go portsWorker(ports, cli)

	for i := c.PortMin; i <= c.PortMax; i++ {
		portsArray, _ := ports.Get(1)
		port := portsArray[0].(uint32)
		assert.Equal(t, i, port)
	}
}
