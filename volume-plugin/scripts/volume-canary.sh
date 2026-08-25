#!/usr/bin/env bash
# Volume-driver canary: full create/mount/write/remount/verify/remove cycle,
# plus a scan of the driver's own alerts in the docker journal.
# Emits exactly one result line, cron- and Zabbix-UserParameter-friendly:
#   CANARY ok create_s=<s> total_s=<s> queueing=<0|1> rbd_zombies=<n> rbd_mapped=0 rbd_orphans=<n> unmap_abandoned=<n> unmap_late=<n> unmap_slow=<n> time=<iso>
#   CANARY FAIL step=<step> rbd_zombies=<n> rbd_mapped=<n> rbd_orphans=<n> unmap_abandoned=<n> unmap_late=<n> unmap_slow=<n> time=<iso>
# time is last so the `^CANARY ok` anchor and the field patterns keep matching.
# cron appends to the log without dates of its own, so a line that cannot be
# dated cannot be attributed to a plugin version: a soak needs that.
# Exit 0 on success, 1 on failure. The rbd_zombies count catches the historic
# failure mode where timed-out rbd children were never reaped.
#
# queueing is 1 when a cycle succeeded but took longer than CANARY_QUEUEING_SECONDS.
# A cascade does not fail the canary: measured 2026-08-25, a burst that failed 17 of
# 25 concurrent removes stretched one cycle from 4s to 213s and still reported ok with
# every counter at 0, so latency was the only evidence and nothing read it. It is a
# separate field rather than a FAIL because queueing is degradation, not an outage,
# and paging on it as an outage teaches people to ignore the canary.
#
# Run on a swarm manager. Override with DRIVER / CANARY_IMAGE / CANARY_SIZE_MB /
# CANARY_STATE_DIR / CANARY_QUEUEING_SECONDS.

set -u

DRIVER="${DRIVER:-centralesupelec/mydockervolume:latest}"
IMAGE="${CANARY_IMAGE:-busybox:latest}"
SIZE_MB="${CANARY_SIZE_MB:-1024}"
VOL="canary-$(hostname -s)-$$"
STATE_DIR="${CANARY_STATE_DIR:-/var/lib/mydocker}"
# A healthy cycle is 3-5s on preprod. 30 is high enough that ordinary jitter and a
# busy node do not trip it, low enough to catch a queue forming well before the 60s
# cap dockerd puts on Remove and Unmount.
QUEUEING_SECONDS="${CANARY_QUEUEING_SECONDS:-30}"
CURSOR="${STATE_DIR}/canary-journal.cursor"
ORPHAN_LATCH="${STATE_DIR}/rbd-orphans.pending"
ABANDONED_LATCH="${STATE_DIR}/rbd-abandoned.pending"

# Count rbd devices still mapped for this canary's image. A create or a remove that returned
# success while its device is still mapped is exactly the failure the zombie count cannot see:
# the child was reaped correctly, the CLI exited 0, and the kernel still holds the mapping.
rbd_mapped() {
    # /sys/bus/rbd/devices/*/name holds the image name of each mapped device.
    count=0
    for name_file in /sys/bus/rbd/devices/*/name; do
        [ -r "$name_file" ] || continue
        if [ "$(cat "$name_file" 2>/dev/null)" = "$1" ]; then
            count=$((count + 1))
        fi
    done
    echo "$count"
}

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

latch_count() {
    [ -f "$1" ] || { echo 0; return; }
    wc -l < "$1" | tr -d ' '
}

# Scan the docker journal for the driver's alert lines since the previous run. dockerd forwards
# plugin stderr to its own unit, so RBD_* lines land in `journalctl -u docker.service`.
#
# ORPHAN_IMAGE and UNMAP_ABANDONED are latched to files, not reported as a per-window count: each
# is a one-shot line meaning an image or a kernel mapping is stranded until a human reclaims it. A
# window count falls back to zero one interval later, so a monitor polling in between would see a
# clean host and the event would be lost. The latch stays raised until an operator empties it after
# reclaiming the image or the mapping by hand. LATE and SLOW are trend signals, not work items, so
# the window count is the right shape for them.
#
# All four report -1 when the journal cannot be read: a monitoring path that silently answers
# "nothing wrong" when it is in fact blind is the failure this whole canary exists to prevent.
journal_scan() {
    orphans=0; abandoned=0; late=0; slow=0
    window=""

    command -v journalctl >/dev/null 2>&1 || { orphans=-1; abandoned=-1; late=-1; slow=-1; return; }
    mkdir -p "$STATE_DIR" 2>/dev/null

    if [ ! -f "$CURSOR" ]; then
        # First run: park the cursor at the current end of the journal. Scanning from the start
        # would latch every historic event, including ones already dealt with, and the operator
        # would begin by clearing a backlog that says nothing about the running version.
        journalctl -u docker.service --cursor-file="$CURSOR" -n 1 >/dev/null 2>&1
    else
        window=$(journalctl -u docker.service --cursor-file="$CURSOR" --no-pager -o short-iso 2>/dev/null)
        if [ $? -ne 0 ]; then
            orphans=-1; abandoned=-1; late=-1; slow=-1
            return
        fi
        printf '%s\n' "$window" | grep -F 'Message=RBD_ORPHAN_IMAGE' >> "$ORPHAN_LATCH" 2>/dev/null
        printf '%s\n' "$window" | grep -F 'Message=RBD_UNMAP_ABANDONED' >> "$ABANDONED_LATCH" 2>/dev/null
    fi

    orphans=$(latch_count "$ORPHAN_LATCH")
    abandoned=$(latch_count "$ABANDONED_LATCH")
    late=$(printf '%s\n' "$window" | grep -cF 'Message=RBD_UNMAP_LATE')
    slow=$(printf '%s\n' "$window" | grep -cF 'Message=RBD_UNMAP_SLOW')
}

fail() {
    journal_scan
    echo "CANARY FAIL step=$1 rbd_zombies=$(rbd_zombies) rbd_mapped=$(rbd_mapped "$VOL") rbd_orphans=${orphans} unmap_abandoned=${abandoned} unmap_late=${late} unmap_slow=${slow} time=$(date -Is)"
    docker volume rm -f "$VOL" >/dev/null 2>&1
    exit 1
}

t_start=$(date +%s)

docker volume create -d "$DRIVER" -o size="$SIZE_MB" "$VOL" >/dev/null 2>&1 \
    || fail create
t_created=$(date +%s)

# Create leaves the image unmapped by contract. If a device is still mapped here, the driver
# reported a success its own teardown did not achieve, and nothing else in this script would
# notice: the volume works, the zombie count is zero, and the leak is silent.
[ "$(rbd_mapped "$VOL")" -eq 0 ] || fail create-left-device-mapped

docker run --rm -v "$VOL:/data" "$IMAGE" \
    sh -c 'echo canary > /data/canary' >/dev/null 2>&1 \
    || fail write

# Checked after BOTH container cycles, not just the second: if the first Unmount falsely reported
# success and left the device mapped, the second mount would reuse that same mapping and the later
# check would pass, hiding it.
[ "$(rbd_mapped "$VOL")" -eq 0 ] || fail unmount-after-write-left-device-mapped

docker run --rm -v "$VOL:/data" "$IMAGE" \
    sh -c 'grep -q canary /data/canary' >/dev/null 2>&1 \
    || fail remount-verify

# The container exited and was removed, so Unmount has run and the device must be released. Checking
# only after create and after remove left a false-success Unmount invisible: the volume still works,
# the next mount still succeeds, and the mapping simply accumulates.
[ "$(rbd_mapped "$VOL")" -eq 0 ] || fail unmount-left-device-mapped

docker volume rm "$VOL" >/dev/null 2>&1 \
    || fail remove

# Same check after removal: a Remove that answered success while the device stayed mapped is the
# other half of the same defect.
[ "$(rbd_mapped "$VOL")" -eq 0 ] || fail remove-left-device-mapped

t_end=$(date +%s)
journal_scan
total_s=$((t_end - t_start))
queueing=0
[ "$total_s" -ge "$QUEUEING_SECONDS" ] && queueing=1
echo "CANARY ok create_s=$((t_created - t_start)) total_s=${total_s} queueing=${queueing} rbd_zombies=$(rbd_zombies) rbd_mapped=0 rbd_orphans=${orphans} unmap_abandoned=${abandoned} unmap_late=${late} unmap_slow=${slow} time=$(date -Is)"
