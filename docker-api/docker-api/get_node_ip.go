package main

import (
	"context"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
)

func (s *server) GetNodeIP(ctx context.Context, request *pb.NodeIPRequest) (*pb.NodeIPResponse, error) {
	dockerUtils := NewDockerUtils(s.dockerClient)
	nodeId, err := dockerUtils.getRunningTaskNodeId(ctx, request.GetUserID(), request.GetCourseID())
	if err != nil {
		return nil, err
	}
	node, _, err := s.dockerClient.NodeInspectWithRaw(ctx, nodeId)
	if err != nil {
		return nil, err
	}
	return &pb.NodeIPResponse{IPAddress: node.Status.Addr}, nil
}
