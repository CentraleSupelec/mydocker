package main

import (
	"archive/zip"
	"bytes"
	"context"
	"fmt"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	containerTypes "github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/mount"
	networkTypes "github.com/docker/docker/api/types/network"
	"github.com/docker/docker/api/types/swarm"
	volumeTypes "github.com/docker/docker/api/types/volume"
	specs "github.com/opencontainers/image-spec/specs-go/v1"
	log "github.com/sirupsen/logrus"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"
	"time"
)

func buildDockerImageWorker(in chan *pb.DockerImageRequest, out chan *pb.DockerImageResponse, dockerClient dockerImageBuilderDockerClient) {
	dockerImageBuilder := &DockerImageBuilder{dockerClient: dockerClient, dockerUtils: NewDockerUtils(dockerClient)}
	for request := range in {
		volumeName, err := dockerImageBuilder.prepareBuildFiles(request.BuildId, request.ContextZip, request.Dockerfile, request.WrapperScript)
		if err != nil {
			log.Errorf("Got an error while preparing file for image %s, error %s", request.GetName(), err)
			response := &pb.DockerImageResponse{
				BuildId: request.BuildId,
				Status:  pb.DockerImageResponse_ERROR,
				Error:   fmt.Sprintf("Got an error while preparing file for image %s, error %s", request.GetName(), err),
			}
			out <- response
			continue
		}
		imageName := c.BuildRegistryAddress + "/" + c.BuildImageRepository + "/" + request.GetName() + ":" + request.GetBuildId()
		err = dockerImageBuilder.doDockerImageBuild(volumeName, imageName, request.BuildId, out)
		if err != nil {
			response := &pb.DockerImageResponse{
				BuildId: request.BuildId,
				Status:  pb.DockerImageResponse_ERROR,
				Error:   fmt.Sprintf("Got an error while building image, error: %s", err),
			}
			out <- response
			continue
		}
		response := &pb.DockerImageResponse{
			BuildId: request.BuildId,
			Status:  pb.DockerImageResponse_DONE,
			Name:    imageName,
		}
		out <- response
	}
}

type dockerImageBuilderDockerClient interface {
	dockerUtilsDockerClient
	ServiceCreate(ctx context.Context, service swarm.ServiceSpec, options types.ServiceCreateOptions) (types.ServiceCreateResponse, error)
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
	ServiceRemove(ctx context.Context, serviceID string) error
	VolumeRemove(ctx context.Context, volumeID string, force bool) error
	VolumeCreate(ctx context.Context, options volumeTypes.VolumeCreateBody) (types.Volume, error)
	ContainerStart(ctx context.Context, container string, options types.ContainerStartOptions) error
	ContainerRemove(ctx context.Context, container string, options types.ContainerRemoveOptions) error
	ContainerCreate(ctx context.Context, config *containerTypes.Config, hostConfig *containerTypes.HostConfig, networkingConfig *networkTypes.NetworkingConfig, platform *specs.Platform, containerName string) (containerTypes.ContainerCreateCreatedBody, error)

	TaskLogs(ctx context.Context, taskID string, options types.ContainerLogsOptions) (io.ReadCloser, error)
}

type DockerImageBuilder struct {
	dockerClient dockerImageBuilderDockerClient
	dockerUtils  *DockerUtils
}

func (d *DockerImageBuilder) doDockerImageBuild(volumeName string, imageName string, buildId string, out chan *pb.DockerImageResponse) error {
	if volumeName == "" {
		log.Errorf("Volume name is empty for image %s", imageName)
		return fmt.Errorf("volume name is empty for image %s", imageName)
	}
	ctx := context.Background()
	one := uint64(1)
	spec := swarm.ServiceSpec{
		Mode: swarm.ServiceMode{
			ReplicatedJob: &swarm.ReplicatedJob{},
		},
		TaskTemplate: swarm.TaskSpec{
			RestartPolicy: &swarm.RestartPolicy{
				Condition:   "on-failure",
				MaxAttempts: &one,
			},
			Placement: &swarm.Placement{
				Constraints: []string{fmt.Sprintf("node.labels.volume_backend==rbd")},
			},
			ContainerSpec: &swarm.ContainerSpec{
				Image: c.BuildImage,
				Args: []string{
					"--dockerfile=Dockerfile", "--context=dir:///context", "--destination=" + imageName, "--push-retry", "5", "--log-format", "text", "--cache=true",
				},
				Mounts: []mount.Mount{
					{
						Type:     mount.TypeVolume,
						Source:   volumeName,
						Target:   "/context",
						ReadOnly: true,
						VolumeOptions: &mount.VolumeOptions{
							DriverConfig: &mount.Driver{
								Name: "centralesupelec/mydockervolume",
							},
						},
					},
				},
			},
		},
	}
	service, err := d.dockerClient.ServiceCreate(ctx, spec, buildServiceCreateOptions(c.BuildImage))
	if err != nil {
		return err
	}
	log.Debugf("Created service %s", service.ID)
	completed, taskError := false, ""
	for completed == false && taskError == "" {
		tasks, err := d.dockerClient.TaskList(ctx, types.TaskListOptions{Filters: filters.NewArgs(filters.Arg("service", service.ID))})
		if err != nil {
			return err
		}
		if len(tasks) >= 1 {
			task := tasks[len(tasks)-1]
			log.Debugf("State for %s : %s", service.ID, task.Status.State)
			completed = task.Status.State == swarm.TaskStateComplete
			taskError = task.Status.Err
			time.Sleep(5 * time.Second)
			reader, err := d.dockerClient.TaskLogs(ctx, task.ID, types.ContainerLogsOptions{ShowStdout: true, ShowStderr: true, Follow: true})
			if err != nil {
				log.Errorf("failed to read logs for task %s while building an image", task.ID)
				continue
			}
			logs, err := readLogs(reader)
			if err != nil {
				log.Errorf("failed to convert logs for task %s while building an image", task.ID)
				continue
			}
			_ = reader.Close()
			response := &pb.DockerImageResponse{
				BuildId: buildId,
				Status:  pb.DockerImageResponse_BUILDING,
				Logs:    logs,
			}
			out <- response
		}
	}

	err = d.dockerClient.ServiceRemove(ctx, service.ID)
	if err != nil {
		log.Errorf("failed to remove service %s after building an image", service.ID)
	}
	err = d.dockerClient.VolumeRemove(ctx, volumeName, true)
	if err != nil {
		log.Errorf("failed to remove volume %s after building an image", volumeName)
	}
	if taskError != "" {
		return fmt.Errorf("failed to build image: %s", taskError)
	} else {
		log.Debugf("build image %s successfully", imageName)
	}

	return nil
}

func (d *DockerImageBuilder) prepareBuildFiles(buildId string, contextZip []byte, dockerfile string, wrapperScript string) (string, error) {
	buildFilesPath, err := ioutil.TempDir("", "build-"+buildId)

	// Unzip context folder
	if contextZip != nil && len(contextZip) > 0 {
		zipReader, err := zip.NewReader(bytes.NewReader(contextZip), int64(len(contextZip)))
		if err != nil {
			return "", err
		}
		_, err = Unzip(zipReader, buildFilesPath)
		if err != nil {
			return "", err
		}
	}

	err = writeFile(buildFilesPath, "Dockerfile", []byte(dockerfile))
	if err != nil {
		return "", err
	}

	err = writeFile(buildFilesPath, "wrapper_script.sh", []byte(wrapperScript))
	if err != nil {
		return "", err
	}

	volumeName := "volume_build_" + buildId
	s := strconv.Itoa(5 * 1024)
	driverOpts := map[string]string{"size": s}

	options := volumeTypes.VolumeCreateBody{
		Driver:     "centralesupelec/mydockervolume",
		DriverOpts: driverOpts,
		Name:       volumeName,
	}

	_, err = d.dockerClient.VolumeCreate(context.Background(), options)
	if err != nil {
		return "", err
	}

	contConfig := &containerTypes.Config{
		Image: SAVE_IMAGE,
		Cmd:   []string{"rsync", "-a", "--delete", "/tmp/source/", "/tmp/dest"},
	}

	hostConfig := &containerTypes.HostConfig{
		Mounts: []mount.Mount{
			{
				Type:     mount.TypeBind,
				Source:   buildFilesPath,
				Target:   "/tmp/source",
				ReadOnly: true,
			},
			{
				Type:   mount.TypeVolume,
				Source: volumeName,
				Target: "/tmp/dest",
				VolumeOptions: &mount.VolumeOptions{
					DriverConfig: &mount.Driver{
						Name: "centralesupelec/mydockervolume",
					},
				},
			},
		},
	}

	err = d.dockerUtils.pullImage(SAVE_IMAGE, false)
	if err != nil {
		return "", err
	}

	copyContainer, err := d.dockerClient.ContainerCreate(context.TODO(), contConfig, hostConfig, nil, nil, "copy_volume_"+buildId)
	if err != nil {
		return "", err
	}

	err = d.dockerClient.ContainerStart(context.TODO(), copyContainer.ID, types.ContainerStartOptions{})
	if err != nil {
		return "", err
	}

	err = d.dockerUtils.waitForContainer(copyContainer.ID)
	if err != nil {
		return "", err
	}

	err = d.dockerClient.ContainerRemove(
		context.TODO(),
		copyContainer.ID,
		types.ContainerRemoveOptions{
			RemoveVolumes: false,
			RemoveLinks:   false,
		},
	)
	if err != nil {
		return "", err
	}
	err = os.RemoveAll(buildFilesPath)
	if err != nil {
		return "", err
	}
	return volumeName, err
}

func writeFile(dname string, filename string, data []byte) error {
	fullPath := filepath.Join(dname, filename)
	err := ioutil.WriteFile(fullPath, data, 0666)
	return err
}
