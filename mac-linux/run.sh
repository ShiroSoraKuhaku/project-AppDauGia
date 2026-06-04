#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
STATE_DIR="$SCRIPT_DIR/.run"

mkdir -p "$STATE_DIR"

docker_compose() {
  if command -v docker >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi

  echo "Khong tim thay Docker Compose. Hay mo Docker Desktop truoc khi chay." >&2
  exit 1
}

pushd "$ROOT_DIR" >/dev/null
docker_compose up -d --build db server
popd >/dev/null

echo "Waiting for server on localhost:8080..."
ready=0
for _ in $(seq 1 60); do
  if (echo > /dev/tcp/127.0.0.1/8080) >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "$ready" -ne 1 ]]; then
  echo "Server is not ready on localhost:8080 after 2 minutes." >&2
  exit 1
fi
echo "System started. Run client.sh to open one or more client instances."
