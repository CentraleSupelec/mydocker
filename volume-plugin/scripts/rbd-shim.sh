#!/bin/sh
# Shim installed as /usr/bin/rbd (real CLI at /usr/bin/rbd.real).
#
# `rbd unmap` can hang forever waiting for a udev confirmation over libudev
# even though the kernel already removed the device. The volume driver does
# not rely on the CLI finishing (it watches /sys/bus/rbd/devices itself), so
# a hung unmap is pure waste: cap it hard. Exit codes propagate untouched --
# the driver keys on `exit status 16` (device busy). Belt and braces on top
# of the driver-side fix; every unmap and every kill is logged.

REAL=/usr/bin/rbd.real

for arg in "$@"; do
    case "$arg" in
        unmap)
            echo "RBD_SHIM capped unmap: rbd $*" >&2
            timeout -s KILL 60 "$REAL" "$@" >/dev/null
            rc=$?
            if [ "$rc" -eq 137 ]; then
                echo "RBD_SHIM killed hung unmap (rc=137): rbd $*" >&2
            fi
            exit "$rc"
            ;;
    esac
done

exec "$REAL" "$@"
