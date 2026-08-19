#!/usr/bin/env bash
# Volume-driver canary: full create/mount/write/remount/verify/remove cycle.
# Emits exactly one result line, cron- and Zabbix-UserParameter-friendly:
#   CANARY ok create_s=<s> total_s=<s> rbd_zombies=<n>
#   CANARY FAIL step=<step> rbd_zombies=<n>
# Exit 0 on success, 1 on failure. The rbd_zombies count catches the historic
# failure mode where timed-out rbd children were never reaped.
#
# Run on a swarm manager. Override with DRIVER / CANARY_IMAGE / CANARY_SIZE_MB.

set -u

DRIVER="${DRIVER:-centralesupelec/mydockervolume:latest}"
IMAGE="${CANARY_IMAGE:-busybox:latest}"
SIZE_MB="${CANARY_SIZE_MB:-1024}"
VOL="canary-$(hostname -s)-$$"

rbd_zombies() {
    # count only zombies parented to the plugin process; fall back to a
    # host-wide count if the plugin PID cannot be resolved
    plugin_pid=$(pgrep -f '/mydockervolume' | head -1)
    if [ -n "$plugin_pid" ]; then
        ps -eo stat=,comm=,ppid= | awk -v p="$plugin_pid" '$1 ~ /^Z/ && $2 == "rbd" && $3 == p' | wc -l | tr -d ' '
    else
        ps -eo stat=,comm= | awk '$1 ~ /^Z/ && $2 == "rbd"' | wc -l | tr -d ' '
    fi
}

fail() {
    echo "CANARY FAIL step=$1 rbd_zombies=$(rbd_zombies)"
    docker volume rm -f "$VOL" >/dev/null 2>&1
    exit 1
}

t_start=$(date +%s)

docker volume create -d "$DRIVER" -o size="$SIZE_MB" "$VOL" >/dev/null 2>&1 \
    || fail create
t_created=$(date +%s)

docker run --rm -v "$VOL:/data" "$IMAGE" \
    sh -c 'echo canary > /data/canary' >/dev/null 2>&1 \
    || fail write

docker run --rm -v "$VOL:/data" "$IMAGE" \
    sh -c 'grep -q canary /data/canary' >/dev/null 2>&1 \
    || fail remount-verify

docker volume rm "$VOL" >/dev/null 2>&1 \
    || fail remove

t_end=$(date +%s)
echo "CANARY ok create_s=$((t_created - t_start)) total_s=$((t_end - t_start)) rbd_zombies=$(rbd_zombies)"
