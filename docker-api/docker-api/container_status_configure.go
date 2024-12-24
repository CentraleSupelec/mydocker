package main

import (
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	log "github.com/sirupsen/logrus"
)

func configureContainerStatus(in <-chan *pb.ContainerStatusRequest, containersToWatch map[string]bool) {
	logger := log.WithFields(log.Fields{"service": "containerStatusConfigure"})
	for request := range in {
		logger.Debug("Received container status request",
			"user_id", request.GetUserID(),
			"course_id", request.GetCourseID(),
			"is_admin", request.GetIsAdmin(),
			"action", request.GetAction())
		var containerName string
		if request.GetIsAdmin() {
			containerName = createAdminContainerName(request.GetCourseID())
		} else {
			containerName = createContainerName(request.GetUserID(), request.GetCourseID())
		}
		if request.GetAction() == pb.ContainerStatusRequest_on {
			containersToWatch[containerName] = true
			logger.Info("Started watching container",
				"container", containerName,
				"is_admin", request.GetIsAdmin())
		} else if request.GetAction() == pb.ContainerStatusRequest_off {
			delete(containersToWatch, containerName)
			logger.Info("Stopped watching container",
				"container", containerName,
				"is_admin", request.GetIsAdmin())
		}
	}
}
