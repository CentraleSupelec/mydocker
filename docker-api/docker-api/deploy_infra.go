package main

import (
	"context"
	"fmt"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	log "github.com/sirupsen/logrus"
)

func (s *server) DeployInfra(request *pb.DeployRequest, responseStream pb.ContainerService_DeployInfraServer) error {
	// Build TF_VARIABLES
	jobId := RandomString(6)
	log.Tracef("Start deploy infra #%s for %s", jobId, request.GetId())
	defer log.Tracef("End deploy infra #%s", jobId)
	terraformConfig := NewTerraformConfig("")

	for _, w := range request.Workers {
		_, exist := terraformConfig.NbWorkerByRegion[w.Region]
		if !exist {
			terraformConfig.NbWorkerByRegion[w.Region] = make(map[string]map[string]TerraformWorkerConfig)
		}
		flavor, exist := terraformConfig.NbWorkerByRegion[w.Region][w.Flavor]
		if !exist {
			terraformConfig.NbWorkerByRegion[w.Region][w.Flavor] = make(map[string]TerraformWorkerConfig)
			flavor = terraformConfig.NbWorkerByRegion[w.Region][w.Flavor]
		}
		ownerWorkers, exist := flavor[w.Owner]
		if exist {
			flavor[w.Owner] = TerraformWorkerConfig{
				Count:           ownerWorkers.Count + w.Count,
				InstanceImageId: w.ImageId,
			}
		} else {
			flavor[w.Owner] = TerraformWorkerConfig{
				Count:           w.Count,
				InstanceImageId: w.ImageId,
			}
		}
	}

	// Clean services

	for _, courseId := range request.CourseIds {
		log.Debugf("Cleaning services for course %s", courseId)
		services, err := s.dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{
			Filters: filters.NewArgs(filters.KeyValuePair{
				Key: "label", Value: fmt.Sprintf("courseId=%s", courseId),
			}),
		})
		if err != nil {
			log.Errorf("Failed to list service for courseId %s before cleaning; %s", courseId, err)
		}
		for _, service := range services {
			log.Debugf("Removing service %s", service.ID)
			err = s.dockerClient.ServiceRemove(context.TODO(), service.ID)
			if err != nil {
				log.Errorf("Failed to remove service %s service before cleaning; %s", service.ID, err)
			}
		}
	}
	callback := createSessionScalingCallback(request.Id, responseStream)
	deployUtils := NewDeployUtils(s.dockerClient)
	log.Debugf("Deploying with Terraform config : %v", terraformConfig)
	launchErr := deployUtils.launchTerraform(terraformConfig, callback, true)
	if launchErr != nil {
		var err error
		if _, ok := launchErr.(*SkippedError); ok {
			err = callback(&DeployStatus{
				Status: pb.DeployResponse_SKIPPED,
				Error:  launchErr.Error(),
			})
		} else {
			err = callback(&DeployStatus{
				Error: launchErr.Error(),
			})
		}
		return err
	}
	return nil
}

func createSessionScalingCallback(requestId string, responseStream pb.ContainerService_DeployInfraServer) deployCallback {
	return func(status *DeployStatus) error {
		return sendData(newDeployResponse(status, requestId), responseStream)
	}
}

func sendData(response *pb.DeployResponse, responseStream pb.ContainerService_DeployInfraServer) error {
	err := responseStream.Send(response)

	if err != nil {
		log.Errorf("Got an error while responding to deploy infra #%s: %s", response.Id, err)
	}
	return err
}
