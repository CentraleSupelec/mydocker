package main

import (
	"context"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	log "github.com/sirupsen/logrus"
	"sync"
)

var scaleUpConfig = ScaleUpConfig{ScaleUpOwners: make(map[string]scaleUpOwner)}
var autoscalingLock sync.Mutex

func (s *server) InitAutoscaling(ctx context.Context, request *pb.InitAutoscalingRequest) (*pb.InitAutoscalingResponse, error) {
	var err error

	scaleUpConfig.Lock.Lock()
	log.Debugf("Received autoscaling init config %v", request.GetOwners())
	for owner, _ := range scaleUpConfig.ScaleUpOwners {
		if _, exist := request.GetOwners()[owner]; !exist {
			delete(scaleUpConfig.ScaleUpOwners, owner)
		}
	}
	for owner, ownerConfig := range request.GetOwners() {
		regions := []ScalingRegion{}
		for _, region := range ownerConfig.GetRegions() {
			regions = append(regions, ScalingRegion{
				ImageId: region.GetImageId(),
				Region:  region.GetRegion(),
			})
		}
		scaleUpConfig.ScaleUpOwners[owner] = scaleUpOwner{
			InstanceType:      ownerConfig.GetInstanceType(),
			MinIdleNodesCount: ownerConfig.GetMinIdleNodesCount(),
			MaxNodesCount:     ownerConfig.GetMaxNodesCount(),
			ManualNodesCount:  ownerConfig.GetManualNodesCount(),
			Regions:           regions,
		}
	}
	scaleUpConfig.Lock.Unlock()
	err = addScaleUpCron(s.cronScheduler, s.dockerClient, &autoscalingLock, &scaleUpConfig)
	if err != nil {
		return &pb.InitAutoscalingResponse{Error: err.Error()}, nil
	}
	err = addScaleDownCron(s.cronScheduler, s.dockerClient, &autoscalingLock, &scaleUpConfig)
	if err != nil {
		return &pb.InitAutoscalingResponse{Error: err.Error()}, nil
	}
	return &pb.InitAutoscalingResponse{}, nil
}
