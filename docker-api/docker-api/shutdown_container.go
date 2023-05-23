package main

import (
	"context"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/client"
	log "github.com/sirupsen/logrus"
)

func shutdownContainer(
	in <-chan *pb.ShutdownContainerRequest,
	out chan<- *pb.ShutdownContainerResponse,
	dockerClient *client.Client,
) {
	for request := range in {
		serviceName := createContainerName(request.GetUserID(), request.GetCourseID())
		log.Debugf("Shutdowning service %s", serviceName)
		err := dockerClient.ServiceRemove(context.TODO(), serviceName)
		response := &pb.ShutdownContainerResponse{
			UserID:   request.UserID,
			CourseID: request.CourseID,
		}
		if err != nil {
			log.Errorf("Error shutdowning service %s : %s", serviceName, err)
			response.Error = err.Error()
		} else {
			log.Debugf("Successfully shutdowned service %s", serviceName)
		}
		out <- response
	}
}
