#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
STATE_DIR="$SCRIPT_DIR/.run"
CLIENT_STATE_DIR="$STATE_DIR/clients"
CLIENT_JAR="$ROOT_DIR/client-module/target/client-1.0-SNAPSHOT.jar"

mkdir -p "$CLIENT_STATE_DIR"

ensure_client_jar() {
  if [[ -f "$CLIENT_JAR" ]]; then
    return
  fi

  pushd "$ROOT_DIR" >/dev/null
  mvn -q -pl client-module -am package -DskipTests
  popd >/dev/null

  if [[ ! -f "$CLIENT_JAR" ]]; then
    echo "Client jar not found: $CLIENT_JAR" >&2
    exit 1
  fi
}

test_server_ready() {
  (echo > /dev/tcp/127.0.0.1/8080) >/dev/null 2>&1
}

echo "Waiting for server on localhost:8080..."
ready=0
for _ in $(seq 1 60); do
  if test_server_ready; then
    ready=1
    break
  fi
  sleep 2
done

if [[ "$ready" -ne 1 ]]; then
  echo "Server is not ready on localhost:8080. Run run.sh first." >&2
  exit 1
fi

ensure_client_jar

echo "Starting client..."
nohup java -jar "$CLIENT_JAR" >/dev/null 2>&1 &
client_pid=$!
disown 2>/dev/null || true
echo "$client_pid" > "$CLIENT_STATE_DIR/client-$client_pid.pid"

echo "Client PID: $client_pid"
echo "Launch client.sh again to open another client instance."
