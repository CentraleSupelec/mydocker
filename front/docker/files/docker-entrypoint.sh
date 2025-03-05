#!/bin/sh

export API_HOST=${API_HOST:-localhost}
export API_PORT=${API_PORT:-8080}

export CAS_HOST=${CAS_HOST:-localhost}
export CAS_PORT=${CAS_PORT:-9443}

ENV_FILE_TEMPLATE="/app/docker/files/config.js.template"
ENV_FILE="/app/src/assets/config.js"

envsubst < "$ENV_FILE_TEMPLATE" > "$ENV_FILE"

cat "$ENV_FILE"

exec "$@"
