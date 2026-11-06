package main

import (
	"testing"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/stretchr/testify/assert"
)

func TestContainerEnv(t *testing.T) {
	response := &pb.ContainerResponse{
		AuthenticationMethod: &pb.ContainerResponse_UserPassword{
			UserPassword: &pb.UserPasswordMethod{Username: "alice", Password: "s3cret"},
		},
	}
	credentials := []string{"MYDOCKER_USERNAME=alice", "MYDOCKER_PASSWORD=s3cret"}
	slurmRequest := func(key, endpoint string) *pb.ContainerRequest {
		return &pb.ContainerRequest{Options: &pb.ContainerRequestOptions{
			SlurmApiContainerParams: &pb.SlurmApiContainerParams{Key: key, Endpoint: endpoint},
		}}
	}

	tests := []struct {
		name     string
		request  *pb.ContainerRequest
		expected []string
	}{
		{"no options", &pb.ContainerRequest{}, credentials},
		{"no slurm params", &pb.ContainerRequest{Options: &pb.ContainerRequestOptions{}}, credentials},
		{"key without endpoint", slurmRequest("k", ""), credentials},
		{"endpoint without key", slurmRequest("", "https://gw"), credentials},
		{"key and endpoint", slurmRequest("k", "https://gw"),
			append(append([]string{}, credentials...), "SLURM_API_KEY=k", "SLURM_API_ENDPOINT=https://gw")},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.expected, containerEnv(response, tt.request))
		})
	}
}

func TestRedactEnv(t *testing.T) {
	env := []string{
		"MYDOCKER_USERNAME=alice",
		"MYDOCKER_PASSWORD=s3cret",
		"SLURM_API_KEY=k",
		"SLURM_API_ENDPOINT=https://gw",
	}
	assert.Equal(t, []string{
		"MYDOCKER_USERNAME=alice",
		"MYDOCKER_PASSWORD=***REDACTED***",
		"SLURM_API_KEY=***REDACTED***",
		"SLURM_API_ENDPOINT=https://gw",
	}, redactEnv(env))
	assert.Equal(t, "MYDOCKER_PASSWORD=s3cret", env[1], "redactEnv must not mutate its input")
}
