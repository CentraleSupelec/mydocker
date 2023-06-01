package main

import (
	"context"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"strconv"
)

func (s *server) DelayDeletion(ctx context.Context, request *pb.DelayDeletionRequest) (*pb.DelayDeletionResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)
	service, err := dockerUtils.findService(ctx, request.GetUserID(), request.GetCourseID())
	if err != nil {
		return nil, err
	}
	spec := service.Spec
	for key, value := range request.GetMetadata().GetTags() {
		spec.Labels[key] = value
	}
	_, err = s.dockerClient.ServiceUpdate(ctx, service.ID, service.Meta.Version, spec, types.ServiceUpdateOptions{})
	if err != nil {
		return nil, err
	}
	response := &pb.DelayDeletionResponse{
		UserID:   request.GetUserID(),
		CourseID: request.GetCourseID(),
	}
	deletionTime, err := strconv.ParseInt(request.GetMetadata().GetTags()["deletionTime"], 10, 64)
	if err == nil {
		response.DeletionTime = deletionTime
		return response, nil
	}
	deletionTime, err = strconv.ParseInt(service.Spec.Labels["deletionTime"], 10, 64)
	if err == nil {
		response.DeletionTime = deletionTime
		return response, nil
	}
	return response, nil
}
