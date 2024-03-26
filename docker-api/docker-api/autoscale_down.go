package main

import (
	"context"
	"fmt"
	"os"
	"strings"
	"sync"
	"time"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
)

func addScaleDownCron(s *gocron.Scheduler, dockerClient *client.Client, lock *sync.Mutex, scaleUpConfig *ScaleUpConfig) error {
	tag := "scaleDown"
	logger := log.WithFields(log.Fields{"service": tag})
	service := newScaleDownService(dockerClient, logger, lock, scaleUpConfig, NewDeployUtils(dockerClient), &AutoscalingUtils{})
	_ = s.RemoveByTag(tag)
	_, err := s.Every(c.ScaleDownInterval).Tag(tag).Do(service.run)
	if err != nil {
		return err
	}
	return nil
}

func newScaleDownService(dockerClient scaleDownDockerClient, logger *log.Entry, lock *sync.Mutex, scaleUpConfig *ScaleUpConfig, deployUtils DeployUtilsInterface, autoscalingUtils AutoscalingUtilsInterface) scaleDownService {
	return scaleDownService{
		dockerClient:     dockerClient,
		rootLogger:       logger,
		logger:           logger,
		autoscalingLock:  lock,
		scaleUpConfig:    scaleUpConfig,
		deployUtils:      deployUtils,
		autoscalingUtils: autoscalingUtils,
		swarmNodes:       map[string]*NodeSummary{},
	}
}

type scaleDownDockerClient interface {
	deployUtilsDockerClient
	NodeList(ctx context.Context, options types.NodeListOptions) ([]swarm.Node, error)
	NodeRemove(ctx context.Context, nodeID string, options types.NodeRemoveOptions) error
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
}

type scaleDownService struct {
	dockerClient     scaleDownDockerClient
	logger           *log.Entry
	rootLogger       *log.Entry
	autoscalingLock  *sync.Mutex
	swarmNodes       map[string]*NodeSummary
	scaleUpConfig    *ScaleUpConfig
	deployUtils      DeployUtilsInterface
	autoscalingUtils AutoscalingUtilsInterface
}

func (s *scaleDownService) run() {
	s.autoscalingLock.Lock()
	defer s.autoscalingLock.Unlock()

	if _, err := os.Stat(c.StopScaleDownFilePath); err == nil {
		s.logger.Info("StopScaleDown file detected. Exiting the scale-down process.")
		return
	}

	jobId := RandomString(6)
	s.logger = s.rootLogger.WithFields(log.Fields{"jobId": jobId})
	s.logger.Tracef("Start scale down #%s", jobId)
	defer s.logger.Tracef("End scale down #%s", jobId)
	s.swarmNodes = make(map[string]*NodeSummary)
	var err error
	terraformConfig, err := s.autoscalingUtils.buildConfigFromExistingInfra()
	s.logger.Debugf("Got Terraform config : %v", terraformConfig)
	if err != nil {
		s.logger.Errorf("Unable to retrieve infra from Terraform : %v", err)
		return
	}
	if len(terraformConfig.NamedWorkers) == 0 {
		s.logger.Infof("No node in terraform")
		return
	}
	nodesToDelete, err := s.findNodesToDelete(terraformConfig)
	s.logger.Infof("Found nodes to delete : %v", nodesToDelete)
	if err != nil {
		s.logger.Errorf("Unable to retrieve nodes status from Swarm : %v", err)
		return
	}
	if len(nodesToDelete) == 0 {
		return
	}
	err = s.deleteNodes(terraformConfig, nodesToDelete)
	if err != nil {
		s.logger.Errorf("Unable to delete nodes : %v", err)
	}
}

func (s *scaleDownService) deleteNodes(terraformConfig *TerraformConfig, nodesToDelete []string) error {
	ctx := context.TODO()
	for _, node := range nodesToDelete {
		delete(terraformConfig.NamedWorkers, node)
	}
	callback := s.createScaleDownCallback()
	err := s.deployUtils.launchTerraform(terraformConfig, callback, false)
	if err != nil {
		return err
	}
	for _, node := range nodesToDelete {
		err := s.dockerClient.NodeRemove(ctx, strings.ToLower(node), types.NodeRemoveOptions{})
		if err != nil {
			return err
		}
	}
	return nil
}

func (s *scaleDownService) createScaleDownCallback() deployCallback {
	var lastLogs string
	return func(status *DeployStatus) error {
		if status.Status == pb.DeployResponse_RUNNING {
			s.logger.Debug("Scale down running")
		}
		if status.Status == pb.DeployResponse_DONE {
			s.logger.Infof("Scale down finished with success")
		}
		if status.Logs != "" {
			s.logger.Tracef("Scale down logs : %s", status.Logs)
			lastLogs = status.Logs
		}
		if status.Status == pb.DeployResponse_ERROR {
			message := fmt.Sprintf("Scale down finished with error : %s\nLogs: %s", status.Error, lastLogs)
			s.logger.Errorf(message)
			_ = sendEmail(fmt.Sprintf(
				"[Scale down error] created at %s",
				time.Now().Format("02/01/2006 03:04"),
			), message)
		}
		return nil
	}
}
func (s *scaleDownService) classifyEmptyNodesInTerraform(terraformConfig *TerraformConfig) (map[string][]string, map[string][]string, map[string][]string) {
	emptyNodesInTerraformByOwner := make(map[string][]string)
	notEmptyNodesInTerraformByOwner := make(map[string][]string)
	allNodesInTerraformByOwner := make(map[string][]string)

	for nodeName, workerConfig := range terraformConfig.NamedWorkers {
		hasTasksRunning, err := s.hasTasksRunning(nodeName)
		if err != nil {
			s.logger.Errorf("Unable to retrieve tasks for node %s : %v", nodeName, err)
			continue
		}
		if hasTasksRunning {
			notEmptyNodesInTerraformByOwner[workerConfig.Owner] = append(notEmptyNodesInTerraformByOwner[workerConfig.Owner], nodeName)
		} else {
			emptyNodesInTerraformByOwner[workerConfig.Owner] = append(emptyNodesInTerraformByOwner[workerConfig.Owner], nodeName)
		}
		allNodesInTerraformByOwner[workerConfig.Owner] = append(allNodesInTerraformByOwner[workerConfig.Owner], nodeName)
	}

	return emptyNodesInTerraformByOwner, notEmptyNodesInTerraformByOwner, allNodesInTerraformByOwner
}

func (s *scaleDownService) findNodesToDelete(terraformConfig *TerraformConfig) ([]string, error) {
	var nodesToDelete []string

	err := s.listSwarmNodes()
	if err != nil {
		return make([]string, 0), err
	}

	emptyNodesInTerraformByOwner, notEmptyNodesInTerraformByOwner, allNodesInTerraformByOwner := s.classifyEmptyNodesInTerraform(terraformConfig)

	s.scaleUpConfig.Lock.Lock()
	for owner, _ := range allNodesInTerraformByOwner {
		emptyNodesInTerraform := emptyNodesInTerraformByOwner[owner]
		notEmptyNodesInTerraform := notEmptyNodesInTerraformByOwner[owner]
		idleManualNodesCount, err := s.countEmptyManualNodes(owner, allNodesInTerraformByOwner[owner])
		if err != nil {
			s.logger.Warnf("Unable to retrieve idle manual nodes : %v", err)
		}
		toDeleteCount := int64(len(emptyNodesInTerraform)) + idleManualNodesCount - s.scaleUpConfig.ScaleUpOwners[owner].MinIdleNodesCount
		s.logger.Debugf("Counted %d nodes to delete (%d + %d - %d)", toDeleteCount, int64(len(emptyNodesInTerraform)), idleManualNodesCount, s.scaleUpConfig.ScaleUpOwners[owner].MinIdleNodesCount)
		for toDeleteCount > 0 {
			lastNode := ""
			if len(emptyNodesInTerraform) > 0 {
				toDeleteCount -= 1
				lastNode = emptyNodesInTerraform[len(emptyNodesInTerraform)-1]
				emptyNodesInTerraform = emptyNodesInTerraform[:len(emptyNodesInTerraform)-1]
			} else if len(notEmptyNodesInTerraform) > 0 {
				toDeleteCount -= 1
				if c.ScaleDownRemoveNonEmpty {
					lastNode = notEmptyNodesInTerraform[len(notEmptyNodesInTerraform)-1]
					notEmptyNodesInTerraform = notEmptyNodesInTerraform[:len(notEmptyNodesInTerraform)-1]
					s.logger.Warnf(
						"No empty node in terraform for owner %s, destroying node %s with a service scheduled",
						owner,
						lastNode,
					)
				} else {
					s.logger.Infof(
						"No empty node in terraform for owner %s, avoiding to destroy node %s with a service scheduled",
						owner,
						notEmptyNodesInTerraform[len(notEmptyNodesInTerraform)-1],
					)
				}
			} else {
				toDeleteCount -= 1
				s.logger.Infof(
					"No empty node nor non-empty node in terraform for owner %s, too many idle manual nodes",
					owner,
				)
			}
			if lastNode != "" {
				nodesToDelete = append(nodesToDelete, lastNode)
				s.logger.Infof("Scheduling %s (%s) for removal", lastNode, s.swarmNodes[strings.ToLower(lastNode)].ID)
			}
		}
		// TODO Remove node not empty if count of existing nodes > c.MaxNodes
	}
	s.scaleUpConfig.Lock.Unlock()
	return nodesToDelete, nil
}

func (s *scaleDownService) hasTasksRunning(nodeName string) (bool, error) {
	lowerCasedNodeName := strings.ToLower(nodeName)
	ctx := context.TODO()
	if _, exist := s.swarmNodes[lowerCasedNodeName]; !exist {
		err := s.listSwarmNodes()
		if err != nil {
			return true, err
		}
	}
	node := s.swarmNodes[lowerCasedNodeName]
	if node == nil {
		return true, fmt.Errorf("node %s is not registered in Swarm", lowerCasedNodeName)
	}
	tasks, err := s.dockerClient.TaskList(ctx, types.TaskListOptions{
		Filters: filters.NewArgs(filters.KeyValuePair{
			Key:   "node",
			Value: node.ID,
		}),
	})
	if err != nil {
		return true, err
	}
	for _, task := range tasks {
		switch task.Status.State {
		case
			swarm.TaskStateNew,
			swarm.TaskStateAllocated,
			swarm.TaskStatePending,
			swarm.TaskStateAssigned,
			swarm.TaskStateAccepted,
			swarm.TaskStatePreparing,
			swarm.TaskStateReady,
			swarm.TaskStateStarting,
			swarm.TaskStateRunning:
			return true, nil
		}
	}
	return false, nil
}

type NodeSummary struct {
	ID    string
	Owner string
	Ready bool
}

func (s *scaleDownService) listSwarmNodes() error {
	ctx := context.TODO()
	nodes, err := s.dockerClient.NodeList(ctx, types.NodeListOptions{})
	if err != nil {
		return err
	}
	for _, node := range nodes {
		s.swarmNodes[strings.ToLower(node.Description.Hostname)] = &NodeSummary{
			ID:    node.ID,
			Owner: node.Spec.Labels["owner"],
			Ready: node.Status.State == swarm.NodeStateReady,
		}
	}
	s.logger.Debugf("Found %d nodes %v", len(s.swarmNodes), s.swarmNodes)
	return nil
}

func (s *scaleDownService) countEmptyManualNodes(owner string, terraformNodes []string) (int64, error) {
	idleManualNodesCount := int64(0)
	for hostname, node := range s.swarmNodes {
		if node.Owner != owner {
			continue
		}
		if findStringInListCaseInsensitive(hostname, terraformNodes) {
			continue
		}
		hasTasksRunning, err := s.hasTasksRunning(hostname)
		if err != nil {
			return 0, err
		}
		if hasTasksRunning {
			continue
		}
		if !node.Ready {
			continue
		}
		idleManualNodesCount = idleManualNodesCount + 1
	}
	return idleManualNodesCount, nil
}
