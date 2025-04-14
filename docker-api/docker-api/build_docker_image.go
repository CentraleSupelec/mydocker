package main

import (
	"archive/tar"
	"archive/zip"
	"bytes"
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"
	"time"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	containerTypes "github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/mount"
	networkTypes "github.com/docker/docker/api/types/network"
	"github.com/docker/docker/api/types/swarm"
	volumeTypes "github.com/docker/docker/api/types/volume"
	specs "github.com/opencontainers/image-spec/specs-go/v1"
	log "github.com/sirupsen/logrus"
)

func buildDockerImageWorker(in chan *pb.DockerImageRequest, out chan *pb.DockerImageResponse, dockerClient dockerImageBuilderDockerClient) {
	dockerImageBuilder := newDockerImageBuilder(dockerClient, NewDockerUtils(dockerClient))
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
	Info(ctx context.Context) (types.Info, error)
	NodeInspectWithRaw(ctx context.Context, nodeID string) (swarm.Node, []byte, error)

	TaskLogs(ctx context.Context, taskID string, options types.ContainerLogsOptions) (io.ReadCloser, error)

	CopyToContainer(ctx context.Context, container string, dstPath string, content io.Reader, options types.CopyToContainerOptions) error
	ContainerList(ctx context.Context, options types.ContainerListOptions) ([]types.Container, error)
}

func newDockerImageBuilder(dockerClient dockerImageBuilderDockerClient, dockerUtils DockerUtilsInterface) *DockerImageBuilder {
	return &DockerImageBuilder{
		dockerClient: dockerClient,
		dockerUtils:  dockerUtils,
	}
}

type DockerImageBuilder struct {
	dockerClient dockerImageBuilderDockerClient
	dockerUtils  DockerUtilsInterface
}

func (d *DockerImageBuilder) buildEnvVars(imageName string) []string {
	credentials := findMatchingCredential(imageName)
	if credentials != nil {
		return []string{
			fmt.Sprintf("DOCKER_REGISTRY_ADDRESS=%s", credentials.Address),
			fmt.Sprintf("DOCKER_REGISTRY_USERNAME=%s", credentials.Username),
			fmt.Sprintf("DOCKER_REGISTRY_PASSWORD=%s", credentials.Password),
		}
	}
	log.Infof("No credentials in config for image %s", imageName)
	return []string{}
}

func (d *DockerImageBuilder) buildConstraint(ctx context.Context) (string, error) {
	nodeInfo, err := d.dockerClient.Info(ctx)
	if err != nil {
		return "", fmt.Errorf("unable to retrieve Docker info: %s", err)
	}
	currentNodeId := nodeInfo.Swarm.NodeID
	if currentNodeId == "" {
		return "", fmt.Errorf("docker node %s is not a Swarm node", nodeInfo.ID)
	}
	nodeInspectResult, _, err := d.dockerClient.NodeInspectWithRaw(ctx, currentNodeId)
	if err != nil {
		return "", fmt.Errorf("unable to inspect Docker node %s : %s", currentNodeId, err)
	}
	volumeBackend := nodeInspectResult.Spec.Labels["volume_backend"]
	if volumeBackend == "" {
		return "", fmt.Errorf("empty volume_backend label on current node %s", currentNodeId)
	}
	constraint := fmt.Sprintf("node.labels.volume_backend==%s", volumeBackend)
	log.Debugf("built constraint '%s'", constraint)
	return constraint, nil
}

func (d *DockerImageBuilder) doDockerImageBuild(volumeName string, imageName string, buildId string, out chan *pb.DockerImageResponse) error {
	if volumeName == "" {
		log.Errorf("Volume name is empty for image %s", imageName)
		return fmt.Errorf("volume name is empty for image %s", imageName)
	}
	ctx := context.Background()
	var constraints []string

	if c.Environment != "dev" {
		constraints = append(constraints, "node.role==worker")
	}

	constraint, err := d.buildConstraint(ctx)
	if err != nil {
		log.Errorf("unable to compute constraint : %s", err)
	} else {
		constraints = append(constraints, constraint)
	}
	one := uint64(1)
	spec := swarm.ServiceSpec{
		TaskTemplate: swarm.TaskSpec{
			RestartPolicy: &swarm.RestartPolicy{
				Condition:   "on-failure",
				MaxAttempts: &one,
			},
			Placement: &swarm.Placement{
				Constraints: constraints,
			},
			ContainerSpec: &swarm.ContainerSpec{
				Image: c.BuildImage,
				Healthcheck: &container.HealthConfig{
					Test: []string{"NONE"},
				},
				Env: d.buildEnvVars(imageName),
				Args: []string{
					"--dockerfile=Dockerfile", "--context=dir:///context", "--destination=" + imageName, "--push-retry", "5", "--log-format", "text", "--cache=true",
				},
				CapabilityAdd: []string{"SYS_PTRACE"},
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
		log.Errorf("failed to remove volume %s after building an image: %v", volumeName, err)
	}
	if taskError != "" {
		return fmt.Errorf("failed to build image: %s", taskError)
	} else {
		log.Debugf("build image %s successfully", imageName)
	}

	return nil
}

func (d *DockerImageBuilder) copyLocalDirToContainer(containerID, localDir, destPath string) error {
	buf := new(bytes.Buffer)
	tw := tar.NewWriter(buf)

	err := filepath.Walk(localDir, func(filePath string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return nil
		}

		relPath, err := filepath.Rel(localDir, filePath)
		if err != nil {
			return err
		}

		header := &tar.Header{
			Name: filepath.Join(destPath, relPath),
			Mode: int64(info.Mode()),
			Size: info.Size(),
		}
		if err := tw.WriteHeader(header); err != nil {
			return err
		}

		f, err := os.Open(filePath)
		if err != nil {
			return err
		}
		defer f.Close()

		if _, err := io.Copy(tw, f); err != nil {
			return err
		}
		return nil
	})
	if err != nil {
		return err
	}

	if err := tw.Close(); err != nil {
		return err
	}

	copyOpts := types.CopyToContainerOptions{}
	return d.dockerClient.CopyToContainer(context.Background(), containerID, "/", bytes.NewReader(buf.Bytes()), copyOpts)
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
		Healthcheck: &container.HealthConfig{
			Test: []string{"NONE"},
		},
		Cmd: []string{"rsync", "-a", "--delete", "/tmp/source/", "/tmp/dest"},
	}

	hostConfig := &containerTypes.HostConfig{
		CapAdd: []string{"SYS_PTRACE"},
		Mounts: []mount.Mount{
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

	if c.DockerConfig.Host != "" {
		err = d.copyLocalDirToContainer(copyContainer.ID, buildFilesPath, "/tmp/source")
		if err != nil {
			return "", err
		}
	} else {
		hostConfig.Mounts = append(hostConfig.Mounts, mount.Mount{
			Type:     mount.TypeBind,
			Source:   buildFilesPath,
			Target:   "/tmp/source",
			ReadOnly: true,
		})
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
