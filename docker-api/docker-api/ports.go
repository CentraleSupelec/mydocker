package main

import (
	"context"

	"github.com/Workiva/go-datastructures/queue"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/client"
	log "github.com/sirupsen/logrus"
)

func portsWorker(portsAvailable *queue.Queue, dockerClient *client.Client) {
	lastPort := c.PortMin
	for {
		if portsAvailable.Len() < c.PortThreshold {
			services, err := dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{})
			if err != nil {
				log.Error(err)
			}
			portUsed := make(map[uint32]bool)
			for _, service := range services {
				for _, port := range service.Endpoint.Ports {
					portUsed[port.PublishedPort] = true
				}
			}
			for portsAvailable.Len() < c.PortSize && lastPort <= c.PortMax {
				if _, exist := portUsed[lastPort]; !exist {
					_ = portsAvailable.Put(lastPort)
				}
				lastPort++
			}
		}
		if lastPort == c.PortMax+1 {
			lastPort = c.PortMin
		}
	}
}
