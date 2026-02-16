package main

import (
	"context"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"google.golang.org/protobuf/types/known/emptypb"
)

func (s *server) GetConnectedUsersByCourseIdMap(ctx context.Context, _ *emptypb.Empty) (*pb.ConnectedUsersByCourseIdMapResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)
	services, err := dockerUtils.getAllServices(ctx)
	if err != nil {
		return nil, err
	}

	connectedUsersByCourseIdMap := make(map[string]int32)

	for _, svc := range services {
		labels := svc.Spec.Annotations.Labels
		if labels == nil {
			continue
		}
		if val, ok := labels["courseId"]; ok {
			connectedUsersByCourseIdMap[val]++
		}
	}

	return &pb.ConnectedUsersByCourseIdMapResponse{
		ConnectedUsersByCourseIdMap: connectedUsersByCourseIdMap,
	}, nil
}
