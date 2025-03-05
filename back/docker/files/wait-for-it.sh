#!/usr/bin/env bash

set -e

HOST="$1"
PORT="$2"
TIMEOUT="${3:-15}"

if [ -z "$HOST" ]; then
  echo "Usage: wait-for-it.sh host port [timeout]"
  exit 1
fi

if [ -n "$HOST" ] && [ -z "$PORT" ]; then
  if [[ "$HOST" =~ :// ]]; then
    HOST="$(echo "$HOST" | sed -E 's|.*://([^/]+).*|\1|')"
  fi
  PORT="$(echo "$HOST" | cut -d':' -f2)"
  HOST="$(echo "$HOST" | cut -d':' -f1)"
fi

echo "Waiting for $HOST:$PORT to be available..."

for i in $(seq $TIMEOUT); do
  if nc -z "$HOST" "$PORT"; then
    echo "$HOST:$PORT is available after $i seconds"
    exit 0
  fi
  sleep 1
done

echo "Timeout after $TIMEOUT seconds waiting for $HOST:$PORT"
exit 1
