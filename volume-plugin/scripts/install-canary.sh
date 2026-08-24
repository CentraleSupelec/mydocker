#!/usr/bin/env bash
# Install the volume canary on this host: copy the script, add a cron entry,
# and optionally configure the FS backing-store guard.
#
# The canary is what keeps a broken volume driver from going unnoticed again:
# volume creation is not on the student hot path, so nothing else exercises it
# regularly. It writes one line per run to a log a Zabbix UserParameter can
# read (exit code and the CANARY ok/FAIL line).
#
# Usage:
#   scripts/install-canary.sh --driver centralesupelec/mydockervolume:latest
#   scripts/install-canary.sh --driver ... --interval 15 --log /var/log/volume-canary.log
#   scripts/install-canary.sh --driver ... --fs-marker .mydocker-fs-ready --fs-root /mnt/mydocker-fs
#   scripts/install-canary.sh --driver ... --state-dir /var/lib/mydocker
#   scripts/install-canary.sh --uninstall
#   scripts/install-canary.sh --dry-run
set -euo pipefail

DRIVER="centralesupelec/mydockervolume:latest"
INTERVAL=15
LOG="/var/log/volume-canary.log"
INSTALL_DIR="/usr/local/lib/mydocker"
STATE_DIR="/var/lib/mydocker"
CRON_FILE="/etc/cron.d/volume-canary"
FS_MARKER=""
FS_ROOT="/mnt/mydocker-fs"
UNINSTALL=0
DRY_RUN=0
# Passed to `docker plugin enable`. Same default and override as upgrade.sh.
ENABLE_TIMEOUT="${ENABLE_TIMEOUT:-120}"

usage() {
    sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --driver)    DRIVER="$2"; shift 2 ;;
        --interval)  INTERVAL="$2"; shift 2 ;;
        --log)       LOG="$2"; shift 2 ;;
        --fs-marker) FS_MARKER="$2"; shift 2 ;;
        --fs-root)   FS_ROOT="$2"; shift 2 ;;
        --state-dir) STATE_DIR="$2"; shift 2 ;;
        --uninstall) UNINSTALL=1; shift ;;
        --dry-run)   DRY_RUN=1; shift ;;
        -h|--help)   usage 0 ;;
        *)           echo "unknown argument: $1" >&2; usage 1 ;;
    esac
done

run() {
    if [ "$DRY_RUN" -eq 1 ]; then
        echo "+ $*"
    else
        echo "+ $*" >&2
        "$@"
    fi
}

write_file() {  # path, content on stdin
    if [ "$DRY_RUN" -eq 1 ]; then
        echo "+ write $1:"
        sed 's/^/    /'
    else
        cat > "$1"
        echo "+ wrote $1" >&2
    fi
}

if [ "$UNINSTALL" -eq 1 ]; then
    run rm -f "$CRON_FILE"
    run rm -f "${INSTALL_DIR}/volume-canary.sh"
    # The state directory is kept deliberately: it holds the orphan and abandoned latches, and
    # an unreclaimed image outlives the canary that reported it.
    echo "### canary removed (log at $LOG and state at $STATE_DIR kept)"
    exit 0
fi

[ "$INTERVAL" -ge 1 ] && [ "$INTERVAL" -le 59 ] || { echo "--interval must be 1..59 minutes" >&2; exit 1; }

script_dir=$(cd "$(dirname "$0")" && pwd)
[ -f "${script_dir}/volume-canary.sh" ] || { echo "volume-canary.sh not found next to this script" >&2; exit 1; }

run mkdir -p "$INSTALL_DIR"
run mkdir -p "$STATE_DIR"
run install -m 755 "${script_dir}/volume-canary.sh" "${INSTALL_DIR}/volume-canary.sh"

write_file "$CRON_FILE" <<EOF
# MyDocker volume canary: exercises the volume driver so a broken plugin is
# noticed within one interval instead of at the next student session, and scans
# the docker journal for the driver's own RBD_* alerts.
# Installed by mydockervolume/scripts/install-canary.sh
SHELL=/bin/sh
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
*/${INTERVAL} * * * * root DRIVER=${DRIVER} CANARY_STATE_DIR=${STATE_DIR} ${INSTALL_DIR}/volume-canary.sh >> ${LOG} 2>&1
EOF

# FS mode: the guard only does anything once the marker exists on the real
# backing store and the plugin knows its name.
if [ -n "$FS_MARKER" ]; then
    if [ "$DRY_RUN" -eq 0 ] && ! mountpoint -q "$FS_ROOT"; then
        echo "refusing to create the marker: $FS_ROOT is not a mount point." >&2
        echo "Creating it on the naked rootfs would defeat the guard entirely." >&2
        exit 1
    fi
    run touch "${FS_ROOT}/${FS_MARKER}"
    run docker plugin disable -f "$DRIVER"
    run docker plugin set "$DRIVER" "FS_READY_MARKER=${FS_MARKER}"
    # --timeout: dockerd's HTTP client timeout for driver calls. A bare enable
    # inherits the daemon default, under which concurrent creates were observed
    # failing at ~40s on preprod while the volumes were in fact created
    # (2026-08-21). Re-enabling without it would silently reimpose that.
    run docker plugin enable --timeout "$ENABLE_TIMEOUT" "$DRIVER"
    echo "### FS guard active: ${FS_ROOT}/${FS_MARKER}"
fi

echo "### canary installed, runs every ${INTERVAL} min, logging to ${LOG}"
echo "### state (journal cursor, orphan and abandoned latches) in ${STATE_DIR}"
cat <<EOF

Zabbix UserParameter suggestions (add to the agent config on this host):
  UserParameter=mydocker.volume.canary,tail -1 ${LOG} | grep -c '^CANARY ok'
  UserParameter=mydocker.volume.zombies,tail -1 ${LOG} | sed -n 's/.*rbd_zombies=\([0-9]*\).*/\1/p'
  UserParameter=mydocker.volume.orphans,tail -1 ${LOG} | sed -n 's/.*rbd_orphans=\(-\{0,1\}[0-9]*\).*/\1/p'
  UserParameter=mydocker.volume.unmap_abandoned,tail -1 ${LOG} | sed -n 's/.*unmap_abandoned=\(-\{0,1\}[0-9]*\).*/\1/p'
  UserParameter=mydocker.volume.unmap_slow,tail -1 ${LOG} | sed -n 's/.*unmap_slow=\(-\{0,1\}[0-9]*\).*/\1/p'

Triggers:
  canary is 0                      the volume driver failed a full cycle
  zombies above 0                  timed-out rbd children are not being reaped
  orphans not 0                    an rbd image is stranded with no owner, see the runbook.
                                   -1 means the journal could not be read, so the check is blind
  unmap_abandoned not 0            a kernel mapping was never released, same handling as orphans
  unmap_slow above 0 repeatedly    unmap is degrading; not itself an incident

orphans and unmap_abandoned latch: they stay raised until an operator empties
${STATE_DIR}/rbd-orphans.pending or ${STATE_DIR}/rbd-abandoned.pending after reclaiming.
Each latched line carries the pool, namespace and image. Reclaim first, then clear
the line: the latch is the only lasting record, the journal line ages out. Confirm
nothing owns the image (no docker volume, not in rbd showmapped, no watcher in
rbd status) before removing it, and never unmap a device that findmnt shows mounted.
EOF
