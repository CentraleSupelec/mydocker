package main

import (
	"context"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
)

func (s *server) GetCourseInfra(ctx context.Context, request *pb.CourseInfraRequest) (*pb.CourseInfraResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)
	nodeWithCourseIdLabelExists, err := dockerUtils.nodeWithCourseIdLabelExists(ctx, request.GetCourseID())
	if err != nil {
		return nil, err
	}
	return &pb.CourseInfraResponse{NodeWithCourseIdLabelExists: nodeWithCourseIdLabelExists}, nil
}
