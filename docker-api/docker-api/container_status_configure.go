package main

import (
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	log "github.com/sirupsen/logrus"
)

func configureContainerStatus(in <-chan *pb.ContainerStatusRequest, containersToWatch map[string]bool) {
	logger := log.WithFields(log.Fields{"service": "containerStatusConfigure"})
	for request := range in {
		logger.WithFields(
			log.Fields{
				"user_id":   request.GetUserID(),
				"course_id": request.GetCourseID(),
				"is_admin":  request.GetIsAdmin(),
				"action":    request.GetAction(),
			}).Debug("Received container status request")
		var containerName string
		if request.GetIsAdmin() {
			containerName = createAdminContainerName(request.GetCourseID())
		} else {
			containerName = createContainerName(request.GetUserID(), request.GetCourseID())
		}
		if request.GetAction() == pb.ContainerStatusRequest_on {
			containersToWatch[containerName] = true
			logger.WithFields(
				log.Fields{
					"container": containerName,
					"is_admin":  request.GetIsAdmin(),
				}).Info("Started watching container")
		} else if request.GetAction() == pb.ContainerStatusRequest_off {
			delete(containersToWatch, containerName)
			logger.WithFields(
				log.Fields{
					"container": containerName,
					"is_admin":  request.GetIsAdmin(),
				}).Info("Stopped watching container")
		}
	}
}
