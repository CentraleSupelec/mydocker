package main

import (
	"context"
	"time"

	"github.com/Workiva/go-datastructures/queue"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/client"
	log "github.com/sirupsen/logrus"
)

func portsWorker(ctx context.Context, portsAvailable *queue.Queue, dockerClient *client.Client) {
	interval, err := time.ParseDuration(c.PortWorkerInterval)
	if err != nil {
		log.WithError(err).Error("Invalid PortWorkerInterval in config")
		return
	}

	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	lastPort := c.PortMin
	var lastQueueLength int64

	log.WithFields(log.Fields{
		"portMin": c.PortMin,
		"portMax": c.PortMax,
		"interval": interval,
	}).Info("Starting ports worker")

	for {
		select {
		case <-ctx.Done():
			log.Info("Ports worker shutting down")
			return
		case <-ticker.C:
			currentLength := portsAvailable.Len()

			// Only process if queue length has changed or is below threshold
			if currentLength != lastQueueLength || currentLength < c.PortThreshold {
				if err := refillPortsQueue(ctx, portsAvailable, dockerClient, &lastPort); err != nil {
					log.WithError(err).Error("Failed to refill ports queue")
				}
				lastQueueLength = portsAvailable.Len()
			}

			if lastPort == c.PortMax+1 {
				lastPort = c.PortMin
			}
		}
	}
}

func refillPortsQueue(ctx context.Context, portsAvailable *queue.Queue, dockerClient *client.Client, lastPort *uint32) error {
	if portsAvailable.Len() >= c.PortThreshold {
		return nil
	}

	services, err := dockerClient.ServiceList(ctx, types.ServiceListOptions{})
	if err != nil {
		return err
	}

	// Create a map of used ports for O(1) lookup
	portUsed := make(map[uint32]bool, len(services))
	for _, service := range services {
		for _, port := range service.Endpoint.Ports {
			portUsed[port.PublishedPort] = true
		}
	}

	// Batch port additions to minimize lock contention
	portsToAdd := make([]interface{}, 0, c.PortSize-portsAvailable.Len())

	for portsAvailable.Len() < c.PortSize && *lastPort <= c.PortMax {
		if _, exist := portUsed[*lastPort]; !exist {
			portsToAdd = append(portsToAdd, *lastPort)
		}
		*lastPort++
	}

	if len(portsToAdd) > 0 {
		if err := portsAvailable.Put(portsToAdd...); err != nil {
			log.WithError(err).Error("Failed to add ports to queue")
			return err
		}

		log.WithFields(log.Fields{
			"addedPorts": len(portsToAdd),
			"queueLength": portsAvailable.Len(),
		}).Debug("Added ports to queue")
	}

	return nil
}
