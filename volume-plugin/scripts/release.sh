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
# Usage:
#   scripts/release.sh --version 1.4.0 --name registry.example.org/centralesupelec/mydockervolume
#   scripts/release.sh --version 1.4.0 --name ... --platform linux/arm64 --no-push
#   scripts/release.sh --version 1.4.0 --name ... --dry-run
set -euo pipefail

VERSION=""
NAME=""
PLATFORM="linux/amd64"
PUSH=1
DRY_RUN=0

usage() {
    sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --version)  VERSION="$2"; shift 2 ;;
        --name)     NAME="$2"; shift 2 ;;
        --platform) PLATFORM="$2"; shift 2 ;;
        --no-push)  PUSH=0; shift ;;
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

# host architecture vs requested platform
host_arch=$(uname -m)
case "$host_arch" in
    x86_64|amd64)  host_platform="linux/amd64" ;;
    aarch64|arm64) host_platform="linux/arm64" ;;
    *)             host_platform="unknown/$host_arch" ;;
esac

if [ "$PLATFORM" != "$host_platform" ]; then
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

repo_root=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_root"

echo "### building ${NAME}:${VERSION} for ${PLATFORM}"
run make PLUGIN_NAME="$NAME" PLUGIN_VERSION="$VERSION" PLATFORM="$PLATFORM" all

if [ "$PUSH" -eq 1 ]; then
    echo "### pushing ${NAME}:${VERSION}"
    run make PLUGIN_NAME="$NAME" PLUGIN_VERSION="$VERSION" push
else
    echo "### skipping push (--no-push)"
fi

echo "### done: ${NAME}:${VERSION}"
echo "next: scripts/upgrade.sh --name ${NAME} --version ${VERSION} on each host, then scripts/smoke.sh"
