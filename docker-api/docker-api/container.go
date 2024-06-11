package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/Workiva/go-datastructures/queue"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/mount"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
	"github.com/docker/docker/pkg/namesgenerator"
	log "github.com/sirupsen/logrus"
)

const (
	SAVE_IMAGE                = "instrumentisto/rsync-ssh:latest"
	CEPHFS_DRIVER             = "cephfs"
	CADDY_KEY                 = "caddy_%d"
	CADDY_REVERSE_PROXY_KEY   = "caddy_%d.reverse_proxy"
	CADDY_REVERSE_PROXY_VALUE = "{{ upstreams %s }}"
	PORT_TO_CADDY_INDEX_KEY   = "port_%d_caddy_index"
	MYDOCKER_USERNAME         = "MYDOCKER_USERNAME"
	MYDOCKER_PASSWORD         = "MYDOCKER_PASSWORD"
)

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
		ip, err := getIPAdress(dockerClient)
		if err != nil {
			log.Error(err)
			continue
		}
		response := &pb.ContainerResponse{
			UserID:    request.UserID,
			CourseID:  request.CourseID,
			Options:   request.Options,
			IpAddress: ip,
		}
		mapPort := map[uint32]*pb.ResponsePort{}
		for _, port := range request.Ports {
			mapPort[port.PortToMap] = convertRequestPortToResponsePort(port)
		}
		name := createContainerName(request.UserID, request.CourseID)
		if exist, id, imageId, userPassword, existingPorts, deletionTime, err := exist(name, mapPort, dockerClient); exist {
			log.Debugf("Service %s already exists", name)
			response.ImageID = imageId
			response.AuthenticationMethod = userPassword
			response.Ports = existingPorts
			response.DeletionTime = deletionTime
			if err != nil {
				log.Error(err)
				continue
			}

			if request.Options != nil && request.Options.ForceRecreate {
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
		response.ImageID = request.ImageID
		portsArray, err := ports.Get(int64(len(request.Ports)))
		log.Debugf("Got available ports %v", portsArray)
		if err != nil {
			log.Error(err)
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
			Password: randPassword(10),
		}
		if request.Options != nil && request.Options.UserPassword != nil {
			userPassword = request.Options.UserPassword
		}

		response.AuthenticationMethod = &pb.ContainerResponse_UserPassword{
			UserPassword: userPassword,
		}

		// Pre-create volume if needed
		if request.Options != nil && request.Options.SaveStudentWork && c.PrecreateVolume {
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
				log.Errorf("Failed to pre-create volume %s with error %s", name, createImageError)
				continue
			}
		}

		err = create(name, response, dockerClient, request)
		if err != nil {
			log.Error(err)
			continue
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
		ip, err := getIPAdress(dockerClient)
		if err != nil {
			log.Error(err)
			continue
		}
		response := &pb.AdminContainerResponse{
			CourseID:      request.CourseID,
			UserName:      request.UserName,
			ForceRecreate: request.ForceRecreate,
			IpAddress:     ip,
			CourseName:    request.CourseName,
		}
		name := fmt.Sprintf("%s-admin", request.GetCourseID())
		mapPort := map[uint32]*pb.ResponsePort{}
		mapPort[request.Port.PortToMap] = convertRequestPortToResponsePort(request.Port)
		if exist, id, _, userPassword, publishedPort, _, err := exist(name, mapPort, dockerClient); exist {
			response.UserPassword = userPassword.UserPassword
			response.Port = publishedPort[0]
			if err != nil {
				log.Error(err)
				continue
			}

			if request.ForceRecreate {
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
			Password: randPassword(10),
		}
		err = createAdmin(name, response, dockerClient)
		if err != nil {
			log.Error(err)
			continue
		}
		out <- response
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
		Mode: swarm.ServiceMode{
			ReplicatedJob: &swarm.ReplicatedJob{},
		},
		TaskTemplate: swarm.TaskSpec{
			Placement: &swarm.Placement{
				Constraints: []string{fmt.Sprintf("node.id==%s", nodeId)},
			},
			RestartPolicy: &swarm.RestartPolicy{
				Condition:   "on-failure",
				MaxAttempts: &one,
			},
			ContainerSpec: &swarm.ContainerSpec{
				Image: SAVE_IMAGE,
				Args:  []string{"rsync", "-a", "--delete", "--exclude='lost+found'", "/tmp/source/", "/tmp/dest"},
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

func exist(
	name string,
	mapPorts map[uint32]*pb.ResponsePort,
	dockerClient *client.Client,
) (bool, string, string, *pb.ContainerResponse_UserPassword, []*pb.ResponsePort, int64, error) {
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("name", name)
	services, err := dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{Filters: filtersArgs})
	if err != nil {
		return false, "", "", nil, []*pb.ResponsePort{}, 0, err
	}
	switch len(services) {
	case 0:
		return false, "", "", nil, []*pb.ResponsePort{}, 0, nil
	case 1:
		service := services[0]
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
			return true, service.ID, imageID, authenticationMethod, ports, 0, nil
		}
		return true, service.ID, imageID, authenticationMethod, ports, deletionTime, nil
	default:
		return false, "", "", nil, []*pb.ResponsePort{}, 0, errors.New("Something is wrong ... Multiple service have the same name")
	}
}

func getIPAdress(dockerClient *client.Client) (string, error) {
	nodes, err := dockerClient.NodeList(context.TODO(), types.NodeListOptions{})
	if err != nil {
		return "", err
	}
	for _, node := range nodes {
		// Return the first active node
		if node.Spec.Availability == swarm.NodeAvailabilityActive && node.Status.State == swarm.NodeStateReady {
			return node.Status.Addr, nil
		}
	}
	return "", errors.New("No active node found ... Return empty string")
}

func create(name string, response *pb.ContainerResponse, dockerClient *client.Client, request *pb.ContainerRequest) error {
	var mounts []mount.Mount
	var args []string

	if request.Options != nil && request.Options.Command != "" {
		command := request.Options.Command
		command = strings.Replace(command, "{{USERNAME}}", response.GetUserPassword().Username, -1)
		command = strings.Replace(command, "{{PASSWORD}}", response.GetUserPassword().Password, -1)
		args = commandToParts(command)
	}

	if request.Options != nil && request.Options.SaveStudentWork {
		mounts = []mount.Mount{
			{
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
			},
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
	for _, label := range request.GetOptions().GetMandatoryLabels() {
		constraints = append(constraints, fmt.Sprintf("node.labels.%s==%s", label.GetKey(), label.GetValue()))
	}
	for _, label := range request.GetOptions().GetForbiddenLabels() {
		constraints = append(constraints, fmt.Sprintf("node.labels.%s!=%s", label.GetKey(), label.GetValue()))
	}
	spec := swarm.ServiceSpec{
		Annotations: swarm.Annotations{
			Name:   name,
			Labels: labels,
		},
		TaskTemplate: swarm.TaskSpec{
			ContainerSpec: &swarm.ContainerSpec{
				Image:  response.ImageID,
				Args:   args,
				Mounts: mounts,
				Env:    []string{fmt.Sprintf("%s=%s", MYDOCKER_USERNAME, response.GetUserPassword().Username), fmt.Sprintf("%s=%s", MYDOCKER_PASSWORD, response.GetUserPassword().Password)},
			},
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
	s, _ := json.Marshal(spec)
	log.Debugf("Payload : '%s'", s)
	_, err := dockerClient.ServiceCreate(context.TODO(), spec, buildServiceCreateOptions(response.GetImageID()))
	if err != nil {
		return err
	}
	return nil
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
			ContainerSpec: &swarm.ContainerSpec{
				Image: c.AdminImage,
				Args:  []string{response.GetUserPassword().Username, response.GetUserPassword().Password, "--branding.name", "MyDocker"},
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
