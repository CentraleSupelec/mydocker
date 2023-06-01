package main

import (
	"context"
	"encoding/json"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	containerTypes "github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	networkTypes "github.com/docker/docker/api/types/network"
	specs "github.com/opencontainers/image-spec/specs-go/v1"
	log "github.com/sirupsen/logrus"
	"io"
	"time"
)

type TerraformWorkerConfig struct {
	Count           uint32 `json:"count"`
	InstanceImageId string `json:"instance_image_id"`
}

type TerraformConfig struct {
	InstancePrefix   string                                                 `json:"instance_prefix"`
	CephServiceName  string                                                 `json:"ceph_service_name"`
	Env              string                                                 `json:"env"`
	NbWorkerByRegion map[string]map[string]map[string]TerraformWorkerConfig `json:"nb_worker_by_region"`
	StateName        string                                                 `json:"-"`
	NamedWorkers     map[string]TerraformNamedWorker                        `json:"named_workers"`
}

func NewTerraformConfig(stateName string) *TerraformConfig {
	t := &TerraformConfig{
		InstancePrefix:   "tf-auto-deploy",
		CephServiceName:  c.CephServiceName,
		Env:              c.Environment,
		NbWorkerByRegion: make(map[string]map[string]map[string]TerraformWorkerConfig),
		NamedWorkers:     make(map[string]TerraformNamedWorker),
		StateName:        "tfstate.tf",
	}
	if stateName != "" {
		t.StateName = stateName
	}
	return t
}

type TerraformNamedWorker struct {
	InstanceImageId string `json:"instance_image_id"`
	InstanceType    string `json:"instance_type"`
	Name            string `json:"name"`
	Region          string `json:"region"`
	Owner           string `json:"owner"`
}

const DeployContainerName string = "deploy_container"

type DeployStatus struct {
	Error  string
	Status pb.DeployResponse_Status
	Logs   string
}

func newDeployResponse(d *DeployStatus, requestId string) *pb.DeployResponse {
	return &pb.DeployResponse{
		Id:     requestId,
		Status: d.Status,
		Error:  d.Error,
		Logs:   d.Logs,
	}
}

type deployCallback func(status *DeployStatus) error

var lock = make(chan struct{}, 1)

type SkippedError struct {
	Message string
}

func (e *SkippedError) Error() string {
	return e.Message
}

type deployUtilsDockerClient interface {
	dockerUtilsDockerClient
	ContainerCreate(ctx context.Context, config *containerTypes.Config, hostConfig *containerTypes.HostConfig, networkingConfig *networkTypes.NetworkingConfig, platform *specs.Platform, containerName string) (containerTypes.ContainerCreateCreatedBody, error)
	ContainerInspect(ctx context.Context, container string) (types.ContainerJSON, error)
	ContainerList(ctx context.Context, options types.ContainerListOptions) ([]types.Container, error)
	ContainerLogs(ctx context.Context, container string, options types.ContainerLogsOptions) (io.ReadCloser, error)
	ContainerRemove(ctx context.Context, container string, options types.ContainerRemoveOptions) error
	ContainerStart(ctx context.Context, container string, options types.ContainerStartOptions) error
	ContainerWait(ctx context.Context, container string, condition containerTypes.WaitCondition) (<-chan containerTypes.ContainerWaitOKBody, <-chan error)
}

type DeployUtils struct {
	dockerClient deployUtilsDockerClient
	dockerUtils  *DockerUtils
}

type DeployUtilsInterface interface {
	launchTerraform(terraformConfig *TerraformConfig, callback deployCallback, removeContainerOnError bool) error
	retrieveLogsAndSend(deployContainerId string, response *DeployStatus, callback deployCallback) error
}

func NewDeployUtils(dockerClient deployUtilsDockerClient) *DeployUtils {
	return &DeployUtils{
		dockerClient: dockerClient,
		dockerUtils:  NewDockerUtils(dockerClient),
	}
}

func (d *DeployUtils) launchTerraform(terraformConfig *TerraformConfig, callback deployCallback, removeContainerOnError bool) error {
	var err error
	err = d.dockerUtils.pullImage(c.DeployImage, true)
	if err != nil {
		return err
	}
	select {
	case lock <- struct{}{}:
		defer func() { <-lock }()
	default:
		err := &SkippedError{Message: "Locked, waiting before deploying"}
		log.Debugf(err.Error())
		return err
	}
	jsonConfig, err := json.Marshal(terraformConfig)
	if err != nil {
		return err
	}

	// Run deploy container
	log.Debugf("Deploying %s on %s with payload %s", c.Environment, c.DeployImage, string(jsonConfig))
	envVars := []string{
		"TF_VARIABLES=" + string(jsonConfig),
		"ENV=" + c.Environment,
		"STATE_NAME=" + terraformConfig.StateName,
	}
	for _, value := range c.DeployEnvSecrets {
		envVars = append(envVars, value)
	}
	contConfig := &containerTypes.Config{
		Image: c.DeployImage,
		Env:   envVars,
	}

	filtersArgs := filters.NewArgs()
	filtersArgs.Add("name", DeployContainerName)

	containers, err := d.dockerClient.ContainerList(context.TODO(), types.ContainerListOptions{Filters: filtersArgs, All: true})
	if err != nil {
		return err
	}
	if len(containers) > 0 {
		if containers[0].State == "exited" {
			err = d.dockerClient.ContainerRemove(
				context.TODO(),
				containers[0].ID,
				types.ContainerRemoveOptions{
					RemoveVolumes: false,
					RemoveLinks:   false,
				},
			)
			if err != nil {
				log.Errorf("Failed to clean before launch %s", err)
				return err
			}
		} else {
			err := &SkippedError{Message: "A deploy container is already running"}
			log.Debugf(err.Error())
			return err
		}
	}

	deployContainer, err := d.dockerClient.ContainerCreate(context.TODO(), contConfig, nil, nil, nil, DeployContainerName)
	if err != nil {
		return err
	}

	err = d.dockerClient.ContainerStart(context.TODO(), deployContainer.ID, types.ContainerStartOptions{})
	if err != nil {
		return err
	}

	response := &DeployStatus{
		Status: pb.DeployResponse_RUNNING,
	}
	err = callback(response)
	if err != nil {
		return err
	}
	// Wait for completion and return logs
	statusCh, errCh := d.dockerClient.ContainerWait(context.TODO(), deployContainer.ID, containerTypes.WaitConditionNotRunning)
	ticker := time.NewTicker(5 * time.Second)
	running := true
	for running {
		select {
		case err := <-errCh:
			if err != nil {
				response.Error = err.Error()
				response.Status = pb.DeployResponse_ERROR
				errSending := callback(response)
				if errSending != nil {
					return errSending
				}
				running = false
			}
		case <-statusCh:
			ticker.Stop()
			running = false
		case <-ticker.C:
			errorLogs := d.retrieveLogsAndSend(
				deployContainer.ID,
				response,
				callback)
			if errorLogs != nil {
				return errorLogs
			}
		}
	}

	if response.Status == pb.DeployResponse_RUNNING {
		response.Status = pb.DeployResponse_DONE
	}

	c, err := d.dockerClient.ContainerInspect(context.TODO(), deployContainer.ID)

	if err == nil {
		if c.State.ExitCode != 0 {
			response.Status = pb.DeployResponse_ERROR
			response.Error = c.State.Error
		}
	}

	err = d.retrieveLogsAndSend(
		deployContainer.ID,
		response,
		callback)
	if err != nil {
		return err
	}

	if response.Status != pb.DeployResponse_ERROR || removeContainerOnError {
		err = d.dockerClient.ContainerRemove(
			context.TODO(),
			deployContainer.ID,
			types.ContainerRemoveOptions{
				RemoveVolumes: false,
				RemoveLinks:   false,
			},
		)
	}

	return err
}

func (d *DeployUtils) retrieveLogsAndSend(
	deployContainerId string,
	response *DeployStatus,
	callback deployCallback) error {

	reader, err := d.dockerClient.ContainerLogs(
		context.TODO(), deployContainerId, types.ContainerLogsOptions{ShowStdout: true, ShowStderr: true})
	if err != nil {
		log.Error("failed to read logs for deploy container")
	} else {
		logs, err := readLogs(reader)
		if err != nil {
			log.Error("failed to convert logs for deploy container")
		} else {
			_ = reader.Close()
			response.Logs = logs
			errSending := callback(response)
			if errSending != nil {
				return errSending
			}
		}
	}
	return nil
}
