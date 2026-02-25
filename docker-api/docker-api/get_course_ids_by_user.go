package main

import (
	"context"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"google.golang.org/protobuf/types/known/emptypb"
)

func (s *server) GetCourseIdsByUser(ctx context.Context, _ *emptypb.Empty) (*pb.CourseIdsByUserIdMapResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)

	services, err := dockerUtils.getAllServices(ctx)
	if err != nil {
		return nil, err
	}

	courseIdsByUser := make(map[string][]string)

	for _, svc := range services {
		labels := svc.Spec.Annotations.Labels
		if labels == nil {
			continue
		}

		userId, userOk := labels["userId"]
		courseId, courseOk := labels["courseId"]

		if !userOk || !courseOk {
			continue
		}

		courseIdsByUser[userId] = append(courseIdsByUser[userId], courseId)
	}

	result := make(map[string]*pb.CourseIds)

	for userId, courseIds := range courseIdsByUser {
		result[userId] = &pb.CourseIds{
			CourseIds: courseIds,
		}
	}

	return &pb.CourseIdsByUserIdMapResponse{
		CourseIdsByUserIdMap: result,
	}, nil
}
