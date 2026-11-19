package main

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	tasksTypes "github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/swarm"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// resetAutoscalingState gives a test an empty, not loaded list and long cron intervals, and puts
// the globals back afterwards.
func resetAutoscalingState(t *testing.T) {
	t.Helper()
	oldOwners, oldLoaded := scaleUpConfig.ScaleUpOwners, scaleUpConfig.Loaded
	oldUp, oldDown := c.ScaleUpInterval, c.ScaleDownInterval
	scaleUpConfig.ScaleUpOwners = map[string]scaleUpOwner{}
	scaleUpConfig.Loaded = false
	c.ScaleUpInterval, c.ScaleDownInterval = "1h", "1h"
	t.Cleanup(func() {
		scaleUpConfig.ScaleUpOwners, scaleUpConfig.Loaded = oldOwners, oldLoaded
		c.ScaleUpInterval, c.ScaleDownInterval = oldUp, oldDown
	})
}

func ownerNames(owners map[string]scaleUpOwner) []string {
	names := []string{}
	for name := range owners {
		names = append(names, name)
	}
	return names
}

// --- the pull: one request to the back, answer read as a complete list or as an error ---

func TestFetchAutoscalingConfigAsksTheBackForTheClusterWithTheToken(t *testing.T) {
	var gotMethod, gotPath, gotToken string
	back := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotMethod, gotPath, gotToken = r.Method, r.URL.EscapedPath(), r.Header.Get("X-Autoscaling-Token")
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"owners":{` +
			`"gpu-a":{"minIdleNodesCount":1,"maxNodesCount":3,"manualNodesCount":2,` +
			`"instancesRegions":[{"instanceType":"b3-8","regions":[` +
			`{"imageId":"img-1","region":"GRA11"},{"imageId":"img-2","region":"SBG5"}]}]},` +
			`"cpu":{"minIdleNodesCount":0,"maxNodesCount":5}}}`))
	}))
	defer back.Close()

	// A trailing slash on the configured address makes no difference.
	owners, err := fetchAutoscalingConfig(context.Background(), back.Client(), back.URL+"/api/", "cluster a", "s3cret")

	require.NoError(t, err)
	assert.Equal(t, http.MethodGet, gotMethod)
	assert.Equal(t, "/api/internal/autoscaling/cluster%20a/compute-types", gotPath)
	assert.Equal(t, "s3cret", gotToken)
	require.ElementsMatch(t, []string{"gpu-a", "cpu"}, ownerNames(owners))
	gpu := owners["gpu-a"]
	assert.Equal(t, int64(1), gpu.MinIdleNodesCount)
	assert.Equal(t, int64(3), gpu.MaxNodesCount)
	assert.Equal(t, int64(2), gpu.ManualNodesCount)
	assert.Equal(t, []InstanceRegions{{
		InstanceType: "b3-8",
		Regions: []ScalingRegion{
			{ImageId: "img-1", Region: "GRA11"},
			{ImageId: "img-2", Region: "SBG5"},
		},
	}}, gpu.InstancesRegions)
	cpu := owners["cpu"]
	assert.Equal(t, int64(0), cpu.MinIdleNodesCount)
	assert.Equal(t, int64(5), cpu.MaxNodesCount)
	assert.Empty(t, cpu.InstancesRegions)
}

// Anything but a clean answer holding an explicit list is an error, never an empty list:
// an empty list would make every VM look orphaned.
func TestFetchAutoscalingConfigAnythingButACompleteListIsAnError(t *testing.T) {
	cases := []struct {
		name   string
		status int
		body   string
	}{
		{"unknown cluster", http.StatusNotFound, `{"owners":{}}`},
		{"wrong token", http.StatusForbidden, ``},
		{"server error", http.StatusInternalServerError, `{"owners":{"a":{"maxNodesCount":1}}}`},
		{"not json", http.StatusOK, `<html>proxy error</html>`},
		{"truncated", http.StatusOK, `{"owners":{"a":{"maxNodesCount":3,`},
		{"no owners key", http.StatusOK, `{}`},
		{"null owners", http.StatusOK, `{"owners":null}`},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			back := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(tc.status)
				_, _ = w.Write([]byte(tc.body))
			}))
			defer back.Close()

			owners, err := fetchAutoscalingConfig(context.Background(), back.Client(), back.URL, "a", "tok")

			assert.Error(t, err)
			assert.Nil(t, owners)
		})
	}

	t.Run("back unreachable", func(t *testing.T) {
		back := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {}))
		client, url := back.Client(), back.URL
		back.Close()

		owners, err := fetchAutoscalingConfig(context.Background(), client, url, "a", "tok")

		assert.Error(t, err)
		assert.Nil(t, owners)
	})
}

// A cluster with no autoscaled type is a complete list of zero types, not a failure.
func TestFetchAutoscalingConfigAnExplicitEmptyListIsValid(t *testing.T) {
	back := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte(`{"owners":{}}`))
	}))
	defer back.Close()

	owners, err := fetchAutoscalingConfig(context.Background(), back.Client(), back.URL, "a", "tok")

	require.NoError(t, err)
	require.NotNil(t, owners)
	assert.Empty(t, owners)
}

// --- the pull loop: back unreachable at startup ---

func TestPullRetriesWithGrowingBackoffUntilTheBackAnswersThenAppliesOnce(t *testing.T) {
	want := map[string]scaleUpOwner{"a": {MaxNodesCount: 2}}
	fetches := 0
	fetch := func(ctx context.Context) (map[string]scaleUpOwner, error) {
		fetches++
		if fetches <= 4 {
			return nil, errors.New("back unreachable")
		}
		return want, nil
	}
	var applied []map[string]scaleUpOwner
	apply := func(owners map[string]scaleUpOwner) (bool, error) {
		applied = append(applied, owners)
		return true, nil
	}
	var sleeps []time.Duration
	sleep := func(ctx context.Context, d time.Duration) error {
		assert.Empty(t, applied, "nothing is applied while the back does not answer")
		sleeps = append(sleeps, d)
		return nil
	}

	pullAutoscalingConfigUntilLoaded(context.Background(), fetch, apply, sleep, time.Second, 3*time.Second)

	assert.Equal(t, 5, fetches)
	assert.Equal(t, []time.Duration{time.Second, 2 * time.Second, 3 * time.Second, 3 * time.Second}, sleeps)
	require.Len(t, applied, 1)
	assert.Equal(t, want, applied[0])
}

func TestPullTreatsAFailedApplyLikeAFailedFetch(t *testing.T) {
	fetches := 0
	fetch := func(ctx context.Context) (map[string]scaleUpOwner, error) {
		fetches++
		return map[string]scaleUpOwner{"a": {MaxNodesCount: 1}}, nil
	}
	applies := 0
	apply := func(owners map[string]scaleUpOwner) (bool, error) {
		applies++
		if applies == 1 {
			return false, errors.New("cannot start the crons")
		}
		return true, nil
	}
	var sleeps []time.Duration
	sleep := func(ctx context.Context, d time.Duration) error {
		sleeps = append(sleeps, d)
		return nil
	}

	pullAutoscalingConfigUntilLoaded(context.Background(), fetch, apply, sleep, time.Second, time.Minute)

	assert.Equal(t, 2, fetches)
	assert.Equal(t, 2, applies)
	assert.Equal(t, []time.Duration{time.Second}, sleeps)
}

// A push of the back that landed first is complete too: the pull ends and does not retry.
func TestPullStopsWhenAPushAlreadyLoadedTheList(t *testing.T) {
	fetches := 0
	fetch := func(ctx context.Context) (map[string]scaleUpOwner, error) {
		fetches++
		return map[string]scaleUpOwner{"a": {MaxNodesCount: 1}}, nil
	}
	apply := func(owners map[string]scaleUpOwner) (bool, error) { return false, nil }
	var sleeps []time.Duration
	sleep := func(ctx context.Context, d time.Duration) error {
		sleeps = append(sleeps, d)
		return nil
	}

	pullAutoscalingConfigUntilLoaded(context.Background(), fetch, apply, sleep, time.Second, time.Minute)

	assert.Equal(t, 1, fetches)
	assert.Empty(t, sleeps)
}

// A push of the back that lands while the back cannot answer the pull (an older back without the
// endpoint) ends the pull: it fetches no more and logs no more errors.
func TestPullStopsAfterAFailedFetchOnceAPushLoadedTheList(t *testing.T) {
	resetAutoscalingState(t)
	fetches := 0
	fetch := func(ctx context.Context) (map[string]scaleUpOwner, error) {
		fetches++
		return nil, errors.New("401 from a back without the endpoint")
	}
	apply := func(owners map[string]scaleUpOwner) (bool, error) {
		t.Fatal("nothing to apply: every fetch fails")
		return false, nil
	}
	sleeps := 0
	sleep := func(ctx context.Context, d time.Duration) error {
		sleeps++
		if sleeps == 1 {
			// the push of the back lands during the first backoff
			scaleUpConfig.Lock.Lock()
			scaleUpConfig.Loaded = true
			scaleUpConfig.Lock.Unlock()
		}
		if sleeps >= 3 {
			return errors.New("test bound: the pull is still retrying")
		}
		return nil
	}

	pullAutoscalingConfigUntilLoaded(context.Background(), fetch, apply, sleep, time.Second, time.Minute)

	assert.Equal(t, 1, fetches, "no fetch after a push loaded the list")
	assert.Equal(t, 1, sleeps)
}

// A push that landed before the pull started: the pull fetches nothing.
func TestPullFetchesNothingWhenAPushLoadedTheListBeforeItStarts(t *testing.T) {
	resetAutoscalingState(t)
	scaleUpConfig.Loaded = true
	fetches := 0
	fetch := func(ctx context.Context) (map[string]scaleUpOwner, error) {
		fetches++
		return nil, errors.New("back unreachable")
	}
	apply := func(owners map[string]scaleUpOwner) (bool, error) { return false, nil }
	sleeps := 0
	sleep := func(ctx context.Context, d time.Duration) error {
		sleeps++
		if sleeps >= 3 {
			return errors.New("test bound: the pull is still retrying")
		}
		return nil
	}

	pullAutoscalingConfigUntilLoaded(context.Background(), fetch, apply, sleep, time.Second, time.Minute)

	assert.Equal(t, 0, fetches)
	assert.Equal(t, 0, sleeps)
}

// --- applying a list, from the pull or from a push ---

func TestApplyOwnersReplacesTheWholeListAtOnceAndStartsBothCrons(t *testing.T) {
	resetAutoscalingState(t)
	s := &server{cronScheduler: gocron.NewScheduler(time.UTC)}

	applied, err := s.applyAutoscalingOwners(map[string]scaleUpOwner{
		"a": {MaxNodesCount: 1},
		"b": {MaxNodesCount: 2},
	}, false)

	require.NoError(t, err)
	assert.True(t, applied)
	assert.ElementsMatch(t, []string{"a", "b"}, ownerNames(scaleUpConfig.ScaleUpOwners))
	assert.True(t, scaleUpConfig.Loaded)
	for _, tag := range []string{"scaleUp", "scaleDown"} {
		jobs, findErr := s.cronScheduler.FindJobsByTag(tag)
		require.NoError(t, findErr, tag)
		assert.Len(t, jobs, 1, tag)
	}

	// A type at max 0 or deleted is no longer in the next list: it leaves the config with it.
	applied, err = s.applyAutoscalingOwners(map[string]scaleUpOwner{"a": {MaxNodesCount: 5}}, false)

	require.NoError(t, err)
	assert.True(t, applied)
	assert.Equal(t, []string{"a"}, ownerNames(scaleUpConfig.ScaleUpOwners))
	assert.Equal(t, int64(5), scaleUpConfig.ScaleUpOwners["a"].MaxNodesCount)
}

func TestInitAutoscalingAndThePullShareOneApply(t *testing.T) {
	resetAutoscalingState(t)
	s := &server{cronScheduler: gocron.NewScheduler(time.UTC)}

	response, err := s.InitAutoscaling(context.Background(), &pb.InitAutoscalingRequest{
		Owners: map[string]*pb.OwnerAutoscalingConfig{
			"a": {
				MinIdleNodesCount: 1,
				MaxNodesCount:     3,
				ManualNodesCount:  2,
				InstancesRegions: []*pb.InstanceRegionsConfig{{
					InstanceType: "b3-8",
					Regions:      []*pb.ScalingRegion{{ImageId: "img-1", Region: "GRA11"}},
				}},
			},
		},
	})

	require.NoError(t, err)
	assert.Empty(t, response.GetError())
	assert.Equal(t, []string{"a"}, ownerNames(scaleUpConfig.ScaleUpOwners))
	assert.Equal(t, int64(3), scaleUpConfig.ScaleUpOwners["a"].MaxNodesCount)
	assert.True(t, scaleUpConfig.Loaded, "a push of the back is a complete list")

	// The list is loaded: a pull answer arriving later must not replace it.
	applied, err := s.applyAutoscalingOwners(map[string]scaleUpOwner{}, true)

	require.NoError(t, err)
	assert.False(t, applied)
	assert.Equal(t, []string{"a"}, ownerNames(scaleUpConfig.ScaleUpOwners))
}

// --- no destroy before a complete list, and the destroy once it is there ---

type recordingAutoscalingUtils struct {
	calls int
}

func (r *recordingAutoscalingUtils) buildConfigFromExistingInfra() (*TerraformConfig, error) {
	r.calls++
	return &TerraformConfig{NamedWorkers: map[string]TerraformNamedWorker{
		"gone-node": {Name: "gone-node", Owner: "gone"},
	}}, nil
}

func TestScaleDownDestroysNothingBeforeTheListIsLoaded(t *testing.T) {
	swarmClient := &fakeSwarmClient{nodes: []swarm.Node{readySwarmNode("gone-node", "gone")}}
	deploy := &fakeDeployUtils{}
	utils := &recordingAutoscalingUtils{}
	config := &ScaleUpConfig{ScaleUpOwners: map[string]scaleUpOwner{}}
	svc := newScaleDownService(swarmClient, log.NewEntry(log.New()), &sync.Mutex{}, config, deploy, utils)

	svc.run()

	assert.Equal(t, 0, deploy.launched, "no Terraform destroy before a complete list")
	assert.Empty(t, swarmClient.removed, "no swarm node removed before a complete list")
	assert.Equal(t, 0, utils.calls, "the run stops before it reads the infrastructure")

	// The same run once the list is loaded (it holds no owner "gone": the type was deleted).
	config.Loaded = true
	svc.run()

	assert.Equal(t, 1, deploy.launched)
}

type busyAndIdleSwarm struct {
	scaleDownDockerClient
	nodes      []swarm.Node
	busyNodeID string
}

func (b *busyAndIdleSwarm) NodeList(context.Context, tasksTypes.NodeListOptions) ([]swarm.Node, error) {
	return b.nodes, nil
}

func (b *busyAndIdleSwarm) TaskList(_ context.Context, options tasksTypes.TaskListOptions) ([]swarm.Task, error) {
	if options.Filters.Get("node")[0] == b.busyNodeID {
		return []swarm.Task{{Status: swarm.TaskStatus{State: swarm.TaskStateRunning}}}, nil
	}
	return nil, nil
}

// Guard, green today: the VMs of a type that left the list (max 0 or deleted) are scheduled for
// removal as soon as they are empty, whatever min idle was; a VM with a task is left for now.
func TestScaleDownRemovesTheEmptyVMOfATypeAbsentFromTheList(t *testing.T) {
	oldRemoveNonEmpty := c.ScaleDownRemoveNonEmpty
	c.ScaleDownRemoveNonEmpty = false
	t.Cleanup(func() { c.ScaleDownRemoveNonEmpty = oldRemoveNonEmpty })
	swarmClient := &busyAndIdleSwarm{
		nodes:      []swarm.Node{readySwarmNode("empty-node", "gone"), readySwarmNode("busy-node", "gone")},
		busyNodeID: "busy-node",
	}
	svc := newScaleDownService(
		swarmClient,
		log.NewEntry(log.New()),
		&sync.Mutex{},
		&ScaleUpConfig{Loaded: true, ScaleUpOwners: map[string]scaleUpOwner{"kept": {MinIdleNodesCount: 1, MaxNodesCount: 3}}},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{NamedWorkers: map[string]TerraformNamedWorker{
		"empty-node": {Name: "empty-node", Owner: "gone"},
		"busy-node":  {Name: "busy-node", Owner: "gone"},
	}}

	nodesToDelete, err := svc.findNodesToDelete(tfConfig)

	require.NoError(t, err)
	assert.Equal(t, []string{"empty-node"}, nodesToDelete)
}

// Guard, green today: a complete but empty list (a cluster with no autoscaled type left) makes every
// VM of the cluster an orphan, yet the one that still runs tasks (student containers) is never
// destroyed; it is kept until the existing scale-down logic finds it empty. The empty one goes.
func TestScaleDownKeepsAVMThatRunsTasksWhenTheListIsEmpty(t *testing.T) {
	oldRemoveNonEmpty := c.ScaleDownRemoveNonEmpty
	c.ScaleDownRemoveNonEmpty = false
	t.Cleanup(func() { c.ScaleDownRemoveNonEmpty = oldRemoveNonEmpty })
	swarmClient := &busyAndIdleSwarm{
		nodes:      []swarm.Node{readySwarmNode("empty-node", "gone"), readySwarmNode("busy-node", "gone")},
		busyNodeID: "busy-node",
	}
	svc := newScaleDownService(
		swarmClient,
		log.NewEntry(log.New()),
		&sync.Mutex{},
		&ScaleUpConfig{Loaded: true, ScaleUpOwners: map[string]scaleUpOwner{}},
		nil,
		nil,
	)
	tfConfig := &TerraformConfig{NamedWorkers: map[string]TerraformNamedWorker{
		"empty-node": {Name: "empty-node", Owner: "gone"},
		"busy-node":  {Name: "busy-node", Owner: "gone"},
	}}

	nodesToDelete, err := svc.findNodesToDelete(tfConfig)

	require.NoError(t, err)
	assert.Equal(t, []string{"empty-node"}, nodesToDelete, "the VM that runs a task stays, only the empty one is destroyed")
	assert.NotContains(t, nodesToDelete, "busy-node")
}
