#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIE_ROOT="${TEXTUS_SIE_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-semantic-integration-engine}"
SCRAPER_ROOT="${TEXTUS_SCRAPER_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-scraper}"
CNCF_BIN="${CNCF_BIN:-$(command -v cncf || true)}"
CNCF_VERSION="${CNCF_VERSION:-0.5.1-SNAPSHOT}"
CNCF_RUNTIME_ARGS=(--runtime "$CNCF_VERSION")
if [[ -n "${CNCF_RUNTIME_DEV_DIR:-}" ]]; then
  CNCF_RUNTIME_ARGS+=(--runtime-dev-dir "$CNCF_RUNTIME_DEV_DIR")
fi
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19545}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
STARTUP_TIMEOUT_SECONDS="${BOK_MIGRATION_SAR_STARTUP_TIMEOUT_SECONDS:-120}"
SHUTDOWN_TIMEOUT_SECONDS="${BOK_MIGRATION_SAR_SHUTDOWN_TIMEOUT_SECONDS:-30}"
SIE_CAR="$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car"
BOK_CAR="$PROJECT_ROOT/target/textus-bok-0.1.0-SNAPSHOT.car"
SCRAPER_CAR="$SCRAPER_ROOT/target/textus-scraper-0.1.1-SNAPSHOT.car"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/bok-migration-sar/subsystem-descriptor.yaml"
FIXTURE_ROOT="$PROJECT_ROOT/examples/bok-migration-sar/fixtures"

case "$CNCF_HTTP_BASEURL" in
  http://127.0.0.1:* | http://localhost:*) ;;
  *)
    echo "The migration SAR check requires a loopback HTTP base URL: $CNCF_HTTP_BASEURL" >&2
    exit 1
    ;;
esac

for required in "$CNCF_BIN" "$SIE_ROOT/project.yaml" "$SCRAPER_ROOT/project.yaml" "$SAR_DESCRIPTOR" "$FIXTURE_ROOT/metadata/cncf/knowledge-source.json"; do
  if [[ ! -e "$required" ]]; then
    echo "Required migration SAR input is missing: $required" >&2
    exit 1
  fi
done
if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
  echo "A server already responds at $CNCF_HTTP_BASEURL; refusing to reuse it." >&2
  exit 1
fi

(cd "$SCRAPER_ROOT" && sbt --batch cozyBuildCAR)
(cd "$SIE_ROOT" && sbt --batch cozyBuildCAR)
(cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)

runtime_dir="$(mktemp -d "${TMPDIR:-/tmp}/textus-bok-migration-sar.XXXXXX")"
component_dir="$runtime_dir/component.d"
sar_root="$runtime_dir/textus-bok-migration.sar.d"
sar_file="$component_dir/textus-bok-migration.sar"
server_log="$runtime_dir/server.log"
server_pid=""
server_listener_pid=""

stop_server() {
  local deadline
  if [[ -n "$server_listener_pid" ]] && kill -0 "$server_listener_pid" >/dev/null 2>&1; then
    kill "$server_listener_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" >/dev/null 2>&1; then
    kill "$server_pid" >/dev/null 2>&1 || true
  fi
  if [[ -n "$server_pid" ]]; then
    wait "$server_pid" >/dev/null 2>&1 || true
  fi
  deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
  while curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; do
    if ((SECONDS >= deadline)); then
      echo "Timed out waiting for the migration SAR server to stop." >&2
      return 1
    fi
    sleep 0.25
  done
}

cleanup() {
  stop_server || true
  rm -rf "$runtime_dir"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p "$component_dir" "$sar_root"
cp "$SIE_CAR" "$BOK_CAR" "$SCRAPER_CAR" "$component_dir/"
cp "$SAR_DESCRIPTOR" "$sar_root/subsystem-descriptor.yaml"
(cd "$sar_root" && zip -qr "$sar_file" subsystem-descriptor.yaml)

env \
  CNCF_SERVER_PORT="$CNCF_SERVER_PORT" \
  CNCF_HTTP_BASEURL="$CNCF_HTTP_BASEURL" \
  TEXTUS_SIE_RDF_DB="in-memory" \
  TEXTUS_SIE_VECTOR_DB="in-memory" \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=in-memory -Dtextus.sie.vector-db=in-memory" \
  "$CNCF_BIN" \
    "${CNCF_RUNTIME_ARGS[@]}" \
    "--textus.resource.url.file.roots=$FIXTURE_ROOT" \
    "--textus.subsystem=textus-bok-migration" \
    server \
    --no-project-classpath \
    --component-dir "$component_dir" >"$server_log" 2>&1 &
server_pid=$!

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
until curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; do
  if ! kill -0 "$server_pid" >/dev/null 2>&1; then
    echo "The migration SAR server exited before readiness." >&2
    tail -n 300 "$server_log" >&2
    exit 1
  fi
  if ((SECONDS >= deadline)); then
    echo "Timed out waiting for the migration SAR server." >&2
    tail -n 300 "$server_log" >&2
    exit 1
  fi
  sleep 0.5
done

server_listener_pid="$(lsof -tiTCP:"$CNCF_SERVER_PORT" -sTCP:LISTEN 2>/dev/null || true)"
server_listener_pid="${server_listener_pid%%$'\n'*}"
if [[ ! "$server_listener_pid" =~ ^[0-9]+$ ]]; then
  echo "Could not identify the owned migration SAR listener." >&2
  tail -n 300 "$server_log" >&2
  exit 1
fi

if ! "$SCRIPT_DIR/probe-bok-migration-sar.py" \
  --base-url "$CNCF_HTTP_BASEURL" \
  --source-uri "$(cd "$FIXTURE_ROOT" && pwd -P | sed 's#^#file://#')/"; then
  tail -n 300 "$server_log" >&2
  exit 1
fi

echo "BOK_MIGRATION_SAR_LIFECYCLE_OK runtime=$CNCF_VERSION"
