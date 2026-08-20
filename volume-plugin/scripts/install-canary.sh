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
#   scripts/install-canary.sh --uninstall
#   scripts/install-canary.sh --dry-run
set -euo pipefail

DRIVER="centralesupelec/mydockervolume:latest"
INTERVAL=15
LOG="/var/log/volume-canary.log"
INSTALL_DIR="/usr/local/lib/mydocker"
CRON_FILE="/etc/cron.d/volume-canary"
FS_MARKER=""
FS_ROOT="/mnt/mydocker-fs"
UNINSTALL=0
DRY_RUN=0

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
    echo "### canary removed (log at $LOG kept)"
    exit 0
fi

[ "$INTERVAL" -ge 1 ] && [ "$INTERVAL" -le 59 ] || { echo "--interval must be 1..59 minutes" >&2; exit 1; }

script_dir=$(cd "$(dirname "$0")" && pwd)
[ -f "${script_dir}/volume-canary.sh" ] || { echo "volume-canary.sh not found next to this script" >&2; exit 1; }

run mkdir -p "$INSTALL_DIR"
run install -m 755 "${script_dir}/volume-canary.sh" "${INSTALL_DIR}/volume-canary.sh"

write_file "$CRON_FILE" <<EOF
# MyDocker volume canary: exercises the volume driver so a broken plugin is
# noticed within one interval instead of at the next student session.
# Installed by mydockervolume/scripts/install-canary.sh
SHELL=/bin/sh
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
*/${INTERVAL} * * * * root DRIVER=${DRIVER} ${INSTALL_DIR}/volume-canary.sh >> ${LOG} 2>&1
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
    run docker plugin enable "$DRIVER"
    echo "### FS guard active: ${FS_ROOT}/${FS_MARKER}"
fi

echo "### canary installed, runs every ${INTERVAL} min, logging to ${LOG}"
cat <<EOF

Zabbix UserParameter suggestions (add to the agent config on this host):
  UserParameter=mydocker.volume.canary,tail -1 ${LOG} | grep -c '^CANARY ok'
  UserParameter=mydocker.volume.zombies,tail -1 ${LOG} | sed -n 's/.*rbd_zombies=\([0-9]*\).*/\1/p'
Alert when the first is 0 or the second is above 0.
EOF
