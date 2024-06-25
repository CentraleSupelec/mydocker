package main

import (
	"context"
	"github.com/docker/docker/api/types"
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
		})
}

func findServiceToDeleteAndDoIt(dockerClient *client.Client) {
	nowSec := time.Now().Unix()
	services, err := dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{Status: true})
	if err != nil {
		log.Errorf("Failed to get service to delete: %s", err)
	}
	for _, service := range services {
		if deleteAfter, exist := service.Spec.Labels["deleteAfter"]; !exist || (deleteAfter != "true") {
			if service.ServiceStatus.DesiredTasks != service.ServiceStatus.RunningTasks {
				log.Infof("Delete service %s with completed tasks", service.ID)
				err = dockerClient.ServiceRemove(context.TODO(), service.ID)
				if err != nil {
					log.Errorf("Failed to delete service %s in cron job", service.ID)
				}
			}
			continue
		}
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
