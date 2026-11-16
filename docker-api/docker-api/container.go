package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/Workiva/go-datastructures/queue"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/mount"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
	"github.com/docker/docker/pkg/namesgenerator"
	log "github.com/sirupsen/logrus"
)

type StorageBackend string

const (
	SAVE_IMAGE                                                = "instrumentisto/rsync-ssh:latest"
	CEPHFS_DRIVER                                             = "cephfs"
	CADDY_KEY                                                 = "caddy_%d"
	CADDY_REVERSE_PROXY_KEY                                   = "caddy_%d.reverse_proxy"
	CADDY_REVERSE_PROXY_VALUE                                 = "{{ upstreams %s }}"
	CADDY_REVERSE_PROXY_STREAM_CLOSE_DELAY_KEY                = "caddy_%d.reverse_proxy.stream_close_delay"
	PORT_TO_CADDY_INDEX_KEY                                   = "port_%d_caddy_index"
	MYDOCKER_USERNAME                                         = "MYDOCKER_USERNAME"
	MYDOCKER_PASSWORD                                         = "MYDOCKER_PASSWORD"
	SLURM_API_KEY                                             = "SLURM_API_KEY"
	SLURM_API_ENDPOINT                                        = "SLURM_API_ENDPOINT"
	NFS                                        StorageBackend = "NFS"
	LOCAL                                      StorageBackend = "LOCAL"
	RBD                                        StorageBackend = "RBD"
	ON_FAILURE                                                = "on-failure"
	MOUNT_TARGET                                              = "/student_volume"
)

// A start failure that is not a VisibleError is reported with this generic error
// key, and the original technical message under CONTAINER_START_ERROR_DETAIL_KEY
// in the error params.
const (
	CONTAINER_START_ERROR            = "container.start-failed"
	CONTAINER_START_ERROR_DETAIL_KEY = "detail"
)

type VisibleError struct {
	msg    string
	params map[string]string
}

func (e *VisibleError) Error() string {
	return e.msg
}
func (e *VisibleError) Params() map[string]string {
	return e.params
}

// setStartError records a failed start on the response so the back can complete
// the request instead of waiting for an answer that never comes.
func setStartError(response *pb.ContainerResponse, err error) {
	var visibleError *VisibleError
	if errors.As(err, &visibleError) {
		log.Warnf("%s , %v", err.Error(), visibleError.Params())
		response.Error = err.Error()
		response.ErrorParams = visibleError.Params()
		return
	}
	log.Error(err)
	response.Error = CONTAINER_START_ERROR
	response.ErrorParams = map[string]string{CONTAINER_START_ERROR_DETAIL_KEY: err.Error()}
}

func getOrCreateContainer(
	in <-chan *pb.ContainerRequest,
	out chan<- *pb.ContainerResponse,
	dockerClient *client.Client,
	ports *queue.Queue,
	createRbdImageIn chan<- CreateRbdImageWorkerRequest,
	createRbdImageOut <-chan CreateRbdImageWorkerResponse,
	workerIndex int,
) {
	for request := range in {
		response := &pb.ContainerResponse{
			UserID:   request.UserID,
			CourseID: request.CourseID,
			Options:  request.Options,
		}
		ip, err := getIPAdress(dockerClient, c.Monolithic)
		if err != nil {
			setStartError(response, err)
			out <- response
			continue
		}
		response.IpAddress = ip
		mapPort := map[uint32]*pb.ResponsePort{}
		for _, port := range request.Ports {
			mapPort[port.PortToMap] = convertRequestPortToResponsePort(port)
		}
		name := createContainerName(request.UserID, request.CourseID)
		// exist reports a failed lookup as not existing, so the error is checked first.
		found, id, imageId, existingAuthentication, existingPorts, deletionTime, shouldBeReplaced, err := exist(name, mapPort, dockerClient)
		if err != nil {
			setStartError(response, err)
			out <- response
			continue
		}
		if found {
			log.Debugf("Service %s already exists", name)
			response.ImageID = imageId
			response.AuthenticationMethod = existingAuthentication
			response.Ports = existingPorts
			response.DeletionTime = deletionTime

			if shouldBeReplaced || (request.Options != nil && request.Options.ForceRecreate) {
				err = deleteService(id, dockerClient)
				if err != nil {
					setStartError(response, err)
					out <- response
					continue
				}
			} else {
				out <- response
				continue
			}
		}
		response.ImageID = request.ImageID
		portsArray, err := ports.Get(int64(len(request.Ports)))
		log.Debugf("Got available ports %v", portsArray)
		if err != nil {
			setStartError(response, err)
			out <- response
			continue
		}
		response.Ports = make([]*pb.ResponsePort, len(request.Ports))
		for index, value := range request.Ports {
			response.Ports[index] = convertRequestPortToResponsePort(value)
			response.Ports[index].MapTo = portsArray[index].(uint32)
			if value.ConnexionType == "HTTP" {
				response.Ports[index].Hostname = fmt.Sprintf("%s.%s", strings.ReplaceAll(namesgenerator.GetRandomName(0), "_", "-"), c.ReverseProxyUrl)
			}
		}

		userPassword := &pb.UserPasswordMethod{
			Username: namesgenerator.GetRandomName(0),
			Password: randPassword(containerPasswordLength),
		}
		if request.Options != nil && request.Options.UserPassword != nil {
			userPassword = request.Options.UserPassword
		}

		response.AuthenticationMethod = &pb.ContainerResponse_UserPassword{
			UserPassword: userPassword,
		}

		// Pre-create volume if needed
		if request.Options != nil && request.Options.SaveStudentWork && c.PrecreateVolume && request.Options.StorageBackend.String() == string(RBD) {
			createRbdImageIn <- CreateRbdImageWorkerRequest{
				imageName:   name,
				size:        request.Options.WorkdirSize,
				workerIndex: workerIndex,
			}
			var createImageError error
			select {
			case r := <-createRbdImageOut:
				if r.imageName == name {
					createImageError = r.err
					break
				}
			case <-time.After(6 * time.Minute):
				createImageError = fmt.Errorf("create rdb image timeout after 6 minutes")
				break
			}
			if createImageError != nil {
				setStartError(response, fmt.Errorf("Failed to pre-create volume %s with error %s", name, createImageError))
				out <- response
				continue
			}
		}

		err = create(name, response, dockerClient, request)
		if err != nil {
			setStartError(response, err)
		}
		out <- response
	}
}

func saveData(in <-chan *pb.SaveDataRequest, out chan<- *pb.SaveDataResponse, dockerClient *client.Client) {
	for request := range in {
		err := doSaveData(dockerClient, request)
		var errorMessage string
		if err != nil {
			log.Error(err)
			errorMessage = err.Error()
		}
		response := &pb.SaveDataResponse{
			UserID:   request.GetUserID(),
			CourseID: request.GetCourseID(),
			Error:    errorMessage,
		}
		out <- response
	}
}

func getOrCreateAdminContainer(in <-chan *pb.AdminContainerRequest, out chan<- *pb.AdminContainerResponse, dockerClient *client.Client, ports *queue.Queue) {
	for request := range in {
		ip, err := getIPAdress(dockerClient, c.Monolithic)
		if err == nil {
			response := &pb.AdminContainerResponse{
				CourseID:      request.CourseID,
				UserName:      request.UserName,
				ForceRecreate: request.ForceRecreate,
				IpAddress:     ip,
				CourseName:    request.CourseName,
			}
			name := createAdminContainerName(request.GetCourseID())
			mapPort := map[uint32]*pb.ResponsePort{}
			mapPort[request.Port.PortToMap] = convertRequestPortToResponsePort(request.Port)
			if exist, id, _, userPassword, publishedPort, _, shouldBeReplaced, err := exist(name, mapPort, dockerClient); exist {
				response.UserPassword = userPassword.UserPassword
				response.Port = publishedPort[0]
				if err != nil {
					log.Error(err)
					continue
				}

				if shouldBeReplaced || request.ForceRecreate {
					err = deleteService(id, dockerClient)
					if err != nil {
						log.Error(err)
						continue
					}
				} else {
					out <- response
					continue
				}
			}
			portsArray, err := ports.Get(1)
			if err != nil {
				log.Error(err)
				continue
			}
			response.Port = convertRequestPortToResponsePort(request.Port)
			response.Port.MapTo = portsArray[0].(uint32)

			response.UserPassword = &pb.UserPasswordMethod{
				Username: request.GetUserName(),
				Password: randPassword(containerPasswordLength),
			}
			err = createAdmin(name, response, dockerClient)
			if err != nil {
				log.Error(err)
				continue
			}
			out <- response
		} else {
			log.Error(err)
		}
	}
}

func doSaveData(dockerClient *client.Client, request *pb.SaveDataRequest) error {
	ctx := context.Background()
	volume, err := dockerClient.VolumeInspect(ctx, createContainerName(request.GetUserID(), request.GetCourseID()))
	if err != nil {
		return fmt.Errorf("could not find volume for student %s and course %s", request.GetUserID(), request.GetCourseID())
	}
	name := createContainerName(request.UserID, request.CourseID)
	tasks, err := dockerClient.TaskList(ctx, types.TaskListOptions{Filters: filters.NewArgs(filters.Arg("service", name))})
	if err != nil || len(tasks) < 1 {
		return fmt.Errorf("could not find task for service %s", name)
	}
	nodeId := tasks[len(tasks)-1].NodeID
	one := uint64(1)
	userDir := fmt.Sprintf("%s-%s", strings.Split(request.GetUserEmail(), "@")[0], request.GetUserID())
	spec := swarm.ServiceSpec{
		// A save runs once and is finished when its task completes. The happy path removes
		// this service further down; the label is what lets the reaper clean up after a
		// docker-api that stopped between the two, which the unscoped reaper used to do by
		// accident.
		Annotations: swarm.Annotations{
			Labels: map[string]string{oneShotLabel: "true"},
		},
		TaskTemplate: swarm.TaskSpec{
			Placement: &swarm.Placement{
				Constraints: []string{fmt.Sprintf("node.id==%s", nodeId)},
			},
			RestartPolicy: &swarm.RestartPolicy{
				Condition:   ON_FAILURE,
				MaxAttempts: &one,
			},
			ContainerSpec: &swarm.ContainerSpec{
				Image: SAVE_IMAGE,
				Healthcheck: &container.HealthConfig{
					Test: []string{"NONE"},
				},
				Args:          []string{"rsync", "-a", "--delete", "--exclude='lost+found'", "/tmp/source/", "/tmp/dest"},
				CapabilityAdd: []string{"SYS_PTRACE"},
				Mounts: []mount.Mount{
					{
						Type:     mount.TypeVolume,
						Source:   volume.Name,
						Target:   "/tmp/source",
						ReadOnly: true,
						VolumeOptions: &mount.VolumeOptions{
							DriverConfig: &mount.Driver{
								Name:    volume.Driver,
								Options: volume.Options,
							},
						},
					},
					{
						Type:   mount.TypeVolume,
						Source: fmt.Sprintf("%s-submission", volume.Name),
						Target: "/tmp/dest",
						VolumeOptions: &mount.VolumeOptions{
							Labels: request.GetMetadata().GetTags(),
							DriverConfig: &mount.Driver{
								Name: CEPHFS_DRIVER,
								Options: map[string]string{
									"remote_path": fmt.Sprintf("/%s/%s/%s", c.CephFSPrefix, request.GetCourseID(), userDir),
								},
							},
						},
					},
				},
			},
		},
	}
	service, err := dockerClient.ServiceCreate(ctx, spec, types.ServiceCreateOptions{})
	if err != nil {
		return err
	}
	log.Debugf("Created service %s", service.ID)
	completed := false
	taskError := ""
	for completed == false && taskError == "" {
		tasks, err := dockerClient.TaskList(ctx, types.TaskListOptions{Filters: filters.NewArgs(filters.Arg("service", service.ID))})
		if err != nil {
			return err
		}
		if len(tasks) >= 1 {
			log.Debugf("State for %s : %s", service.ID, tasks[len(tasks)-1].Status.State)
			completed = tasks[len(tasks)-1].Status.State == swarm.TaskStateComplete
			taskError = tasks[len(tasks)-1].Status.Err
			time.Sleep(500 * time.Millisecond)
		}
	}

	if taskError != "" {
		return fmt.Errorf("failed to save data for student %s and course %s : %s", request.GetUserID(), request.GetCourseID(), taskError)
	} else {
		log.Debug(fmt.Sprintf("Saved data for student %s and course %s", request.GetUserID(), request.GetCourseID()))
	}
	err = dockerClient.ServiceRemove(ctx, service.ID)
	if err != nil {
		return err
	}
	return nil
}

type dockerExistenceCheckClient interface {
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
}

func shouldServiceBeReplaced(service swarm.Service, dockerClient dockerExistenceCheckClient) (bool, error) {
	if service.ServiceStatus.RunningTasks == service.ServiceStatus.DesiredTasks {
		return false, nil
	}
	tasks, err := dockerClient.TaskList(context.TODO(), types.TaskListOptions{Filters: filters.NewArgs(filters.KeyValuePair{
		Key:   "service",
		Value: service.Spec.Name,
	})})
	if err != nil {
		return false, nil
	}
	tasksByState := map[swarm.TaskState]int{}
	for _, task := range tasks {
		tasksByState[task.Status.State] = tasksByState[task.Status.State] + 1
	}
	// Replace if all tasks are in "complete" state
	if tasksByState[swarm.TaskStateComplete] == len(tasks) {
		log.Debugf("service %s should be replaced because all tasks are complete, %v", service.Spec.Name, tasksByState)
		return true, nil
	}
	// Replace if all tasks (except the one being retried) are in "error" state
	if tasksByState[swarm.TaskStateFailed] > 0 && tasksByState[swarm.TaskStateFailed] >= (len(tasks)-1) {
		log.Debugf("service %s should be replaced because all tasks are failed, %v", service.Spec.Name, tasksByState)
		return true, nil
	}
	return false, nil
}

func exist(
	name string,
	mapPorts map[uint32]*pb.ResponsePort,
	dockerClient *client.Client,
) (bool, string, string, *pb.ContainerResponse_UserPassword, []*pb.ResponsePort, int64, bool, error) {
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("name", name)
	services, err := dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{Filters: filtersArgs, Status: true})
	if err != nil {
		return false, "", "", nil, []*pb.ResponsePort{}, 0, false, err
	}
	switch len(services) {
	case 0:
		return false, "", "", nil, []*pb.ResponsePort{}, 0, false, nil
	case 1:
		service := services[0]
		shouldBeReplaced, err := shouldServiceBeReplaced(service, dockerClient)
		imageID := service.Spec.TaskTemplate.ContainerSpec.Image
		for _, port := range service.Endpoint.Ports {
			if _, ok := mapPorts[port.TargetPort]; ok {
				mapPorts[port.TargetPort].MapTo = port.PublishedPort
				if mapPorts[port.TargetPort].ConnexionType == "HTTP" {
					caddyIndex, _ := strconv.Atoi(service.Spec.Labels[fmt.Sprintf(PORT_TO_CADDY_INDEX_KEY, port.TargetPort)])
					mapPorts[port.TargetPort].Hostname = service.Spec.Labels[fmt.Sprintf(CADDY_KEY, caddyIndex)]
				}
			}
		}

		var username string
		var password string

		for _, envVariable := range service.Spec.TaskTemplate.ContainerSpec.Env {
			split := strings.Split(envVariable, "=")
			switch split[0] {
			case MYDOCKER_USERNAME:
				username = split[1]
			case MYDOCKER_PASSWORD:
				password = split[1]
			}
		}

		authenticationMethod := &pb.ContainerResponse_UserPassword{
			UserPassword: &pb.UserPasswordMethod{
				Username: username,
				Password: password,
			}}
		ports := make([]*pb.ResponsePort, len(mapPorts))
		index := 0
		for _, value := range mapPorts {
			ports[index] = value
			index++
		}
		deletionTime, err := strconv.ParseInt(service.Spec.Labels["deletionTime"], 10, 64)
		if err != nil {
			return true, service.ID, imageID, authenticationMethod, ports, 0, shouldBeReplaced, nil
		}
		return true, service.ID, imageID, authenticationMethod, ports, deletionTime, shouldBeReplaced, nil
	default:
		return false, "", "", nil, []*pb.ResponsePort{}, 0, false, errors.New("Something is wrong ... Multiple service have the same name")
	}
}

func getIPAdress(dockerClient *client.Client, monolithic bool) (string, error) {
	nodes, err := dockerClient.NodeList(context.TODO(), types.NodeListOptions{})
	if err != nil {
		return "", err
	}
	for _, node := range nodes {
		// Return the first active node (manager or worker based on if monolithic is true or false)
		// NOTE : If swarm contains mutliple nodes that are all managers and has no workers, we should use monolithic = true even though
		// it is not technically a monolithic setup
		if node.Spec.Availability == swarm.NodeAvailabilityActive && node.Status.State == swarm.NodeStateReady {
			if monolithic && node.ManagerStatus != nil {
				return strings.SplitN(node.ManagerStatus.Addr, ":", 2)[0], nil
			} else if !monolithic && node.ManagerStatus == nil {
				return node.Status.Addr, nil
			}
		}
	}
	return "", errors.New("No active node found ... Return empty string")
}

func create(name string, response *pb.ContainerResponse, dockerClient *client.Client, request *pb.ContainerRequest) error {
	var mounts []mount.Mount
	var args []string

	runUid, err := runUser(request.GetOptions())
	if err != nil {
		return err
	}

	if request.Options != nil && request.Options.Command != "" {
		command := request.Options.Command
		command = strings.Replace(command, "{{USERNAME}}", response.GetUserPassword().Username, -1)
		command = strings.Replace(command, "{{PASSWORD}}", response.GetUserPassword().Password, -1)
		command = strings.Replace(command, "{{IP}}", response.GetIpAddress(), -1)

		command = substituteCommandPorts(command, response.Ports)
		args = commandToParts(command)
	}

	for _, volumeMount := range c.VolumeMounts {
		mounts = append(mounts, mount.Mount{
			Type:     mount.TypeVolume,
			Source:   volumeMount.Source,
			Target:   volumeMount.Target,
			ReadOnly: true,
			VolumeOptions: &mount.VolumeOptions{
				DriverConfig: &mount.Driver{
					Name: "centralesupelec/mydockervolume",
					Options: map[string]string{
						"mkfsOptions": "-O ^mmp",
					},
				},
			},
		})
	}

	if request.Options != nil {
		if request.Options.SaveStudentWork {
			mounts = append(mounts, mount.Mount{
				Type:   mount.TypeVolume,
				Source: name,
				Target: request.Options.WorkdirPath,
				VolumeOptions: &mount.VolumeOptions{
					DriverConfig: &mount.Driver{
						Name: "centralesupelec/mydockervolume",
						Options: map[string]string{
							"size":        fmt.Sprintf("%d", request.Options.WorkdirSize),
							"mkfsOptions": "-O ^mmp",
						},
					},
				},
			})
		}

		if request.Options.UseStudentVolume {
			if res, err := canCreateStudentVolume(dockerClient, request); res {
				if fullCmd := buildVolumeInitCommand(request.Options.GetPaths(), request.Options.GetUid(), runUid); "" != fullCmd {
					log.Debugln("Full Command :", fullCmd)

					serviceSpec := swarm.ServiceSpec{
						Annotations: swarm.Annotations{
							Name: fmt.Sprintf("volume-init-%s", name),
							// One-shot, like the save and the build: removed below once it
							// completes, and reaped by label if this process never gets
							// there. A leaked volume-init also keeps a mount on the student
							// volume, which stops the plugin unmapping it.
							Labels: map[string]string{oneShotLabel: "true"},
						},
						TaskTemplate: swarm.TaskSpec{
							ContainerSpec: &swarm.ContainerSpec{
								Image:   "alpine:3.22.2",
								Command: []string{"sh", "-c", fullCmd},
								User:    "root",
								Mounts: []mount.Mount{
									{
										Type:   mount.TypeVolume,
										Source: createStudentVolumeName(request.UserID),
										Target: MOUNT_TARGET,
										VolumeOptions: &mount.VolumeOptions{
											DriverConfig: &mount.Driver{
												Name: "centralesupelec/mydockervolume",
												Options: map[string]string{
													"size":        fmt.Sprintf("%d", c.StudentVolumeSize),
													"mkfsOptions": "-O ^mmp",
												},
											},
										},
									},
								},
							},
							RestartPolicy: &swarm.RestartPolicy{
								Condition: swarm.RestartPolicyConditionNone,
							},
						},
					}

					started := time.Now()
					resp, err := dockerClient.ServiceCreate(context.TODO(), serviceSpec, types.ServiceCreateOptions{})
					if err != nil {
						return err
					}

					for {
						tasks, err := dockerClient.TaskList(context.TODO(), types.TaskListOptions{
							Filters: filters.NewArgs(
								filters.KeyValuePair{Key: "service", Value: resp.ID},
							),
						})
						if err != nil {
							return err
						}

						if len(tasks) == 0 {
							time.Sleep(100 * time.Millisecond)
							continue
						}

						task := tasks[0]
						switch task.Status.State {
						case swarm.TaskStateComplete:
							logVolumeInit(dockerClient, serviceSpec.Name, task, time.Since(started))
							goto Done
						case swarm.TaskStateFailed, swarm.TaskStateRejected:
							logVolumeInit(dockerClient, serviceSpec.Name, task, time.Since(started))
							return fmt.Errorf("volume init service failed: %s", task.Status.Err)
						}

						time.Sleep(100 * time.Millisecond)
					}
				Done:
					if err := dockerClient.ServiceRemove(context.TODO(), resp.ID); err != nil {
						log.Errorf("Failed to remove service %s: %v", resp.ID, err)
					}
				}

				mounts = append(mounts, mount.Mount{
					Type:   mount.TypeVolume,
					Source: createStudentVolumeName(request.UserID),
					Target: request.Options.StudentVolumePath,
					VolumeOptions: &mount.VolumeOptions{
						DriverConfig: &mount.Driver{
							Name: "centralesupelec/mydockervolume",
							Options: map[string]string{
								"size":        fmt.Sprintf("%d", c.StudentVolumeSize),
								"mkfsOptions": "-O ^mmp",
							},
						},
					},
				})
			} else {
				return err
			}
		}
	}
	var labels map[string]string = make(map[string]string)

	if request.Metadata != nil {
		labels = request.Metadata.Tags
	}

	deletionTime, parseError := strconv.ParseInt(labels["deletionTime"], 10, 64)
	if parseError == nil {
		response.DeletionTime = deletionTime
	}

	limit := &swarm.Limit{
		NanoCPUs:    c.DefaultNanoCPUsLimit,
		MemoryBytes: c.DefaultMemoryBytesLimit,
	}
	if request.Options != nil && request.Options.Limit != nil {
		limit = &swarm.Limit{
			NanoCPUs:    request.Options.Limit.NanoCPUs,
			MemoryBytes: request.Options.Limit.MemoryBytes,
		}
	}

	ports := []swarm.PortConfig{}
	caddyIndex := 0
	for _, port := range response.Ports {
		ports = append(ports, swarm.PortConfig{
			TargetPort:    port.PortToMap,
			PublishedPort: port.MapTo,
			Protocol:      swarm.PortConfigProtocolTCP,
		})

		if port.ConnexionType == "HTTP" {
			labels[fmt.Sprintf(CADDY_KEY, caddyIndex)] = port.Hostname
			labels[fmt.Sprintf(CADDY_REVERSE_PROXY_KEY, caddyIndex)] = fmt.Sprintf(CADDY_REVERSE_PROXY_VALUE, strconv.FormatUint(uint64(port.PortToMap), 10))
			labels[fmt.Sprintf(PORT_TO_CADDY_INDEX_KEY, port.PortToMap)] = strconv.FormatInt(int64(caddyIndex), 10)
			labels[fmt.Sprintf(CADDY_REVERSE_PROXY_STREAM_CLOSE_DELAY_KEY, caddyIndex)] = c.CaddyStreamCloseDelay

			if c.CaddyTlsInternal {
				labels[fmt.Sprintf(CADDY_KEY+".tls", caddyIndex)] = "internal"
			} else {
				labels[fmt.Sprintf(CADDY_KEY+".tls", caddyIndex)] = fmt.Sprintf("%s %s", c.CaddyTlsCertificatePath, c.CaddyTlsKeyPath)
			}
			caddyIndex += 1
		}
	}

	if caddyIndex != 0 {
		labels["caddy_ingress_network"] = c.CaddyOverlayNetwork
	}

	networks := []swarm.NetworkAttachmentConfig{}
	networks = append(networks, swarm.NetworkAttachmentConfig{Target: c.CaddyOverlayNetwork})

	var resources []swarm.GenericResource
	for key, value := range request.GetOptions().GetGenericResources() {
		resources = append(resources, swarm.GenericResource{DiscreteResourceSpec: &swarm.DiscreteGenericResource{
			Kind:  key,
			Value: int64(value),
		}})
	}
	var constraints []string

	if !c.Monolithic {
		constraints = append(constraints, "node.role==worker")
	}

	for _, label := range request.GetOptions().GetMandatoryLabels() {
		constraints = append(constraints, fmt.Sprintf("node.labels.%s==%s", label.GetKey(), label.GetValue()))
	}
	for _, label := range request.GetOptions().GetForbiddenLabels() {
		constraints = append(constraints, fmt.Sprintf("node.labels.%s!=%s", label.GetKey(), label.GetValue()))
	}

	envVars := containerEnv(response, request)

	spec := swarm.ServiceSpec{
		Annotations: swarm.Annotations{
			Name:   name,
			Labels: labels,
		},
		TaskTemplate: swarm.TaskSpec{
			RestartPolicy: &swarm.RestartPolicy{
				Condition: ON_FAILURE,
			},
			ContainerSpec: buildStudentContainerSpec(response, args, mounts, runUid, envVars),
			Resources: &swarm.ResourceRequirements{
				Limits:       limit,
				Reservations: &swarm.Resources{GenericResources: resources},
			},
			Placement: &swarm.Placement{Constraints: constraints},
			Networks:  networks,
		},
		EndpointSpec: &swarm.EndpointSpec{
			Ports: ports,
		}}
	// Redact credential env vars (Slurm session key, container password) from
	// the debug payload: they are secrets and must not land in logs.
	logSpec := spec
	logTask := spec.TaskTemplate
	if logTask.ContainerSpec != nil {
		csCopy := *logTask.ContainerSpec
		csCopy.Env = redactEnv(csCopy.Env)
		logTask.ContainerSpec = &csCopy
	}
	logSpec.TaskTemplate = logTask
	s, _ := json.Marshal(logSpec)
	log.Debugf("Payload : '%s'", s)
	_, err = dockerClient.ServiceCreate(context.TODO(), spec, buildServiceCreateOptions(response.GetImageID()))
	if err != nil {
		return err
	}
	return nil
}

// containerEnv returns the environment of a student container: its credentials, plus the Slurm
// gateway key and endpoint when the request carries both.
func containerEnv(response *pb.ContainerResponse, request *pb.ContainerRequest) []string {
	env := []string{
		fmt.Sprintf("%s=%s", MYDOCKER_USERNAME, response.GetUserPassword().GetUsername()),
		fmt.Sprintf("%s=%s", MYDOCKER_PASSWORD, response.GetUserPassword().GetPassword()),
	}
	slurm := request.GetOptions().GetSlurmApiContainerParams()
	if slurm.GetKey() != "" && slurm.GetEndpoint() != "" {
		env = append(env,
			fmt.Sprintf("%s=%s", SLURM_API_KEY, slurm.GetKey()),
			fmt.Sprintf("%s=%s", SLURM_API_ENDPOINT, slurm.GetEndpoint()),
		)
	}
	return env
}

// redactEnv returns a copy of env with the values of credential variables
// masked, for safe logging. It never mutates the input slice.
func redactEnv(env []string) []string {
	redacted := make([]string, len(env))
	for i, e := range env {
		if kv := strings.SplitN(e, "=", 2); len(kv) == 2 {
			switch kv[0] {
			case SLURM_API_KEY, MYDOCKER_PASSWORD:
				redacted[i] = kv[0] + "=***REDACTED***"
				continue
			}
		}
		redacted[i] = e
	}
	return redacted
}

// buildStudentContainerSpec returns the student container. An empty runUid
// leaves User unset, so the image's own user applies.
func buildStudentContainerSpec(response *pb.ContainerResponse, args []string, mounts []mount.Mount, runUid string, env []string) *swarm.ContainerSpec {
	return &swarm.ContainerSpec{
		Image: response.ImageID,
		Healthcheck: &container.HealthConfig{
			Test: []string{"NONE"},
		},
		Args:          args,
		User:          runUid,
		CapabilityAdd: []string{"SYS_PTRACE"},
		Mounts:        mounts,
		Env:           env,
	}
}

// Same rule as the back's validation. Checked again here because the value is
// spliced into the volume-init shell command.
var runUidPattern = regexp.MustCompile(`^[1-9][0-9]*(:[1-9][0-9]*)?$`)

// runUser returns the course's forced run uid ("uid" or "uid:gid"), or "" to
// keep the image's own user.
func runUser(options *pb.ContainerRequestOptions) (string, error) {
	runUid := options.GetRunUid()
	if "" != runUid && !runUidPattern.MatchString(runUid) {
		return "", fmt.Errorf("invalid run uid %q", runUid)
	}
	return runUid, nil
}

// volumeInitWalkSeconds returns the cap on the ownership walk of a volume-init:
// VolumeInitWalkSeconds from the config, or the default when unset or not
// positive (busybox timeout 0 would mean no cap at all).
func volumeInitWalkSeconds() int {
	if c.VolumeInitWalkSeconds > 0 {
		return c.VolumeInitWalkSeconds
	}
	return defaultVolumeInitWalkSeconds
}

// volumeInitWalkMarker starts the line the walk prints when it ends, followed by
// find's exit code. busybox timeout reports a cap hit as 143, GNU timeout as 124.
const volumeInitWalkMarker = "volume-init walk: find exit "

// volumeInitWalkResult returns what the walk reported after the marker, and
// whether the cap stopped it. It returns "" when the logs hold no marker: no run
// uid, a walk that never finished, or a marker pushed out of the log tail.
func volumeInitWalkResult(logs string) (string, bool) {
	for _, line := range strings.Split(logs, "\n") {
		if strings.HasPrefix(line, volumeInitWalkMarker) {
			// A walk without find errors ends its line with ": ", dropped here.
			result := strings.TrimSuffix(strings.TrimSpace(strings.TrimPrefix(line, volumeInitWalkMarker)), ":")
			return result, strings.HasPrefix(result, "143,") || strings.HasPrefix(result, "124,")
		}
	}
	return "", false
}

// volumeInitRefusedMarker starts the line a guarded step prints when it fails,
// followed by the step, its exit code and its first error line.
const volumeInitRefusedMarker = "volume-init refused: "

// tolerate wraps one step of the volume-init so that its failure, a chown or
// chmod the volume refuses or a directory find cannot read, prints a marker
// line instead of ending the && chain: the lab starts on a partly fixed volume
// rather than not at all. The step's stderr is kept to its first line so a step
// refused on every file cannot push the markers out of the log tail.
func tolerate(cmd, step string) string {
	return fmt.Sprintf(`{ %s 2>/tmp/volume-init-err || { rc=$?; echo "%s%s exit $rc: $(head -n 1 /tmp/volume-init-err)"; }; }`,
		cmd, volumeInitRefusedMarker, step)
}

// volumeInitRefusals returns what each failed guarded step reported after the
// marker, in log order.
func volumeInitRefusals(logs string) []string {
	var refused []string
	for _, line := range strings.Split(logs, "\n") {
		if strings.HasPrefix(line, volumeInitRefusedMarker) {
			refused = append(refused, strings.TrimSpace(strings.TrimPrefix(line, volumeInitRefusedMarker)))
		}
	}
	return refused
}

// volumeInitWalkFailed reports whether the ownership walk did not finish clean:
// find or xargs exited non-zero, which covers the cap hit, chowns refused deeper
// in the tree (xargs 123) and directories find could not read. No walk ("") is
// not a failure; a result that does not parse is.
func volumeInitWalkFailed(walk string) bool {
	if "" == walk {
		return false
	}
	var findExit, xargsExit int
	if n, _ := fmt.Sscanf(walk, "%d, xargs exit %d", &findExit, &xargsExit); 2 != n {
		return true
	}
	return 0 != findExit || 0 != xargsExit
}

// volumeInitNeedsAttention reports whether a volume-init run left the volume
// partly fixed or failed. Some prods run the Go API at LogLevel error, so such a
// run is logged at error level to be seen at all. An unknown exit code (-1, no
// container status) does not count: a failed task is reported by its caller.
func volumeInitNeedsAttention(exitCode int, walk string, refused []string) bool {
	return 0 < exitCode || volumeInitWalkFailed(walk) || 0 < len(refused)
}

// logVolumeInit logs one line per volume-init run: the walk can cost up to its
// cap (two minutes by default) of a lab start, and nothing else records it.
func logVolumeInit(dockerClient *client.Client, serviceName string, task swarm.Task, elapsed time.Duration) {
	exitCode := -1
	if task.Status.ContainerStatus != nil {
		exitCode = task.Status.ContainerStatus.ExitCode
	}

	// The deadline covers the read too: a log line must never hold a lab start.
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	walk, capHit, refused := "", false, []string(nil)
	reader, err := dockerClient.TaskLogs(ctx, task.ID, types.ContainerLogsOptions{ShowStdout: true, ShowStderr: true, Tail: "200"})
	if err != nil {
		log.Warnf("volume-init %s: cannot read logs: %v", serviceName, err)
	} else {
		defer func() { _ = reader.Close() }()
		if logs, _, err := readTaskLogs(reader, 200); err != nil {
			log.Warnf("volume-init %s: cannot decode logs: %v", serviceName, err)
		} else {
			walk, capHit = volumeInitWalkResult(logs)
			refused = volumeInitRefusals(logs)
		}
	}

	logf := log.Infof
	if volumeInitNeedsAttention(exitCode, walk, refused) {
		logf = log.Errorf
	}
	logf("volume-init %s: %s after %.1f s, exit code %d, walk %q, cap hit %t, refused %q",
		serviceName, task.Status.State, elapsed.Seconds(), exitCode, walk, capHit, refused)
}

// buildVolumeInitCommand returns the shell command the volume-init service runs
// on the student volume, or "" when there is nothing to do.
//
// With a run uid, an empty volume is first seeded with a .profile and a .bashrc
// owned by that uid (StudentVolumeSeed, on by default), so Docker no longer
// copies the image's content into it with the image's owner. Then every start
// gives the volume back to that uid: the root and its direct entries
// unconditionally, then a stat-only walk of the whole tree that chowns only the
// mismatched entries. The walk is capped (VolumeInitWalkSeconds, default 120 s)
// and its failure ignored, so the largest volumes get partial coverage instead
// of a failed start. change_owner rows then keep their chmod but skip their
// chown, which would hand the folders back to the old uid.
func buildVolumeInitCommand(paths []*pb.Path, targetUID, runUid string) string {
	cmdParts := []string{}

	if "" != runUid {
		// The gid defaults to the uid, as busybox does with "uid:". Never write
		// the trailing colon itself: busybox rejects "1000:100:".
		ids := strings.SplitN(runUid, ":", 2)
		uid, gid := ids[0], ids[0]
		if 2 == len(ids) {
			gid = ids[1]
		}
		mismatch := fmt.Sprintf(`\( ! -user %s -o ! -group %s \)`, uid, gid)

		if c.StudentVolumeSeed {
			cmdParts = append(cmdParts, tolerate(buildVolumeSeedCommand(uid, gid), "seed"))
		}

		cmdParts = append(cmdParts,
			// The root and its direct entries, always.
			tolerate(fmt.Sprintf("find %s -maxdepth 1 %s -exec chown -h %s:%s {} +", MOUNT_TARGET, mismatch, uid, gid), "root chown"),
			// The cap kills find only. xargs still chowns every name find printed
			// and then sees the pipe close, so a capped walk keeps its progress,
			// which a pending "-exec {} +" batch would not. Over NFS every chown is
			// a round trip to the server: one at a time managed about 200 entries a
			// second, 8 batches in parallel about 850, and 16 did no better.
			// chown errors are dropped so a volume that refuses
			// every chown cannot flood the log; xargs exits 123 instead. The last
			// line reports both exit codes for the Go API's log. A find killed
			// mid-write can leave a truncated last name, which xargs still passes
			// to chown: accepted, since any path it resolves to lies in the student
			// volume or in this throwaway container.
			// find's own errors (unreadable directories) go to a file too, and only
			// their first line reaches the marker, so they cannot push the other
			// markers out of the log tail.
			fmt.Sprintf(`({ timeout %d find %s %s -print0 2>/tmp/walk-err; echo $? >/tmp/walk-exit; } | xargs -0 -r -n 256 -P 8 chown -h %s:%s 2>/dev/null; rc=$?; echo "%s$(cat /tmp/walk-exit), xargs exit $rc: $(head -n 1 /tmp/walk-err)")`,
				volumeInitWalkSeconds(), MOUNT_TARGET, mismatch, uid, gid, volumeInitWalkMarker),
		)
	}

	// Rows are named by position, never by pattern: the pattern is admin input
	// and would land inside the echo's double quotes.
	for i, p := range paths {
		recursiveFlag := ""

		if p.GetRecursive() {
			recursiveFlag = "-R"
		}

		for _, chmod := range buildChmodCommandsToApplyExecuteOnlyToFolders(MOUNT_TARGET, p.GetValue(), p.GetPermissions(), p.Recursive) {
			cmdParts = append(cmdParts, tolerate(chmod, fmt.Sprintf("chmod row %d", i+1)))
		}

		if p.ChangeOwner && "" != targetUID && "" == runUid {
			cmdParts = append(cmdParts, tolerate(
				fmt.Sprintf(
					// "{} +", not "\;": with "\;" find ignores a failed chown
					// and the guard around this step could never report it.
					"find %s -type d -wholename \"%s\" -exec chown %s %s: {} +",
					MOUNT_TARGET,
					p.GetValue(),
					recursiveFlag,
					targetUID,
				),
				fmt.Sprintf("change_owner row %d", i+1),
			))
		}
	}

	return strings.Join(cmdParts, " && ")
}

type createStudentVolumeDockerClient interface {
	ServiceList(ctx context.Context, options types.ServiceListOptions) ([]swarm.Service, error)
}

func buildChmodCommandsToApplyExecuteOnlyToFolders(basePath, pattern string, mode uint32, recursive bool) []string {
	modeStr := fmt.Sprintf("%03d", mode)
	fileModeStr := stripExecBits(modeStr)

	// "{} +" runs one chmod per batch of names, not one per file: over NFS each
	// process is a round trip. Unlike "\;", a failed batch makes find exit
	// non-zero, so callers must guard each command.
	var cmds []string
	if recursive {
		cmdExecFiles := fmt.Sprintf(`find %s -path "%s*" -type f -perm /a=x -exec chmod %s {} +`, basePath, pattern, modeStr)
		cmdFiles := fmt.Sprintf(`find %s -path "%s*" -type f ! -perm /a=x -exec chmod %s {} +`, basePath, pattern, fileModeStr)
		cmdDirs := fmt.Sprintf(`find %s -path "%s*" -type d -exec chmod %s {} +`, basePath, pattern, modeStr)
		cmds = []string{cmdExecFiles, cmdFiles, cmdDirs}
	} else {
		cmdDir := fmt.Sprintf(`find %s -type d -wholename "%s" -exec chmod %s {} +`, basePath, pattern, modeStr)
		cmds = []string{cmdDir}
	}

	return cmds
}

func stripExecBits(mode string) string {
	if len(mode) != 3 {
		return mode
	}
	result := ""
	for _, c := range mode {
		n := int(c - '0')
		n &^= 1
		result += fmt.Sprintf("%d", n)
	}
	return result
}

func canCreateStudentVolume(dockerClient createStudentVolumeDockerClient, request *pb.ContainerRequest) (bool, error) {
	switch storageBackend := request.Options.StorageBackend.String(); storageBackend {
	case string(NFS):
		return true, nil
	case string(LOCAL):
		return false, &VisibleError{msg: "student-volume.local-storage"}
	case string(RBD):
		ctx := context.TODO()
		services, err := dockerClient.ServiceList(ctx, types.ServiceListOptions{
			Filters: filters.NewArgs(filters.KeyValuePair{
				Key: "label", Value: fmt.Sprintf("userId=%s", request.UserID),
			}),
		})
		if err != nil {
			return false, err
		}
		for _, service := range services {
			for _, serviceMount := range service.Spec.TaskTemplate.ContainerSpec.Mounts {
				if serviceMount.Source == createStudentVolumeName(request.UserID) {
					return false, &VisibleError{
						msg: "student-volume.existing-rbd-service",
						params: map[string]string{
							"courseId":    service.Spec.Labels["courseId"],
							"createdAt":   service.CreatedAt.Format(time.RFC3339),
							"courseTitle": service.Spec.Labels["courseTitle"],
						},
					}
				}
			}
		}
		return true, nil
	}
	return false, fmt.Errorf("unknown storage backend %s", request.Options.StorageBackend.String())
}

func createAdmin(name string, response *pb.AdminContainerResponse, dockerClient *client.Client) error {
	spec := swarm.ServiceSpec{
		Annotations: swarm.Annotations{
			Name: name,
			Labels: map[string]string{
				"courseId": response.GetCourseID(),
			},
		},
		TaskTemplate: swarm.TaskSpec{
			RestartPolicy: &swarm.RestartPolicy{
				Condition: ON_FAILURE,
			},
			ContainerSpec: &swarm.ContainerSpec{
				Image: c.AdminImage,
				Healthcheck: &container.HealthConfig{
					Test: []string{"NONE"},
				},
				Args:          []string{response.GetUserPassword().Username, response.GetUserPassword().Password, "--branding.name", "MyDocker"},
				CapabilityAdd: []string{"SYS_PTRACE"},
				Mounts: []mount.Mount{
					{
						Type:     mount.TypeVolume,
						Source:   fmt.Sprintf("%s-submissions", response.GetCourseID()),
						ReadOnly: true,
						Target:   fmt.Sprintf("/srv/%s", response.GetCourseName()),
						VolumeOptions: &mount.VolumeOptions{
							DriverConfig: &mount.Driver{
								Name: CEPHFS_DRIVER,
								Options: map[string]string{
									"remote_path": fmt.Sprintf("/%s/%s", c.CephFSPrefix, response.GetCourseID()),
								},
							},
						},
					},
				},
			},
		},
		EndpointSpec: &swarm.EndpointSpec{
			Ports: []swarm.PortConfig{{
				TargetPort:    response.Port.PortToMap,
				PublishedPort: response.Port.MapTo,
				Protocol:      swarm.PortConfigProtocolTCP,
			}},
		}}
	_, err := dockerClient.ServiceCreate(context.TODO(), spec, buildServiceCreateOptions(c.AdminImage))
	if err != nil {
		return err
	}
	return nil
}

func deleteService(id string, dockerClient *client.Client) error {
	return dockerClient.ServiceRemove(context.TODO(), id)
}
