package main

import (
	"context"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
)

func (s *server) GetLogs(ctx context.Context, request *pb.LogRequest) (*pb.LogResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)
	service, err := dockerUtils.findService(ctx, request.GetUserID(), request.GetCourseID())
	if err != nil {
		return nil, err
	}
	reader, err := s.dockerClient.ServiceLogs(ctx, service.ID, types.ContainerLogsOptions{ShowStdout: true, ShowStderr: true, Follow: true})
	if err != nil {
		return nil, err
	}
	logs, err := readLogs(reader)
	if err != nil {
		return nil, err
	}
	return &pb.LogResponse{Logs: logs}, nil
}
