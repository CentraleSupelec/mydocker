package main

import (
	"context"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
	"github.com/go-co-op/gocron"
	log "github.com/sirupsen/logrus"
	"strconv"
	"time"
)

func addCleaningCron(s *gocron.Scheduler, dockerClient *client.Client) {
	_, _ = s.Every(5).Minute().Do(
		func() {
			findServiceToDeleteAndDoIt(dockerClient)
			reapCompletedOneShotServices(dockerClient)
		})
}

type scheduledServiceCleanerClient interface {
	ServiceList(ctx context.Context, options types.ServiceListOptions) ([]swarm.Service, error)
	ServiceRemove(ctx context.Context, serviceID string) error
}

func findServiceToDeleteAndDoIt(dockerClient scheduledServiceCleanerClient) {
	nowSec := time.Now().Unix()
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("label", "deleteAfter=true")
	services, err := dockerClient.ServiceList(context.TODO(), types.ServiceListOptions{Filters: filtersArgs})
	if err != nil {
		log.Errorf("Failed to get service to delete: %s", err)
		return
	}
	for _, service := range services {
		deletionDateStr, exist := service.Spec.Labels["deletionTime"]
		if !exist {
			log.Errorf("Failed to find deletion time for service %s", service.ID)
			continue
		}
		deletionDate, err := strconv.Atoi(deletionDateStr)
		if err != nil {
			log.Errorf("Failed to parse deletion time for service %s, deletion time: %s", service.ID, deletionDateStr)
			continue
		}

		if int64(deletionDate) < nowSec {
			log.Infof("Delete service %s", service.ID)
			err = dockerClient.ServiceRemove(context.TODO(), service.ID)
			if err != nil {
				log.Errorf("Failed to delete service %s in cron job", service.ID)
			}
		}
	}
}

// Services this reaper is allowed to remove: one-shot jobs created by this API, which are
// finished once their task completes. Set at creation; see build_docker_image.go.
const oneShotLabel = "mydocker.oneshot"

// Reap one-shot jobs whose task has completed. Named for what it removes, which is
// services: a task cannot be deleted on its own.
//
// The label check is the whole point. This used to list every task in the swarm with no
// filter and remove the service owning any task that ended `complete`, which deletes
// long-running services that merely exited cleanly: measured on a monolith deployment on
// 2026-09-16, where a reboot ended caddy, postgres and the frontend tasks with exit 0 and
// this function removed all three services 22 seconds after boot, leaving the backend
// crash-looping with no database. A clean exit is not a reason to delete a service; swarm
// restarts it per its restart policy. It IS the completion signal for a one-shot job, and a
// task cannot be reaped on its own, so removing the service is the only cleanup available
// there.
//
// Student labs are not reaped here. They carry deleteAfter/deletionTime and belong to
// findServiceToDeleteAndDoIt above, which is scoped by label already.
// Narrow enough to fake in a test. The wide *client.Client satisfies it, and the reaper's
// behaviour is the kind that has to be asserted rather than reasoned about: its previous
// version read correctly and deleted production services.
type completedTaskReaperClient interface {
	TaskList(ctx context.Context, options types.TaskListOptions) ([]swarm.Task, error)
	ServiceInspectWithRaw(ctx context.Context, serviceID string, options types.ServiceInspectOptions) (swarm.Service, []byte, error)
	ServiceRemove(ctx context.Context, serviceID string) error
}

func reapCompletedOneShotServices(dockerClient completedTaskReaperClient) {
	filtersArgs := filters.NewArgs()
	filtersArgs.Add("desired-state", string(swarm.TaskStateShutdown))
	tasks, err := dockerClient.TaskList(context.TODO(), types.TaskListOptions{Filters: filtersArgs})
	if err != nil {
		log.Errorf("Failed to get tasks to delete: %s", err)
		return
	}
	for _, task := range tasks {
		if task.Status.State != swarm.TaskStateComplete {
			continue
		}

		// Ask the service, not the task: the label is on the service spec, and a task
		// whose service has already gone is nothing to clean up.
		service, _, err := dockerClient.ServiceInspectWithRaw(context.TODO(), task.ServiceID, types.ServiceInspectOptions{})
		if err != nil {
			log.Debugf("Skipping completed task %s: cannot inspect service %s: %s", task.ID, task.ServiceID, err)
			continue
		}
		if service.Spec.Labels[oneShotLabel] != "true" {
			log.Debugf("Leaving service %s alone: completed task, but not a one-shot job", service.Spec.Name)
			continue
		}

		log.Infof("Delete service %s", task.ServiceID)
		err = dockerClient.ServiceRemove(context.TODO(), task.ServiceID)
		if err != nil {
			log.Errorf("Failed to delete service %s in cron job", task.ServiceID)
		}
	}
}
