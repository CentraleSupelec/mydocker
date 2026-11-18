package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"strings"
	"time"

	log "github.com/sirupsen/logrus"
)

const (
	autoscalingPullInitialBackoff = 5 * time.Second
	autoscalingPullMaxBackoff     = 5 * time.Minute
)

type backAutoscalingConfig struct {
	// A pointer to tell an explicit empty list ({}) from a missing or null one.
	Owners *map[string]backOwnerConfig `json:"owners"`
}

type backOwnerConfig struct {
	MinIdleNodesCount int64                       `json:"minIdleNodesCount"`
	MaxNodesCount     int64                       `json:"maxNodesCount"`
	ManualNodesCount  int64                       `json:"manualNodesCount"`
	InstancesRegions  []backInstanceRegionsConfig `json:"instancesRegions"`
}

type backInstanceRegionsConfig struct {
	InstanceType string              `json:"instanceType"`
	Regions      []backScalingRegion `json:"regions"`
}

type backScalingRegion struct {
	ImageId string `json:"imageId"`
	Region  string `json:"region"`
}

// fetchAutoscalingConfig asks the back for the complete list of the autoscaled types of a
// cluster (the owners of InitAutoscalingRequest).
func fetchAutoscalingConfig(ctx context.Context, httpClient *http.Client, backUrl string, clusterName string, token string) (map[string]scaleUpOwner, error) {
	requestUrl := fmt.Sprintf(
		"%s/internal/autoscaling/%s/compute-types",
		strings.TrimRight(backUrl, "/"),
		url.PathEscape(clusterName),
	)
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, requestUrl, nil)
	if err != nil {
		return nil, err
	}
	request.Header.Set("X-Autoscaling-Token", token)
	response, err := httpClient.Do(request)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("the back answered %s for %s", response.Status, requestUrl)
	}
	body, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	config := backAutoscalingConfig{}
	err = json.Unmarshal(body, &config)
	if err != nil {
		return nil, fmt.Errorf("unreadable answer of the back: %w", err)
	}
	// An error must never become an empty list: an empty list makes every VM look orphaned.
	if config.Owners == nil || *config.Owners == nil {
		return nil, errors.New("the answer of the back holds no owners list")
	}

	owners := make(map[string]scaleUpOwner, len(*config.Owners))
	for owner, ownerConfig := range *config.Owners {
		instancesRegions := []InstanceRegions{}
		for _, instanceRegions := range ownerConfig.InstancesRegions {
			regions := []ScalingRegion{}
			for _, region := range instanceRegions.Regions {
				regions = append(regions, ScalingRegion{
					ImageId: region.ImageId,
					Region:  region.Region,
				})
			}
			instancesRegions = append(instancesRegions, InstanceRegions{
				InstanceType: instanceRegions.InstanceType,
				Regions:      regions,
			})
		}
		owners[owner] = scaleUpOwner{
			MinIdleNodesCount: ownerConfig.MinIdleNodesCount,
			MaxNodesCount:     ownerConfig.MaxNodesCount,
			ManualNodesCount:  ownerConfig.ManualNodesCount,
			InstancesRegions:  instancesRegions,
		}
	}
	return owners, nil
}

type fetchAutoscalingConfigFunc func(ctx context.Context) (map[string]scaleUpOwner, error)

// applyAutoscalingConfigFunc reports whether the list was applied; false without an error means
// a complete list was already loaded.
type applyAutoscalingConfigFunc func(owners map[string]scaleUpOwner) (bool, error)

type sleepFunc func(ctx context.Context, d time.Duration) error

// autoscalingConfigLoaded reports whether a complete list is already loaded.
func autoscalingConfigLoaded() bool {
	scaleUpConfig.Lock.Lock()
	defer scaleUpConfig.Lock.Unlock()
	return scaleUpConfig.Loaded
}

// pullAutoscalingConfigUntilLoaded fetches and applies the list, retrying with a growing delay
// until a complete list is loaded or ctx ends.
func pullAutoscalingConfigUntilLoaded(ctx context.Context, fetch fetchAutoscalingConfigFunc, apply applyAutoscalingConfigFunc, sleep sleepFunc, initialBackoff time.Duration, maxBackoff time.Duration) {
	backoff := initialBackoff
	for {
		// A push of the back may have loaded the list while every fetch fails (a back without
		// the endpoint): the pull stops without fetching.
		if autoscalingConfigLoaded() {
			log.Infof("Autoscaling config already loaded by a push of the back, the pull stops")
			return
		}
		owners, err := fetch(ctx)
		if err == nil {
			var applied bool
			applied, err = apply(owners)
			if err == nil {
				if applied {
					log.Infof("Autoscaling config loaded from the back: %d owner(s)", len(owners))
				} else {
					log.Infof("Autoscaling config already loaded by a push of the back, the pulled one is ignored")
				}
				return
			}
		}
		log.Errorf("Cannot load the autoscaling config from the back, retrying in %s: %v", backoff, err)
		if sleepErr := sleep(ctx, backoff); sleepErr != nil {
			log.Warnf("Autoscaling config pull stopped: %v", sleepErr)
			return
		}
		backoff *= 2
		if backoff > maxBackoff {
			backoff = maxBackoff
		}
	}
}

// sleepContext waits for d, or less when ctx ends.
func sleepContext(ctx context.Context, d time.Duration) error {
	timer := time.NewTimer(d)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return ctx.Err()
	case <-timer.C:
		return nil
	}
}

// startAutoscalingPull loads the complete list of types from the back in the background, so that
// the gRPC server serves while the back is down (a push of the back satisfies the pull).
func startAutoscalingPull(s *server) {
	if c.BackUrl == "" {
		log.Warn("BackUrl is not set: the autoscaling config is not pulled at startup")
		return
	}
	fetch := func(ctx context.Context) (map[string]scaleUpOwner, error) {
		return fetchAutoscalingConfig(ctx, httpClient, c.BackUrl, c.BackClusterName, c.BackToken)
	}
	apply := func(owners map[string]scaleUpOwner) (bool, error) {
		return s.applyAutoscalingOwners(owners, true)
	}
	go pullAutoscalingConfigUntilLoaded(
		context.Background(),
		fetch,
		apply,
		sleepContext,
		autoscalingPullInitialBackoff,
		autoscalingPullMaxBackoff,
	)
}

// applyAutoscalingOwners replaces the whole list of owners at once and starts the scale-up and
// scale-down crons. With onlyIfNotLoaded it does nothing when a list is already loaded.
func (s *server) applyAutoscalingOwners(owners map[string]scaleUpOwner, onlyIfNotLoaded bool) (bool, error) {
	newOwners := make(map[string]scaleUpOwner, len(owners))
	for owner, ownerConfig := range owners {
		newOwners[owner] = ownerConfig
	}

	// Check and set in one step: a push and a pull answer can arrive together.
	scaleUpConfig.Lock.Lock()
	if onlyIfNotLoaded && scaleUpConfig.Loaded {
		scaleUpConfig.Lock.Unlock()
		return false, nil
	}
	scaleUpConfig.ScaleUpOwners = newOwners
	scaleUpConfig.Loaded = true
	scaleUpConfig.Lock.Unlock()

	err := addScaleUpCron(s.cronScheduler, s.dockerClient, &autoscalingLock, &scaleUpConfig)
	if err == nil {
		err = addScaleDownCron(s.cronScheduler, s.dockerClient, &autoscalingLock, &scaleUpConfig)
	}
	if err != nil {
		// Not loaded while the crons do not run, so that the pull retries.
		scaleUpConfig.Lock.Lock()
		scaleUpConfig.Loaded = false
		scaleUpConfig.Lock.Unlock()
		return false, err
	}
	return true, nil
}
