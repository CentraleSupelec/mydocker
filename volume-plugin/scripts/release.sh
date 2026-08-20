#!/usr/bin/env bash
# Build and push a plugin release.
#
# The driver is cgo (go-ceph/librados), so it cannot be cross-compiled: the
# builder stage must run on the target architecture. Emulated amd64 Go also
# segfaults under qemu on Apple silicon. This script therefore refuses to
# build for an architecture the host cannot build natively, instead of
# producing a plugin whose binary is silently the wrong architecture.
#
# The plugin name must be registry-qualified when pushing anywhere other than
# Docker Hub; the Makefile default is not, so it is required here.
#
# --promote skips the build and re-tags the rootfs already in ./plugin, so the
# exact bits tested on preprod under a release-candidate version can ship as
# the final version without a rebuild. Only valid in the same checkout that
# built them, and only before another build runs `make clean`.
#
# Usage:
#   scripts/release.sh --version 1.4.0 --name registry.example.org/centralesupelec/mydockervolume
#   scripts/release.sh --version 1.4.0 --name ... --platform linux/arm64 --no-push
#   scripts/release.sh --version 1.4.0 --name ... --promote
#   scripts/release.sh --version 1.4.0 --name ... --promote --alias centralesupelec/mydockervolume:latest
#   scripts/release.sh --version 1.4.0 --name ... --dry-run
#
# With --alias, a promotion whose ./plugin directory is gone reconstructs it
# from the plugin currently installed under that alias (its rootfs IS the
# soaked artifact), so the bits that ran the soak are the bits that ship.
set -euo pipefail

VERSION=""
NAME=""
PLATFORM="linux/amd64"
PUSH=1
DRY_RUN=0
PROMOTE=0
ALIAS=""

usage() {
    sed -n '2,27p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --version)  VERSION="$2"; shift 2 ;;
        --name)     NAME="$2"; shift 2 ;;
        --platform) PLATFORM="$2"; shift 2 ;;
        --no-push)  PUSH=0; shift ;;
        --promote)  PROMOTE=1; shift ;;
        --alias)    ALIAS="$2"; shift 2 ;;
        --dry-run)  DRY_RUN=1; shift ;;
        -h|--help)  usage 0 ;;
        *)          echo "unknown argument: $1" >&2; usage 1 ;;
    esac
done

[ -n "$VERSION" ] || { echo "--version is required" >&2; exit 1; }
[ -n "$NAME" ] || { echo "--name is required (registry-qualified, or a bare name pushes to Docker Hub)" >&2; exit 1; }

case "$NAME" in
    */*/*) : ;;
    *) [ "$PUSH" -eq 0 ] || { echo "refusing to push '$NAME': not registry-qualified (host/org/name). Use --no-push to build only." >&2; exit 1; } ;;
esac

# host architecture vs requested platform. A promotion re-tags an existing
# rootfs and compiles nothing, so the architecture check does not apply.
host_arch=$(uname -m)
case "$host_arch" in
    x86_64|amd64)  host_platform="linux/amd64" ;;
    aarch64|arm64) host_platform="linux/arm64" ;;
    *)             host_platform="unknown/$host_arch" ;;
esac

if [ "$PROMOTE" -eq 0 ] && [ "$PLATFORM" != "$host_platform" ]; then
    cat >&2 <<EOF
refusing to build $PLATFORM on a $host_platform host.

The plugin links librados through cgo, so the builder stage cannot
cross-compile, and emulated builds of the Go toolchain segfault. Run this
script on a $PLATFORM host (a swarm manager works), or pass
--platform $host_platform to build for this machine only.
EOF
    exit 1
fi

run() {
    if [ "$DRY_RUN" -eq 1 ]; then
        echo "+ $*"
    else
        echo "+ $*" >&2
        "$@"
    fi
}

# Fail on missing registry credentials BEFORE the build, not after: a push
# that dies on auth wastes the whole build and leaves the operator mid-flow.
if [ "$PUSH" -eq 1 ] && [ "$DRY_RUN" -eq 0 ]; then
    registry=${NAME%%/*}
    if ! grep -q "\"$registry\"" "${DOCKER_CONFIG:-$HOME/.docker}/config.json" 2>/dev/null; then
        echo "no credentials for '$registry' found; run first:" >&2
        echo "    docker login $registry" >&2
        echo "(or pass --no-push to build without pushing)" >&2
        exit 1
    fi
fi

repo_root=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_root"

if [ "$PROMOTE" -eq 1 ]; then
    if [ ! -d ./plugin/rootfs ] && [ -n "$ALIAS" ]; then
        # the installed plugin's rootfs is the soaked artifact itself
        plugin_id=$(docker plugin inspect -f '{{.Id}}' "$ALIAS" 2>/dev/null) || {
            echo "cannot inspect '$ALIAS' to reconstruct the rootfs" >&2; exit 1; }
        plugin_dir="/var/lib/docker/plugins/${plugin_id}"
        if [ ! -d "${plugin_dir}/rootfs" ] || [ ! -f "${plugin_dir}/config.json" ]; then
            echo "installed plugin ${plugin_id} has no readable rootfs/config.json under ${plugin_dir} (need root)" >&2
            exit 1
        fi
        echo "### reconstructing ./plugin from installed ${ALIAS} (${plugin_id})"
        run mkdir -p ./plugin
        run cp -a "${plugin_dir}/config.json" ./plugin/
        run cp -a "${plugin_dir}/rootfs" ./plugin/
    fi
    if [ ! -d ./plugin/rootfs ]; then
        echo "refusing to promote: ./plugin/rootfs does not exist." >&2
        echo "Promotion re-tags the rootfs an earlier build left here; run the build first in this checkout," >&2
        echo "or pass --alias <installed-alias> to reconstruct it from the running plugin." >&2
        exit 1
    fi
    echo "### promoting the rootfs in ./plugin to ${NAME}:${VERSION} (no rebuild)"
    run make PLUGIN_NAME="$NAME" PLUGIN_VERSION="$VERSION" create
else
    echo "### building ${NAME}:${VERSION} for ${PLATFORM}"
    run make PLUGIN_NAME="$NAME" PLUGIN_VERSION="$VERSION" PLATFORM="$PLATFORM" all
fi

if [ "$PUSH" -eq 1 ]; then
    echo "### pushing ${NAME}:${VERSION}"
    run make PLUGIN_NAME="$NAME" PLUGIN_VERSION="$VERSION" push
else
    echo "### skipping push (--no-push)"
fi

echo "### done: ${NAME}:${VERSION}"
echo "next: scripts/upgrade.sh --name ${NAME} --version ${VERSION} on each host, then scripts/smoke.sh"
