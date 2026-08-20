#!/usr/bin/env bash
# Post-upgrade smoke test for the volume plugin.
#
# Prints PASS or FAIL per check with the measured value and exits non-zero if
# any check failed, so it can gate a rollout.
#
# Checks 1-6 are read-mostly: they create and delete their own volumes and
# change no plugin settings. Checks 7-9 have to reconfigure the plugin
# (DRIVER_MODE, SHELL_TIMEOUT_SECONDS, FS_READY_MARKER), which means
# disabling it briefly, so they only run with --mutate-settings and always
# restore the previous values.
#
# Usage:
#   scripts/smoke.sh --driver centralesupelec/mydockervolume:latest
#   scripts/smoke.sh --driver ... --threshold 5 --mutate-settings
#   scripts/smoke.sh --driver ... --dry-run
set -uo pipefail

DRIVER="centralesupelec/mydockervolume:latest"
THRESHOLD=5
IMAGE="busybox:latest"
MUTATE=0
DRY_RUN=0
PREFIX="smoke-$(hostname -s)-$$"

usage() {
    sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --driver)          DRIVER="$2"; shift 2 ;;
        --threshold)       THRESHOLD="$2"; shift 2 ;;
        --image)           IMAGE="$2"; shift 2 ;;
        --mutate-settings) MUTATE=1; shift ;;
        --dry-run)         DRY_RUN=1; shift ;;
        -h|--help)         usage 0 ;;
        *)                 echo "unknown argument: $1" >&2; usage 1 ;;
    esac
done

failures=0
declare -a created_volumes=()

pass() { printf 'PASS  %-34s %s\n' "$1" "${2:-}"; }
fail() { printf 'FAIL  %-34s %s\n' "$1" "${2:-}"; failures=$((failures + 1)); }
skip() { printf 'SKIP  %-34s %s\n' "$1" "${2:-}"; }

now_ms() { date +%s%3N 2>/dev/null || echo $(( $(date +%s) * 1000 )); }

cleanup() {
    for v in "${created_volumes[@]:-}"; do
        [ -n "$v" ] && docker volume rm -f "$v" >/dev/null 2>&1
    done
}
trap cleanup EXIT

create_volume() {  # name, size_mb -> prints elapsed ms, returns create status
    local name="$1" size="$2" start end
    created_volumes+=("$name")
    start=$(now_ms)
    if ! docker volume create -d "$DRIVER" -o size="$size" "$name" >/dev/null 2>&1; then
        return 1
    fi
    end=$(now_ms)
    echo $((end - start))
}

plugin_pid() { pgrep -f '/mydockervolume' | head -1; }

rbd_zombies() {
    local pid
    pid=$(plugin_pid)
    if [ -n "$pid" ]; then
        ps -eo stat=,comm=,ppid= | awk -v p="$pid" '$1 ~ /^Z/ && $2 == "rbd" && $3 == p' | wc -l | tr -d ' '
    else
        echo 0
    fi
}

setting_of() {  # key -> current value ("" if absent)
    docker plugin inspect "$DRIVER" --format \
        '{{range .Settings.Env}}{{println .}}{{end}}' 2>/dev/null \
        | awk -F= -v k="$1" '$1 == k { print substr($0, length(k) + 2) }'
}

set_setting() {  # key=value, requires the plugin to be disabled
    docker plugin disable -f "$DRIVER" >/dev/null 2>&1 || return 1
    docker plugin set "$DRIVER" "$1" >/dev/null 2>&1 || { docker plugin enable "$DRIVER" >/dev/null 2>&1; return 1; }
    docker plugin enable "$DRIVER" >/dev/null 2>&1
}

if [ "$DRY_RUN" -eq 1 ]; then
    cat <<EOF
would run against driver '$DRIVER' (threshold ${THRESHOLD}s, image $IMAGE):
  1 create ${PREFIX}-1g   (1 GB)
  2 create ${PREFIX}-5t   (5 TB)
  3 filesystem type of the mounted volume is ext4
  4 write / unmount / remount / verify content
  5 ten create+delete cycles leave no defunct rbd children
  6 no leftover rbd device for the test volumes in /sys/bus/rbd/devices
EOF
    if [ "$MUTATE" -eq 1 ]; then
        cat <<EOF
  7 DRIVER_MODE=FS create/mount/unmount cycle, then restore
  8 SHELL_TIMEOUT_SECONDS=1 makes a large create fail fast, then restore
  9 FS_READY_MARKER set with the marker absent refuses create, then restore
EOF
    else
        echo "  7-9 skipped (pass --mutate-settings to include them)"
    fi
    exit 0
fi

echo "### smoke test against $DRIVER"

# 1 + 2: creation latency. With discard skipped at format time, size should
# barely matter; a slow 5 TB create is the signal that nodiscard regressed.
for spec in "1g:1024" "5t:5242880"; do
    label="${spec%%:*}"; size="${spec##*:}"
    if elapsed=$(create_volume "${PREFIX}-${label}" "$size"); then
        if [ "$elapsed" -lt $((THRESHOLD * 1000)) ]; then
            pass "create ${label}" "${elapsed}ms"
        else
            fail "create ${label}" "${elapsed}ms (threshold ${THRESHOLD}s)"
        fi
    else
        fail "create ${label}" "docker volume create failed"
    fi
done

# 3: the volume really carries a filesystem, read from the container's mounts
fstype=$(docker run --rm -v "${PREFIX}-1g:/d" "$IMAGE" \
    sh -c "awk '\$2 == \"/d\" { print \$3 }' /proc/mounts" 2>/dev/null | head -1)
case "$fstype" in
    ext4|ext3|ext2|xfs) pass "filesystem present" "$fstype" ;;
    "")                 fail "filesystem present" "could not read /proc/mounts in container" ;;
    *)                  fail "filesystem present" "unexpected type '$fstype'" ;;
esac

# 4: data survives unmount and remount
if docker run --rm -v "${PREFIX}-1g:/d" "$IMAGE" sh -c 'echo mydocker-smoke > /d/canary' >/dev/null 2>&1 &&
   docker run --rm -v "${PREFIX}-1g:/d" "$IMAGE" sh -c 'grep -q mydocker-smoke /d/canary' >/dev/null 2>&1; then
    pass "write, remount, verify"
else
    fail "write, remount, verify" "content did not survive the remount"
fi

# 5: repeated cycles must not leak rbd children (the historic zombie bug)
zombies_before=$(rbd_zombies)
cycle_failures=0
for i in $(seq 1 10); do
    name="${PREFIX}-cycle-${i}"
    if create_volume "$name" 1024 >/dev/null; then
        docker volume rm "$name" >/dev/null 2>&1 || cycle_failures=$((cycle_failures + 1))
    else
        cycle_failures=$((cycle_failures + 1))
    fi
done
zombies_after=$(rbd_zombies)
if [ "$cycle_failures" -gt 0 ]; then
    fail "ten create/delete cycles" "$cycle_failures cycle(s) failed"
elif [ "$zombies_after" -gt "$zombies_before" ]; then
    fail "no rbd zombies" "grew from $zombies_before to $zombies_after"
else
    pass "ten create/delete cycles" "zombies $zombies_before -> $zombies_after"
fi

# 6: no rbd device left mapped for our test volumes
if [ -d /sys/bus/rbd/devices ]; then
    leaked=0
    for dev in /sys/bus/rbd/devices/*/; do
        [ -e "$dev/name" ] || continue
        case "$(cat "$dev/name" 2>/dev/null)" in
            "${PREFIX}"*) leaked=$((leaked + 1)) ;;
        esac
    done
    total=$(find /sys/bus/rbd/devices -mindepth 1 -maxdepth 1 -type d 2>/dev/null | wc -l | tr -d ' ')
    if [ "$leaked" -eq 0 ]; then
        pass "no leaked rbd mappings" "$total device(s) mapped in total"
    else
        fail "no leaked rbd mappings" "$leaked test device(s) still mapped"
    fi
else
    skip "no leaked rbd mappings" "/sys/bus/rbd/devices absent (not an RBD host)"
fi

if [ "$MUTATE" -eq 0 ]; then
    skip "settings checks (7-9)" "pass --mutate-settings to run them"
else
    original_mode=$(setting_of DRIVER_MODE)
    original_timeout=$(setting_of SHELL_TIMEOUT_SECONDS)
    original_marker=$(setting_of FS_READY_MARKER)

    # 7: FS mode still works
    if set_setting "DRIVER_MODE=FS"; then
        if create_volume "${PREFIX}-fs" 1024 >/dev/null &&
           docker run --rm -v "${PREFIX}-fs:/d" "$IMAGE" sh -c 'echo ok > /d/f' >/dev/null 2>&1; then
            pass "FS mode cycle"
        else
            fail "FS mode cycle" "create or mount failed in FS mode"
        fi
        docker volume rm -f "${PREFIX}-fs" >/dev/null 2>&1

        # 9: with a marker configured but absent, the driver must refuse
        if set_setting "FS_READY_MARKER=.mydocker-smoke-absent"; then
            if docker volume create -d "$DRIVER" "${PREFIX}-guard" >/dev/null 2>&1; then
                fail "FS marker guard" "create succeeded with the marker absent"
                docker volume rm -f "${PREFIX}-guard" >/dev/null 2>&1
            else
                pass "FS marker guard" "create refused as expected"
            fi
        else
            fail "FS marker guard" "could not set FS_READY_MARKER"
        fi
        set_setting "FS_READY_MARKER=${original_marker}" || true
    else
        fail "FS mode cycle" "could not set DRIVER_MODE=FS"
    fi
    set_setting "DRIVER_MODE=${original_mode:-RBD}" || true

    # 8: a tiny timeout must fail fast rather than hang, and leave no zombie
    if set_setting "SHELL_TIMEOUT_SECONDS=1"; then
        zb=$(rbd_zombies)
        start=$(now_ms)
        docker volume create -d "$DRIVER" -o size=5242880 "${PREFIX}-timeout" >/dev/null 2>&1
        elapsed=$(( $(now_ms) - start ))
        docker volume rm -f "${PREFIX}-timeout" >/dev/null 2>&1
        za=$(rbd_zombies)
        if [ "$elapsed" -lt $((THRESHOLD * 1000 * 12)) ] && [ "$za" -le "$zb" ]; then
            pass "timeout knob fails fast" "${elapsed}ms, zombies $zb -> $za"
        else
            fail "timeout knob fails fast" "${elapsed}ms, zombies $zb -> $za"
        fi
    else
        fail "timeout knob fails fast" "could not set SHELL_TIMEOUT_SECONDS"
    fi
    set_setting "SHELL_TIMEOUT_SECONDS=${original_timeout:-10}" || true

    echo "### restored: DRIVER_MODE=$(setting_of DRIVER_MODE) SHELL_TIMEOUT_SECONDS=$(setting_of SHELL_TIMEOUT_SECONDS) FS_READY_MARKER=$(setting_of FS_READY_MARKER)"
fi

echo
if [ "$failures" -eq 0 ]; then
    echo "### all checks passed"
    exit 0
fi
echo "### $failures check(s) failed"
exit 1
