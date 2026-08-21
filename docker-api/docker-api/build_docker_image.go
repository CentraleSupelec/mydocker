package main

import (
	"archive/tar"
	"archive/zip"
	"bytes"
	"context"
	"crypto/rand"
	"encoding/hex"
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
	"github.com/docker/docker/errdefs"
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
	VolumeInspect(ctx context.Context, volumeID string) (types.Volume, error)
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

	if !c.Monolithic {
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

// Diagnostics only: the volume NAME carries the per-attempt token, and that is what cleanup keys
// on. Volume metadata is not persisted until the driver call returns, so this label is exactly what
// goes missing on the timed-out create that cleanup exists to handle.
const buildAttemptLabel = "mydocker.build.attempt"

// The driver keeps working after dockerd stops waiting, so cleanup has to outlast both its own
// retry budget (SHELL_TIMEOUT_SECONDS x SHELL_RETRIES, 110s by default) and dockerd's request
// deadline.
//
// This is ONE wall-clock budget shared by the whole rollback, not a retry count. Counting attempts
// was wrong: each attempt can also burn volumeCleanupCallTimeout, so 15 attempts was up to
// 15 x (30s + 10s) = 10 minutes per helper, and container-then-volume ran that twice - a worker-pool
// slot held for twenty minutes on an error path.
//
// Not a guarantee either way: a volume appearing after the budget still leaks, and a durable reaper
// is the real answer to that.
const volumeCleanupCallTimeout = 30 * time.Second

// vars, not consts, so tests do not have to sleep through the real values.
var (
	volumeCleanupBudget   = 150 * time.Second
	volumeCleanupInterval = 10 * time.Second
)

// cleanupCallTimeout clamps a single API call to whatever is left of the shared budget, and reports
// false when there is nothing left to spend.
func cleanupCallTimeout(deadline time.Time) (time.Duration, bool) {
	remaining := time.Until(deadline)
	if remaining <= 0 {
		return 0, false
	}
	if remaining > volumeCleanupCallTimeout {
		return volumeCleanupCallTimeout, true
	}
	return remaining, true
}

// sleepBeforeRetry waits out the retry interval unless the budget would expire first, in which case
// it reports false so the caller stops instead of sleeping past its own deadline.
func sleepBeforeRetry(deadline time.Time) bool {
	remaining := time.Until(deadline)
	if remaining <= 0 {
		return false
	}
	if remaining < volumeCleanupInterval {
		time.Sleep(remaining)
		return false
	}
	time.Sleep(volumeCleanupInterval)
	return true
}

// newBuildAttemptToken returns a value unique to one call of prepareBuildFiles. It is what makes
// the volume and copy-container names unique to this attempt, so two attempts sharing a buildId
// neither collide nor clean up each other's resources.
func newBuildAttemptToken() (string, error) {
	buf := make([]byte, 8)
	if _, err := rand.Read(buf); err != nil {
		return "", err
	}
	return hex.EncodeToString(buf), nil
}

// removeCopyContainer force-removes the copy container, retrying on the same budget as the volume
// cleanup. Reports whether it is gone: while it exists it holds the build volume busy, so the caller
// must not bother trying to remove the volume if this fails.
// copyContainerState is what rollback needs to know about the copy container, and the three cases
// behave differently enough that booleans were the wrong shape for it.
type copyContainerState int

const (
	// The daemon rejected the request outright, so no container exists or can appear.
	copyContainerAbsent copyContainerState = iota
	// The create neither clearly succeeded nor clearly failed - a client timeout, a transport
	// error - so the daemon may still be creating it.
	copyContainerUncertain
	// An ID came back, or the name was already taken: a container by this name exists.
	copyContainerPresent
)

// copyContainerStateAfterCreateError classifies a failed ContainerCreate.
//
// This distinction is not cosmetic: treating every failure as uncertain made the *common* cases -
// a bad parameter, a missing image, a rejected credential - wait out the whole cleanup budget and
// then retain the volume, which leaks it. Those rejections cannot turn into a container later.
func copyContainerStateAfterCreateError(err error) copyContainerState {
	switch {
	case errdefs.IsConflict(err):
		// The name is taken, which means a container by that name is there to remove.
		return copyContainerPresent
	case errdefs.IsInvalidParameter(err), errdefs.IsNotFound(err), errdefs.IsUnauthorized(err):
		// The daemon rejected the request itself, so nothing was created.
		return copyContainerAbsent
	default:
		// Timeouts, transport failures, anything unclassified: assume it may exist.
		//
		// Forbidden lands here deliberately, and not as an oversight. Docker authorization plugins
		// run in two phases, request and response, and a response-phase denial returns 403 AFTER
		// the create handler has already executed - so the container can exist despite the error.
		// Only classify it as absent if this deployment is known never to load an AuthZ plugin, and
		// write that invariant down here if you do.
		return copyContainerUncertain
	}
}

// existenceConfirmed distinguishes the two rollback cases, because not-found means different things
// in each. If the container definitely exists, not-found proves it is gone. If the create outcome
// was uncertain, a first not-found proves nothing: it may still materialise a moment later.
func (d *DockerImageBuilder) removeCopyContainer(containerRef string, deadline time.Time, existenceConfirmed bool) bool {
	for {
		callTimeout, ok := cleanupCallTimeout(deadline)
		if !ok {
			return false
		}
		ctx, cancel := context.WithTimeout(context.Background(), callTimeout)
		err := d.dockerClient.ContainerRemove(ctx, containerRef, types.ContainerRemoveOptions{
			Force:         true,
			RemoveVolumes: false,
			RemoveLinks:   false,
		})
		cancel()

		switch {
		case err == nil:
			// Observed and removed: settled either way.
			return true
		case errdefs.IsNotFound(err):
			if existenceConfirmed {
				// It existed and is now gone.
				return true
			}
			// Uncertain create: keep watching for it to appear rather than concluding absence.
		default:
			log.Errorf("failed to remove copy container %s during rollback, retrying: %v", containerRef, err)
		}

		if !sleepBeforeRetry(deadline) {
			return false
		}
	}
}

// removeBuildVolumeWithinDeadline deletes a build volume whose create reported an error.
//
// Two things make the obvious "remove it on error" wrong, and both were observed rather than
// theorised. dockerd applies an HTTP client timeout to volume driver calls and stops waiting while
// the driver runs to completion, so (1) a create that reported an error may nonetheless have
// produced the volume, and nothing else reclaims it - the 5 GB rbd image leaks for good - and
// (2) the volume may not exist *yet* when the error surfaces, so a single immediate remove returns
// not-found and the leak survives regardless.
//
// Hence the poll rather than a single remove.
//
// No ownership check is needed or wanted: volumeName carries a per-attempt suffix, so anything found
// under it was created by this call and nothing else can claim that name. Deliberately NOT keyed on
// the ownership label - dockerd persists volume metadata only after the driver call returns, so the
// very volume this exists to reclaim is the one most likely to be rediscovered with no labels, and a
// label-keyed check would read that as another attempt's and leak it.
//
// Shares one wall-clock deadline with the container removal, so the whole rollback is bounded by
// volumeCleanupBudget rather than by a retry count that ignores per-call timeouts.
func (d *DockerImageBuilder) removeBuildVolumeWithinDeadline(volumeName string, deadline time.Time) {
	for {
		callTimeout, ok := cleanupCallTimeout(deadline)
		if !ok {
			break
		}
		ctx, cancel := context.WithTimeout(context.Background(), callTimeout)
		_, err := d.dockerClient.VolumeInspect(ctx, volumeName)
		cancel()

		if err != nil {
			if !errdefs.IsNotFound(err) {
				// Retry rather than abandon: an inspect can fail for the same transient reason the
				// create did (a driver still busy, a daemon under load), and giving up on the first
				// one leaks the image for a condition that would have cleared.
				log.Errorf("cannot inspect volume %s for cleanup, retrying: %v", volumeName, err)
			}
			// Either not there yet - the driver may still be creating it after dockerd gave up
			// waiting - or a transient inspect failure. Both want another look.
			if !sleepBeforeRetry(deadline) {
				break
			}
			continue
		}

		rmTimeout, ok := cleanupCallTimeout(deadline)
		if !ok {
			break
		}
		rmCtx, rmCancel := context.WithTimeout(context.Background(), rmTimeout)
		err = d.dockerClient.VolumeRemove(rmCtx, volumeName, true)
		rmCancel()
		if err == nil {
			return
		}
		// Retry inside the same budget rather than giving up on one failure: a remove can fail
		// because the driver is still busy with the create it never got to acknowledge, and that
		// is transient. Giving up here left the image leaked for a reason that would have cleared.
		log.Errorf("failed to remove volume %s after a failed create, retrying: %v", volumeName, err)
		if !sleepBeforeRetry(deadline) {
			break
		}
	}
	log.Errorf("volume %s could not be cleaned up within the window; it may be leaked", volumeName)
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

	// Ownership is encoded in the NAME, not in a label. buildId arrives on the request, so two
	// attempts can share it, and a shared name is what makes cleanup ambiguous. A per-attempt
	// suffix makes the name unique to this call, so anything found under it was created here.
	//
	// A label cannot carry this: dockerd persists volume metadata only once the driver call
	// returns, so a create that timed out can leave a volume that is later rediscovered with no
	// labels at all - and cleanup keyed on labels would read that as another attempt's volume and
	// leak it. The label below is kept for diagnostics only.
	attemptToken, err := newBuildAttemptToken()
	if err != nil {
		return "", err
	}
	volumeName := fmt.Sprintf("volume_build_%s_%s", buildId, attemptToken)
	s := strconv.Itoa(5 * 1024)
	driverOpts := map[string]string{"size": s}

	options := volumeTypes.VolumeCreateBody{
		Driver:     "centralesupelec/mydockervolume",
		DriverOpts: driverOpts,
		Labels:     map[string]string{buildAttemptLabel: attemptToken},
		Name:       volumeName,
	}

	_, err = d.dockerClient.VolumeCreate(context.Background(), options)
	if err != nil {
		d.removeBuildVolumeWithinDeadline(volumeName, time.Now().Add(volumeCleanupBudget))
		return "", err
	}

	// The container name is fixed here, before ContainerCreate is called, precisely because that
	// call can time out at the client while the daemon still creates the container: no ID comes
	// back, and a rollback keyed on the ID would skip a container that exists and holds the volume
	// busy. ContainerRemove accepts a name, so the name is a usable handle from this point on.
	copyContainerName := fmt.Sprintf("copy_volume_%s_%s", buildId, attemptToken)

	// From here the volume exists, and every remaining failure returns "" - so the caller never
	// learns the name and can never clean it up. Roll back instead of leaking.
	succeeded := false
	// Absent until ContainerCreate is attempted; the call itself sets this from its outcome.
	copyContainerStatus := copyContainerAbsent
	defer func() {
		if succeeded {
			return
		}
		// One wall-clock budget for the whole rollback: container first, then volume. The container
		// must actually go, because while it exists it holds the volume busy and the volume cleanup
		// below cannot succeed.
		deadline := time.Now().Add(volumeCleanupBudget)
		if copyContainerStatus != copyContainerAbsent &&
			!d.removeCopyContainer(copyContainerName, deadline, copyContainerStatus == copyContainerPresent) {
			// Deliberately NOT removing the volume here. Either the container exists and holds it,
			// or - after a timed-out create - we never managed to prove it does not exist and it
			// could still appear and mount this volume. Deleting under that uncertainty is the
			// mistake this rollback exists to avoid, so the leak is reported instead.
			log.Errorf(
				"copy container %s neither observed nor removed within the cleanup budget; retaining volume %s because its absence is unproven, both may be leaked",
				copyContainerName, volumeName,
			)
			return
		}
		d.removeBuildVolumeWithinDeadline(volumeName, deadline)
	}()

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

	if c.DockerConfig.Host == "" {
		hostConfig.Mounts = append(hostConfig.Mounts, mount.Mount{
			Type:     mount.TypeBind,
			Source:   buildFilesPath,
			Target:   "/tmp/source",
			ReadOnly: true,
		})
	}

	// Set BEFORE the call, not after: if ContainerCreate times out at the client the daemon may
	// still have created the container, and a rollback keyed on a returned ID would leave it
	// holding the volume. The name is the handle in that case.
	copyContainerStatus = copyContainerUncertain
	copyContainer, err := d.dockerClient.ContainerCreate(
		context.TODO(), contConfig, hostConfig, nil, nil, copyContainerName,
	)
	if err != nil {
		// Downgrade to absent when the daemon rejected the request outright: waiting out the whole
		// cleanup budget and then retaining the volume would leak it for a container that cannot
		// exist. Upgrade to present on a name conflict, where one demonstrably does.
		copyContainerStatus = copyContainerStateAfterCreateError(err)
		return "", err
	}
	// An ID came back, so the container definitely exists: not-found during rollback now proves it
	// is gone, rather than proving nothing.
	copyContainerStatus = copyContainerPresent

	if c.DockerConfig.Host != "" {
		err = d.copyLocalDirToContainer(copyContainer.ID, buildFilesPath, "/tmp/source")
		if err != nil {
			return "", err
		}
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
	// Removed on the happy path: clear the flag so the rollback does not try again.
	copyContainerStatus = copyContainerAbsent

	err = os.RemoveAll(buildFilesPath)
	if err != nil {
		return "", err
	}

	// The volume is now the caller's to own and to remove; rollback must not fire.
	succeeded = true
	return volumeName, err
}

func writeFile(dname string, filename string, data []byte) error {
	fullPath := filepath.Join(dname, filename)
	err := ioutil.WriteFile(fullPath, data, 0666)
	return err
}
