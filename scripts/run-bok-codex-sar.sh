#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SCRIPT_PATH="$SCRIPT_DIR/$(basename "$0")"
SIE_ROOT="${TEXTUS_SIE_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-semantic-integration-engine}"
SCRAPER_ROOT="${TEXTUS_SCRAPER_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-scraper}"
CNCF_BIN="${CNCF_BIN:-$(command -v cncf || true)}"
CNCF_VERSION="${CNCF_VERSION:-0.5.1-SNAPSHOT}"
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-18005}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
TEXTUS_SIE_RDF_DB="${TEXTUS_SIE_RDF_DB:-in-memory}"
TEXTUS_SIE_VECTOR_DB="${TEXTUS_SIE_VECTOR_DB:-in-memory}"
TEXTUS_SIE_FUSEKI_ENDPOINT="${TEXTUS_SIE_FUSEKI_ENDPOINT:-http://127.0.0.1:9030}"
TEXTUS_SIE_FUSEKI_DATASET="${TEXTUS_SIE_FUSEKI_DATASET:-ds}"
TEXTUS_SIE_CHROMA_ENDPOINT="${TEXTUS_SIE_CHROMA_ENDPOINT:-http://127.0.0.1:8081}"
TEXTUS_SIE_CHROMA_COLLECTION="${TEXTUS_SIE_CHROMA_COLLECTION:-simplemodeling}"
TEXTUS_SIE_PROVIDER_TIMEOUT_SECONDS="${TEXTUS_SIE_PROVIDER_TIMEOUT_SECONDS:-10}"
TEXTUS_SIE_EMBEDDING_PROTOCOL_VERSION="${TEXTUS_SIE_EMBEDDING_PROTOCOL_VERSION:-textus.sie.embedding.v1}"
TEXTUS_SIE_EMBEDDING_MODEL="${TEXTUS_SIE_EMBEDDING_MODEL:-deterministic-sha256-v1}"
TEXTUS_SIE_EMBEDDING_DIMENSIONS="${TEXTUS_SIE_EMBEDDING_DIMENSIONS:-128}"
TEXTUS_SIE_EMBEDDING_REQUEST_BATCH_SIZE="${TEXTUS_SIE_EMBEDDING_REQUEST_BATCH_SIZE:-128}"
TEXTUS_SIE_EMBEDDING_TIMEOUT_SECONDS="${TEXTUS_SIE_EMBEDDING_TIMEOUT_SECONDS:-30}"
VIRTUAL_START_AT="${TEXTUS_BOK_CODEX_VIRTUAL_START_AT:-}"
PROBE_QUERY="${TEXTUS_BOK_CODEX_PROBE_QUERY:-Component}"
PROBE_CATEGORY="${TEXTUS_BOK_CODEX_PROBE_CATEGORY:-architecture}"
PROBE_SOURCE_ID="${TEXTUS_BOK_CODEX_PROBE_SOURCE_ID:-codex-bok}"
PROBE_DATASET_ID="${TEXTUS_BOK_CODEX_PROBE_DATASET_ID:-codex-bok}"
PROBE_GENERATION="${TEXTUS_BOK_CODEX_PROBE_GENERATION:-2026-07-21T00:00:00Z}"
PROBE_TIMEOUT_SECONDS="${TEXTUS_BOK_CODEX_PROBE_TIMEOUT_SECONDS:-120}"
STARTUP_TIMEOUT_SECONDS="${TEXTUS_BOK_CODEX_STARTUP_TIMEOUT_SECONDS:-900}"
SHUTDOWN_TIMEOUT_SECONDS="${TEXTUS_BOK_CODEX_SHUTDOWN_TIMEOUT_SECONDS:-30}"
RUN_DIR="${TEXTUS_BOK_CODEX_RUN_DIR:-$HOME/.cncf/textus-bok-codex}"
LAUNCH_LABEL="${TEXTUS_BOK_CODEX_LAUNCH_LABEL:-org.textus.bok-codex-sar}"
PID_FILE="$RUN_DIR/server.pid"
READY_FILE="$RUN_DIR/ready"
CONFIG_FILE="$RUN_DIR/config"
FAILURE_FILE="$RUN_DIR/failure"
SERVER_LOG="$RUN_DIR/server.log"
COMPONENT_DIR="$RUN_DIR/component.d"
SAR_ROOT="$RUN_DIR/textus-bok-codex.sar.d"
SAR_FILE="$COMPONENT_DIR/textus-bok-codex.sar"
SIE_CAR="$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car"
BOK_CAR="$PROJECT_ROOT/target/textus-bok-0.1.0-SNAPSHOT.car"
SCRAPER_CAR="$SCRAPER_ROOT/target/textus-scraper-0.1.1-SNAPSHOT.car"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/bok-codex-sar/subsystem-descriptor.yaml"
FIXTURE_ROOT="${TEXTUS_BOK_CODEX_FIXTURE_ROOT:-$PROJECT_ROOT/examples/bok-codex-sar/fixtures}"
CNCF_RUNTIME_ARGS=(--runtime "$CNCF_VERSION")
SERVER_PID=""
if [[ -n "${CNCF_RUNTIME_DEV_DIR:-}" ]]; then
  CNCF_RUNTIME_ARGS+=(--runtime-dev-dir "$CNCF_RUNTIME_DEV_DIR")
fi
if [[ -n "$VIRTUAL_START_AT" ]]; then
  CNCF_RUNTIME_ARGS+=("--textus.clock.virtual-start-at=$VIRTUAL_START_AT")
fi

case "$RUN_DIR" in
  "" | / | "$HOME")
    echo "Unsafe Textus BoK Codex runtime directory: $RUN_DIR" >&2
    exit 2
    ;;
esac

_is_running() {
  launchctl print "gui/$(id -u)/$LAUNCH_LABEL" >/dev/null 2>&1
}

_requested_config() {
  printf '%s\n' \
    "rdf-db=$TEXTUS_SIE_RDF_DB" \
    "vector-db=$TEXTUS_SIE_VECTOR_DB" \
    "fuseki-endpoint=$TEXTUS_SIE_FUSEKI_ENDPOINT" \
    "fuseki-dataset=$TEXTUS_SIE_FUSEKI_DATASET" \
    "chroma-endpoint=$TEXTUS_SIE_CHROMA_ENDPOINT" \
    "chroma-collection=$TEXTUS_SIE_CHROMA_COLLECTION" \
    "provider-timeout-seconds=$TEXTUS_SIE_PROVIDER_TIMEOUT_SECONDS" \
    "embedding-protocol-version=$TEXTUS_SIE_EMBEDDING_PROTOCOL_VERSION" \
    "embedding-model=$TEXTUS_SIE_EMBEDDING_MODEL" \
    "embedding-dimensions=$TEXTUS_SIE_EMBEDDING_DIMENSIONS" \
    "embedding-request-batch-size=$TEXTUS_SIE_EMBEDDING_REQUEST_BATCH_SIZE" \
    "embedding-timeout-seconds=$TEXTUS_SIE_EMBEDDING_TIMEOUT_SECONDS" \
    "virtual-start-at=$VIRTUAL_START_AT" \
    "probe-query=$PROBE_QUERY" \
    "probe-category=$PROBE_CATEGORY" \
    "probe-source-id=$PROBE_SOURCE_ID" \
    "probe-dataset-id=$PROBE_DATASET_ID" \
    "probe-generation=$PROBE_GENERATION" \
    "probe-timeout-seconds=$PROBE_TIMEOUT_SECONDS" \
    "fixture-root=$FIXTURE_ROOT"
}

_requested_config_fingerprint() {
  _requested_config | LC_ALL=C shasum -a 256 | awk '{print $1}'
}

_build_cars() {
  (cd "$SCRAPER_ROOT" && sbt --batch cozyBuildCAR)
  (cd "$SIE_ROOT" && sbt --batch cozyBuildCAR)
  (cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)
}

_cleanup_failed_start() {
  launchctl remove "$LAUNCH_LABEL" >/dev/null 2>&1 || true
  rm -f "$READY_FILE" "$PID_FILE" "$CONFIG_FILE" "$FAILURE_FILE"
}

_start() {
  mkdir -p "$RUN_DIR"
  if _is_running; then
    if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
      if [[ -s "$CONFIG_FILE" ]] && [[ "$(cat "$CONFIG_FILE")" == "$(_requested_config_fingerprint)" ]]; then
        echo "Textus BoK Codex SAR is already running at $CNCF_HTTP_BASEURL."
        return 0
      fi
      echo "Textus BoK Codex SAR is running with different or unknown provider configuration." >&2
      echo "Use restart to apply the requested configuration." >&2
      return 1
    fi
    launchctl remove "$LAUNCH_LABEL" >/dev/null 2>&1 || true
  fi
  if [[ "${TEXTUS_BOK_CODEX_SKIP_BUILD:-false}" != "true" ]]; then
    _build_cars
  fi
  : >"$SERVER_LOG"
  rm -f "$READY_FILE" "$PID_FILE" "$CONFIG_FILE" "$FAILURE_FILE"
  launchctl remove "$LAUNCH_LABEL" >/dev/null 2>&1 || true
  local launch_command=(
    /usr/bin/env
    "PATH=${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"
    "HOME=$HOME"
    "CNCF_BIN=$CNCF_BIN"
    "CNCF_VERSION=$CNCF_VERSION"
    "CNCF_SERVER_PORT=$CNCF_SERVER_PORT"
    "CNCF_HTTP_BASEURL=$CNCF_HTTP_BASEURL"
    "TEXTUS_SIE_RDF_DB=$TEXTUS_SIE_RDF_DB"
    "TEXTUS_SIE_VECTOR_DB=$TEXTUS_SIE_VECTOR_DB"
    "TEXTUS_SIE_FUSEKI_ENDPOINT=$TEXTUS_SIE_FUSEKI_ENDPOINT"
    "TEXTUS_SIE_FUSEKI_DATASET=$TEXTUS_SIE_FUSEKI_DATASET"
    "TEXTUS_SIE_CHROMA_ENDPOINT=$TEXTUS_SIE_CHROMA_ENDPOINT"
    "TEXTUS_SIE_CHROMA_COLLECTION=$TEXTUS_SIE_CHROMA_COLLECTION"
    "TEXTUS_SIE_PROVIDER_TIMEOUT_SECONDS=$TEXTUS_SIE_PROVIDER_TIMEOUT_SECONDS"
    "TEXTUS_SIE_EMBEDDING_PROTOCOL_VERSION=$TEXTUS_SIE_EMBEDDING_PROTOCOL_VERSION"
    "TEXTUS_SIE_EMBEDDING_MODEL=$TEXTUS_SIE_EMBEDDING_MODEL"
    "TEXTUS_SIE_EMBEDDING_DIMENSIONS=$TEXTUS_SIE_EMBEDDING_DIMENSIONS"
    "TEXTUS_SIE_EMBEDDING_REQUEST_BATCH_SIZE=$TEXTUS_SIE_EMBEDDING_REQUEST_BATCH_SIZE"
    "TEXTUS_SIE_EMBEDDING_TIMEOUT_SECONDS=$TEXTUS_SIE_EMBEDDING_TIMEOUT_SECONDS"
    "TEXTUS_BOK_CODEX_VIRTUAL_START_AT=$VIRTUAL_START_AT"
    "TEXTUS_BOK_CODEX_PROBE_QUERY=$PROBE_QUERY"
    "TEXTUS_BOK_CODEX_PROBE_CATEGORY=$PROBE_CATEGORY"
    "TEXTUS_BOK_CODEX_PROBE_SOURCE_ID=$PROBE_SOURCE_ID"
    "TEXTUS_BOK_CODEX_PROBE_DATASET_ID=$PROBE_DATASET_ID"
    "TEXTUS_BOK_CODEX_PROBE_GENERATION=$PROBE_GENERATION"
    "TEXTUS_BOK_CODEX_PROBE_TIMEOUT_SECONDS=$PROBE_TIMEOUT_SECONDS"
    "TEXTUS_SIE_ROOT=$SIE_ROOT"
    "TEXTUS_SCRAPER_ROOT=$SCRAPER_ROOT"
    "TEXTUS_BOK_CODEX_RUN_DIR=$RUN_DIR"
    "TEXTUS_BOK_CODEX_FIXTURE_ROOT=$FIXTURE_ROOT"
    "TEXTUS_BOK_CODEX_LAUNCH_LABEL=$LAUNCH_LABEL"
  )
  if [[ -n "${JAVA_HOME:-}" ]]; then
    launch_command+=("JAVA_HOME=$JAVA_HOME")
  fi
  if [[ -n "${CNCF_RUNTIME_DEV_DIR:-}" ]]; then
    launch_command+=("CNCF_RUNTIME_DEV_DIR=$CNCF_RUNTIME_DEV_DIR")
  fi
  launch_command+=("$SCRIPT_PATH" serve)
  launchctl submit \
    -l "$LAUNCH_LABEL" \
    -o "$SERVER_LOG" \
    -e "$SERVER_LOG" \
    -- "${launch_command[@]}"
  local deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
  until [[ -s "$READY_FILE" ]]; do
    if [[ -s "$FAILURE_FILE" ]]; then
      echo "Textus BoK Codex SAR failed before readiness." >&2
      tail -n 300 "$SERVER_LOG" >&2
      _cleanup_failed_start
      return 1
    fi
    if ! _is_running; then
      echo "Textus BoK Codex SAR exited before readiness." >&2
      tail -n 300 "$SERVER_LOG" >&2
      _cleanup_failed_start
      return 1
    fi
    if ((SECONDS >= deadline)); then
      echo "Timed out waiting for Textus BoK Codex SAR." >&2
      tail -n 300 "$SERVER_LOG" >&2
      _cleanup_failed_start
      return 1
    fi
    sleep 0.5
  done
  cat "$READY_FILE"
}

_serve() {
  cd "$PROJECT_ROOT"
  echo "$$" >"$PID_FILE"
  _cleanup_serve() {
    local result=$?
    if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" >/dev/null 2>&1; then
      kill "$SERVER_PID" >/dev/null 2>&1 || true
      wait "$SERVER_PID" >/dev/null 2>&1 || true
    fi
    rm -f "$READY_FILE" "$PID_FILE" "$CONFIG_FILE"
    if ((result != 0)); then
      printf 'serve-exit=%s\n' "$result" >"$FAILURE_FILE"
    fi
    return "$result"
  }
  trap _cleanup_serve EXIT INT TERM

  case "$CNCF_HTTP_BASEURL" in
    http://127.0.0.1:* | http://localhost:*) ;;
    *) echo "The BoK Codex SAR requires a loopback URL: $CNCF_HTTP_BASEURL" >&2; return 1 ;;
  esac
  for required in "$CNCF_BIN" "$SAR_DESCRIPTOR" "$FIXTURE_ROOT/metadata/cncf/knowledge-source.json" "$SIE_CAR" "$BOK_CAR" "$SCRAPER_CAR"; do
    [[ -e "$required" ]] || { echo "Required BoK Codex SAR input is missing: $required" >&2; return 1; }
  done
  if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    echo "A server already responds at $CNCF_HTTP_BASEURL." >&2
    return 1
  fi

  rm -rf "$COMPONENT_DIR" "$SAR_ROOT"
  mkdir -p "$COMPONENT_DIR" "$SAR_ROOT"
  cp "$SIE_CAR" "$BOK_CAR" "$SCRAPER_CAR" "$COMPONENT_DIR/"
  cp "$SAR_DESCRIPTOR" "$SAR_ROOT/subsystem-descriptor.yaml"
  (cd "$SAR_ROOT" && zip -qr "$SAR_FILE" subsystem-descriptor.yaml)

  env \
    CNCF_SERVER_PORT="$CNCF_SERVER_PORT" \
    CNCF_HTTP_BASEURL="$CNCF_HTTP_BASEURL" \
    TEXTUS_SIE_RDF_DB="$TEXTUS_SIE_RDF_DB" \
    TEXTUS_SIE_VECTOR_DB="$TEXTUS_SIE_VECTOR_DB" \
    JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=$TEXTUS_SIE_RDF_DB -Dtextus.sie.vector-db=$TEXTUS_SIE_VECTOR_DB" \
    "$CNCF_BIN" \
      "${CNCF_RUNTIME_ARGS[@]}" \
      "--textus.resource.url.file.roots=$FIXTURE_ROOT" \
      "--textus.subsystem.file=$SAR_FILE" \
      "--textus.repository.dir=component-dir:$COMPONENT_DIR" \
      "--textus.sie.rdf-db=$TEXTUS_SIE_RDF_DB" \
      "--textus.sie.vector-db=$TEXTUS_SIE_VECTOR_DB" \
      "--textus.sie.fuseki.endpoint=$TEXTUS_SIE_FUSEKI_ENDPOINT" \
      "--textus.sie.fuseki.dataset=$TEXTUS_SIE_FUSEKI_DATASET" \
      "--textus.sie.chroma.endpoint=$TEXTUS_SIE_CHROMA_ENDPOINT" \
      "--textus.sie.chroma.collection=$TEXTUS_SIE_CHROMA_COLLECTION" \
      "--textus.sie.provider.timeout-seconds=$TEXTUS_SIE_PROVIDER_TIMEOUT_SECONDS" \
      "--textus.sie.embedding.protocol-version=$TEXTUS_SIE_EMBEDDING_PROTOCOL_VERSION" \
      "--textus.sie.embedding.model=$TEXTUS_SIE_EMBEDDING_MODEL" \
      "--textus.sie.embedding.dimensions=$TEXTUS_SIE_EMBEDDING_DIMENSIONS" \
      "--textus.sie.embedding.request-batch-size=$TEXTUS_SIE_EMBEDDING_REQUEST_BATCH_SIZE" \
      "--textus.sie.embedding.timeout-seconds=$TEXTUS_SIE_EMBEDDING_TIMEOUT_SECONDS" \
      server \
      --no-project-classpath \
      --no-default-components &
  SERVER_PID=$!

  local deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
  until curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; do
    kill -0 "$SERVER_PID" >/dev/null 2>&1 || return 1
    ((SECONDS < deadline)) || return 1
    sleep 0.5
  done
  local expected_rdf_provider="$TEXTUS_SIE_RDF_DB"
  local expected_vector_provider="$TEXTUS_SIE_VECTOR_DB"
  if [[ "$expected_rdf_provider" == "in-memory" ]]; then
    expected_rdf_provider="in-memory-rdf"
  fi
  if [[ "$expected_vector_provider" == "in-memory" ]]; then
    expected_vector_provider="in-memory-vector"
  fi
  "$SCRIPT_DIR/probe-bok-codex-sar.py" \
    --base-url "$CNCF_HTTP_BASEURL" \
    --source-uri "$(cd "$FIXTURE_ROOT" && pwd -P | sed 's#^#file://#')/" \
    --expected-rdf-provider "$expected_rdf_provider" \
    --expected-vector-provider "$expected_vector_provider" \
    --probe-query "$PROBE_QUERY" \
    --probe-category "$PROBE_CATEGORY" \
    --source-id "$PROBE_SOURCE_ID" \
    --dataset-id "$PROBE_DATASET_ID" \
    --generation "$PROBE_GENERATION" \
    --timeout "$PROBE_TIMEOUT_SECONDS"
  _requested_config_fingerprint >"$CONFIG_FILE"
  printf 'Textus BoK Codex SAR ready: %s/mcp\n' "$CNCF_HTTP_BASEURL" >"$READY_FILE"
  wait "$SERVER_PID"
}

_stop() {
  if ! _is_running; then
    rm -f "$PID_FILE" "$READY_FILE" "$CONFIG_FILE" "$FAILURE_FILE"
    echo "Textus BoK Codex SAR is not running."
    return 0
  fi
  launchctl remove "$LAUNCH_LABEL"
  local deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
  while _is_running || curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; do
    ((SECONDS < deadline)) || { echo "Timed out stopping Textus BoK Codex SAR." >&2; return 1; }
    sleep 0.25
  done
  echo "Textus BoK Codex SAR stopped."
}

_status() {
  if _is_running && curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    echo "Textus BoK Codex SAR ready: $CNCF_HTTP_BASEURL/mcp"
  else
    echo "Textus BoK Codex SAR is not running."
    return 1
  fi
}

case "${1:-status}" in
  start) _start ;;
  serve) _serve ;;
  stop) _stop ;;
  restart) _stop; _start ;;
  status) _status ;;
  *) echo "Usage: $0 {start|stop|restart|status}" >&2; exit 2 ;;
esac
