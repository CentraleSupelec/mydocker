package main

import (
	"context"
	"fmt"
	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
	"math"
	"math/rand"
	"regexp"
	"strings"
	"sync"
	"time"
)

type scaleUpDockerClient interface {
	deployUtilsDockerClient
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
}

func addScaleUpCron(s *gocron.Scheduler, dockerClient scaleUpDockerClient, lock *sync.Mutex, scaleUpConfig *ScaleUpConfig) error {
	tag := "scaleUp"
	service := newScaleUpService(
		dockerClient,
		log.WithFields(log.Fields{"service": tag}),
		lock,
		scaleUpConfig,
		NewDeployUtils(dockerClient),
		&AutoscalingUtils{},
	)
	_ = s.RemoveByTag(tag)
	_, err := s.Every(c.ScaleUpInterval).Tag(tag).Do(service.run)
	if err != nil {
		return err
	}
	return nil
}

func newScaleUpService(dockerClient scaleUpDockerClient, logger *log.Entry, lock *sync.Mutex, scaleUpConfig *ScaleUpConfig, deployUtils DeployUtilsInterface, autoscalingUtils AutoscalingUtilsInterface) scaleUpService {
	cooldown := time.Duration(0)
	scaleUpErroredValue := false
	scaleUpErroredLock := sync.Mutex{}
	service := scaleUpService{
		dockerClient:       dockerClient,
		rootLogger:         logger,
		logger:             logger,
		cooldown:           &cooldown,
		autoscalingLock:    lock,
		scaleUpConfig:      scaleUpConfig,
		deployUtils:        deployUtils,
		scaleUpErrored:     &scaleUpErroredValue,
		scaleUpErroredLock: &scaleUpErroredLock,
		autoscalingUtils:   autoscalingUtils,
	}
	return service
}

type scaleUpService struct {
	dockerClient       scaleUpDockerClient
	logger             *log.Entry
	rootLogger         *log.Entry
	cooldown           *time.Duration
	autoscalingLock    *sync.Mutex
	scaleUpConfig      *ScaleUpConfig
	deployUtils        DeployUtilsInterface
	scaleUpErrored     *bool
	scaleUpErroredLock *sync.Mutex
	autoscalingUtils   AutoscalingUtilsInterface
}

func (s *scaleUpService) run() {
	s.autoscalingLock.Lock()
	defer s.autoscalingLock.Unlock()
	jobId := RandomString(6)
	s.logger = s.rootLogger.WithFields(log.Fields{"jobId": jobId})
	s.logger.Tracef("Start scale up #%s", jobId)
	defer s.logger.Tracef("End scale up #%s", jobId)
	if *s.cooldown > 0 {
		s.logger.Infof("Cooldown : waiting %s before proceeding", s.cooldown.String())
		time.Sleep(*s.cooldown)
		*s.cooldown = 0
	}
	neededGpus, allGpus, err := s.checkPendingServices()
	s.logger.Debugf("Retrieved GPU services in Swarm (pending/all) : %v / %v", neededGpus, allGpus)
	if err != nil {
		s.logger.Errorf("Error computing needed Gpus : %s", err)
	}
	existingGpus, err := retrieveExistingGpus()
	s.logger.Debugf("Retrieved GPU nodes in Terraform : %v", existingGpus)
	if err != nil {
		s.logger.Errorf("Error retrieving existing nodes : %s", err)
	}
	err = s.createNodes(allGpus, existingGpus)
	if err != nil {
		s.logger.Errorf("Error creating nodes : %s", err)
	}
}

func (s *scaleUpService) checkPendingServices() (map[string]int64, map[string]int64, error) {
	neededGpus := make(map[string]int64)
	allGpus := make(map[string]int64)
	ctx := context.TODO()
	tasks, err := s.dockerClient.TaskList(ctx, types.TaskListOptions{
		Filters: filters.NewArgs(filters.KeyValuePair{
			"desired-state",
			"running",
		}),
	})
	if err != nil {
		return neededGpus, allGpus, err
	}
	for _, listedTask := range tasks {
		gpuCount := int64(0)
		if listedTask.Spec.Resources == nil || listedTask.Spec.Resources.Reservations == nil || listedTask.Spec.Resources.Reservations.GenericResources == nil {
			s.logger.Debugf("Skipping task %s for service %s because it has no resource reservations", listedTask.ID, listedTask.ServiceID)
			continue
		}
		for _, resource := range listedTask.Spec.Resources.Reservations.GenericResources {
			if resource.DiscreteResourceSpec.Kind == "gpu" {
				gpuCount += resource.DiscreteResourceSpec.Value
			}
		}
		owner := extractGpuOwnerFromConstraints(listedTask.Spec.Placement.Constraints)
		if owner == "" || gpuCount == 0 {
			continue
		}
		allGpus[owner] += gpuCount
		if listedTask.Status.State == "pending" && strings.Contains(listedTask.Status.Err, "no suitable node") {
			neededGpus[owner] += gpuCount
		}
	}
	return neededGpus, allGpus, nil
}

func retrieveExistingGpus() (map[string]int64, error) {
	state := &TerraformState{}
	err := getJson(fmt.Sprintf("%s/%s", c.AutoscalingStateBaseUrl, autoscalingStateName), state, true)
	if err != nil {
		return nil, err
	}
	gpus := make(map[string]int64)
	for _, resource := range state.Resources {
		if resource.Type == "openstack_compute_instance_v2" {
			for _, instance := range resource.Instances {
				owner := instance.Attributes["metadata"].AttributeMap["owner"].AttributeString
				gpus[owner] += 1
			}
		}
	}
	return gpus, nil
}

func (s *scaleUpService) createNodes(allGpus map[string]int64, existingGpus map[string]int64) error {
	missingGpus := make(map[string]int64)
	s.scaleUpConfig.Lock.Lock()
	for owner, ownerConfig := range s.scaleUpConfig.ScaleUpOwners {
		maxToAdd := ownerConfig.MaxNodesCount - existingGpus[owner]
		desired := allGpus[owner] - existingGpus[owner] + ownerConfig.MinIdleNodesCount - ownerConfig.ManualNodesCount
		if desired > maxToAdd {
			s.logger.Warnf("Too many nodes desired (%d > %d), %d existing for owner '%s'", desired, maxToAdd, existingGpus[owner], owner)
		}
		missingGpus[owner] = int64(math.Max(math.Min(float64(maxToAdd), float64(desired)), float64(0)))
		s.logger.Infof("Adding %d nodes for owner '%s'", missingGpus[owner], owner)
	}
	terraformConfig, err := s.createTerraformConfig(missingGpus)
	s.scaleUpConfig.Lock.Unlock()
	if err != nil {
		return err
	}
	if len(terraformConfig.NamedWorkers) == 0 {
		return nil
	}
	if allMapValuesZero(missingGpus) && !*s.scaleUpErrored {
		s.logger.Debugf("No missing GPUs, skipping deploy")
		return nil
	}
	callback := s.createScaleUpCallback()
	err = s.deployUtils.launchTerraform(terraformConfig, callback, false)
	if err != nil {
		return err
	}
	return nil
}

func (s *scaleUpService) createTerraformConfig(missingGpus map[string]int64) (*TerraformConfig, error) {
	terraformConfig, err := s.autoscalingUtils.buildConfigFromExistingInfra()
	s.logger.Debugf("Got Terraform config : %v", terraformConfig)
	if err != nil {
		return nil, err
	}
	for owner, gpuCount := range missingGpus {
		if _, exist := s.scaleUpConfig.ScaleUpOwners[owner]; !exist {
			s.logger.Errorf("Missing config for owner '%s'", owner)
			continue
		}
		regions := s.scaleUpConfig.ScaleUpOwners[owner].Regions
		for i := int64(0); i < gpuCount; i++ {
			region := &regions[rand.Intn(len(regions))]
			s.logger.Debugf("Choosing %s among %d regions for owner %s", region.Region, len(regions), owner)
			name := fmt.Sprintf(
				"%s-%s-%s-%s-%s",
				owner,
				c.Environment,
				region.Region,
				s.scaleUpConfig.ScaleUpOwners[owner].InstanceType,
				RandomString(5),
			)
			terraformConfig.NamedWorkers[name] = TerraformNamedWorker{
				InstanceImageId: region.ImageId,
				InstanceType:    s.scaleUpConfig.ScaleUpOwners[owner].InstanceType,
				Name:            name,
				Region:          region.Region,
				Owner:           owner,
			}
		}
	}
	return terraformConfig, nil
}

func (s *scaleUpService) createScaleUpCallback() deployCallback {
	var lastLogs string
	return func(status *DeployStatus) error {
		s.scaleUpErroredLock.Lock()
		*s.scaleUpErrored = false
		s.scaleUpErroredLock.Unlock()
		if status.Status == pb.DeployResponse_RUNNING {
			s.logger.Debug("Scale up running")
		}
		if status.Status == pb.DeployResponse_DONE {
			s.logger.Infof("Scale up finished with success")
		}
		if status.Logs != "" {
			s.logger.Tracef("Scale up logs : %s", status.Logs)
			lastLogs = status.Logs
		}
		if status.Status == pb.DeployResponse_ERROR {
			message := fmt.Sprintf("Scale up finished with error : %s\nLogs: %s", status.Error, lastLogs)
			s.logger.Errorf(message)
			_ = sendEmail(fmt.Sprintf(
				"[Scale up error] created at %s",
				time.Now().Format("02/01/2006 03:04"),
			), message)
			*s.cooldown, _ = time.ParseDuration(c.ScaleUpCooldown)
			s.scaleUpErroredLock.Lock()
			*s.scaleUpErrored = true
			s.scaleUpErroredLock.Unlock()
		}
		return nil
	}
}

func extractGpuOwnerFromConstraints(constraints []string) string {
	ownerLabelRegex := regexp.MustCompile("^node\\.labels\\.owner==(.*)$")
	for _, constraint := range constraints {
		if matches := ownerLabelRegex.FindStringSubmatch(constraint); len(matches) > 0 {
			return matches[1]
		}
	}
	return ""
}
