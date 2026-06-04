#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
STATE_DIR="$SCRIPT_DIR/.run"
CLIENT_STATE_DIR="$STATE_DIR/clients"
LEGACY_CLIENT_PID_FILE="$STATE_DIR/client.pid"
LEGACY_CLIENT_STDOUT_LOG="$STATE_DIR/client.out.log"
LEGACY_CLIENT_STDERR_LOG="$STATE_DIR/client.err.log"

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

if [[ -d "$CLIENT_STATE_DIR" ]]; then
  find "$CLIENT_STATE_DIR" -maxdepth 1 -type f -name '*.pid' -print0 | while IFS= read -r -d '' pid_file; do
    client_pid="$(cat "$pid_file")"
    if [[ "$client_pid" =~ ^[0-9]+$ ]] && kill -0 "$client_pid" >/dev/null 2>&1; then
      kill "$client_pid" >/dev/null 2>&1 || true
      sleep 1
      kill -9 "$client_pid" >/dev/null 2>&1 || true
    fi
    rm -f "$pid_file"
  done
  rmdir "$CLIENT_STATE_DIR" 2>/dev/null || true
fi

if [[ -f "$LEGACY_CLIENT_PID_FILE" ]]; then
  client_pid="$(cat "$LEGACY_CLIENT_PID_FILE")"
  if [[ "$client_pid" =~ ^[0-9]+$ ]] && kill -0 "$client_pid" >/dev/null 2>&1; then
    kill "$client_pid" >/dev/null 2>&1 || true
    sleep 1
    kill -9 "$client_pid" >/dev/null 2>&1 || true
  fi
  rm -f "$LEGACY_CLIENT_PID_FILE"
fi

rm -f "$LEGACY_CLIENT_STDOUT_LOG" "$LEGACY_CLIENT_STDERR_LOG"

pushd "$ROOT_DIR" >/dev/null
docker_compose down --remove-orphans
popd >/dev/null

echo "System stopped."
