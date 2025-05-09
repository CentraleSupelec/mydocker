package main

import (
	"context"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
	"strconv"
	"time"
)

func addCleaningCron(s *gocron.Scheduler, dockerClient *client.Client) {
	_, _ = s.Every(5).Minute().Do(
		func() {
			findServiceToDeleteAndDoIt(dockerClient)
			findAndDeleteCompletedTasks(dockerClient)
		})
}

func findServiceToDeleteAndDoIt(dockerClient *client.Client) {
	nowSec := time.Now().Unix()
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("label", "deleteAfter=true")
	services, err := dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{Filters: filtersArgs})
	if err != nil {
		log.Errorf("Failed to get service to delete: %s", err)
	}
	for _, service := range services {
		deletionDateStr, exist := service.Spec.Labels["deletionTime"]
		if !exist {
			log.Errorf("Failed to find deletion time for service %s", service.ID)
		}
		deletionDate, err := strconv.Atoi(deletionDateStr)
		if err != nil {
			log.Errorf("Failed to parse deletion time for service %s, deletion time: %s", service.ID, deletionDateStr)
		}

		if int64(deletionDate) < nowSec {
			log.Infof("Delete service %s", service.ID)
			err = dockerClient.ServiceRemove(context.TODO(), service.ID)
			if err != nil {
				log.Errorf("Failed to delete service %s in cron job", service.ID)
			}
		}
	}
}

func findAndDeleteCompletedTasks(dockerClient *client.Client) {
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("desired-state", string(swarm.TaskStateShutdown))
	tasks, err := dockerClient.TaskList(context.TODO(), types.TaskListOptions{Filters: filtersArgs})
	if err != nil {
		log.Errorf("Failed to get tasks to delete: %s", err)
	}
	for _, task := range tasks {
		if task.Status.State != swarm.TaskStateComplete {
			continue
		}
		log.Infof("Delete service %s", task.ServiceID)
		err = dockerClient.ServiceRemove(context.TODO(), task.ServiceID)
		if err != nil {
			log.Errorf("Failed to delete service %s in cron job", task.ServiceID)
		}
	}
}
