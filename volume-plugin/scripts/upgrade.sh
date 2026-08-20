#!/usr/bin/env bash
# Upgrade the installed volume plugin on this host.
#
# Records the currently installed version first and prints the exact rollback
# command, because `docker plugin upgrade` leaves the old image only in the
# registry: if the new version misbehaves there is no local undo.
#
# The plugin is disabled with -f, which briefly makes volume operations on
# this host fail. Run it on one host at a time.
#
# Usage:
#   scripts/upgrade.sh --name registry.example.org/centralesupelec/mydockervolume --version 1.4.0
#   scripts/upgrade.sh --name ... --version 1.4.0 --alias centralesupelec/mydockervolume:latest
#   scripts/upgrade.sh --name ... --version 1.4.0 --dry-run
set -euo pipefail

NAME=""
VERSION=""
ALIAS=""
DRY_RUN=0

usage() {
    sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --name)    NAME="$2"; shift 2 ;;
        --version) VERSION="$2"; shift 2 ;;
        --alias)   ALIAS="$2"; shift 2 ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage 0 ;;
        *)         echo "unknown argument: $1" >&2; usage 1 ;;
    esac
done

[ -n "$NAME" ] || { echo "--name is required" >&2; exit 1; }
[ -n "$VERSION" ] || { echo "--version is required" >&2; exit 1; }

# The installed plugin is usually referenced by a stable alias; services keep
# using that name across upgrades.
[ -n "$ALIAS" ] || ALIAS="centralesupelec/mydockervolume:latest"

run() {
    if [ "$DRY_RUN" -eq 1 ]; then
        echo "+ $*"
    else
        echo "+ $*" >&2
        "$@"
    fi
}

dump_env() {  # every settable env value, one KEY=value per line, sorted
    docker plugin inspect "$ALIAS" --format '{{range .Settings.Env}}{{println .}}{{end}}' 2>/dev/null | sed '/^$/d' | sort
}

installed=$(docker plugin inspect "$ALIAS" --format '{{.PluginReference}}' 2>/dev/null || true)
if [ -z "$installed" ]; then
    echo "plugin '$ALIAS' is not installed on this host" >&2
    echo "install it first: docker plugin install ${NAME}:${VERSION} --alias ${ALIAS}" >&2
    exit 1
fi

env_before=$(dump_env)

echo "### currently installed: $installed"
echo "### settings before upgrade:"
echo "$env_before" | sed 's/^/    /'
echo "### rollback command if this upgrade goes wrong:"
echo "    docker plugin disable -f ${ALIAS} && docker plugin upgrade ${ALIAS} ${installed} && docker plugin enable ${ALIAS}"
echo

run docker plugin disable -f "$ALIAS"
run docker plugin upgrade --grant-all-permissions "$ALIAS" "${NAME}:${VERSION}"
run docker plugin enable "$ALIAS"

if [ "$DRY_RUN" -eq 0 ]; then
    echo "### now installed: $(docker plugin inspect "$ALIAS" --format '{{.PluginReference}}')"
    docker plugin inspect "$ALIAS" --format '### enabled: {{.Enabled}}'

    # An upgrade that silently drops a setting is worse than a failed upgrade:
    # a driver that comes back pointing at the wrong pool or keyring fails
    # every volume operation. Compare, and print the exact repair commands.
    env_after=$(dump_env)
    lost=""
    while IFS= read -r line; do
        [ -n "$line" ] || continue
        key=${line%%=*}
        after=$(echo "$env_after" | grep "^${key}=" || true)
        if [ "$after" != "$line" ]; then
            lost="${lost}${line}
"
        fi
    done <<EOF
$env_before
EOF

    if [ -n "$lost" ]; then
        echo
        echo "### WARNING: settings changed or lost across the upgrade:"
        echo "$lost" | sed '/^$/d' | sed 's/^/    was: /'
        echo "### restore them with:"
        echo "    docker plugin disable -f ${ALIAS}"
        echo "$lost" | sed '/^$/d' | sed "s|^|    docker plugin set ${ALIAS} |"
        echo "    docker plugin enable ${ALIAS}"
        echo "### do not run the smoke test until the settings are correct"
        exit 1
    fi
    echo "### settings carried over unchanged"
fi

echo "next: scripts/smoke.sh --driver ${ALIAS}"
