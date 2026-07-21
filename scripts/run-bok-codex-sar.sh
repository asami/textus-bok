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
STARTUP_TIMEOUT_SECONDS="${TEXTUS_BOK_CODEX_STARTUP_TIMEOUT_SECONDS:-240}"
SHUTDOWN_TIMEOUT_SECONDS="${TEXTUS_BOK_CODEX_SHUTDOWN_TIMEOUT_SECONDS:-30}"
RUN_DIR="${TEXTUS_BOK_CODEX_RUN_DIR:-$HOME/.cncf/textus-bok-codex}"
LAUNCH_LABEL="${TEXTUS_BOK_CODEX_LAUNCH_LABEL:-org.textus.bok-codex-sar}"
PID_FILE="$RUN_DIR/server.pid"
READY_FILE="$RUN_DIR/ready"
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

case "$RUN_DIR" in
  "" | / | "$HOME")
    echo "Unsafe Textus BoK Codex runtime directory: $RUN_DIR" >&2
    exit 2
    ;;
esac

_is_running() {
  launchctl print "gui/$(id -u)/$LAUNCH_LABEL" >/dev/null 2>&1
}

_build_cars() {
  (cd "$SCRAPER_ROOT" && sbt --batch cozyBuildCAR)
  (cd "$SIE_ROOT" && sbt --batch cozyBuildCAR)
  (cd "$PROJECT_ROOT" && sbt --batch cozyBuildCAR)
}

_cleanup_failed_start() {
  launchctl remove "$LAUNCH_LABEL" >/dev/null 2>&1 || true
  rm -f "$READY_FILE" "$PID_FILE"
}

_start() {
  mkdir -p "$RUN_DIR"
  if _is_running; then
    if curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
      echo "Textus BoK Codex SAR is already running at $CNCF_HTTP_BASEURL."
      return 0
    fi
    launchctl remove "$LAUNCH_LABEL" >/dev/null 2>&1 || true
  fi
  if [[ "${TEXTUS_BOK_CODEX_SKIP_BUILD:-false}" != "true" ]]; then
    _build_cars
  fi
  : >"$SERVER_LOG"
  rm -f "$READY_FILE" "$PID_FILE"
  launchctl remove "$LAUNCH_LABEL" >/dev/null 2>&1 || true
  local launch_command=(
    /usr/bin/env
    "PATH=${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"
    "HOME=$HOME"
    "CNCF_BIN=$CNCF_BIN"
    "CNCF_VERSION=$CNCF_VERSION"
    "CNCF_SERVER_PORT=$CNCF_SERVER_PORT"
    "CNCF_HTTP_BASEURL=$CNCF_HTTP_BASEURL"
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
    if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" >/dev/null 2>&1; then
      kill "$SERVER_PID" >/dev/null 2>&1 || true
      wait "$SERVER_PID" >/dev/null 2>&1 || true
    fi
    rm -f "$READY_FILE" "$PID_FILE"
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
    TEXTUS_SIE_RDF_DB="in-memory" \
    TEXTUS_SIE_VECTOR_DB="in-memory" \
    JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=in-memory -Dtextus.sie.vector-db=in-memory" \
    "$CNCF_BIN" \
      "${CNCF_RUNTIME_ARGS[@]}" \
      "--textus.resource.url.file.roots=$FIXTURE_ROOT" \
      "--textus.subsystem=textus-bok-codex" \
      server \
      --no-project-classpath \
      --component-dir "$COMPONENT_DIR" &
  SERVER_PID=$!

  local deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
  until curl -fsS "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; do
    kill -0 "$SERVER_PID" >/dev/null 2>&1 || return 1
    ((SECONDS < deadline)) || return 1
    sleep 0.5
  done
  "$SCRIPT_DIR/probe-bok-codex-sar.py" \
    --base-url "$CNCF_HTTP_BASEURL" \
    --source-uri "$(cd "$FIXTURE_ROOT" && pwd -P | sed 's#^#file://#')/"
  printf 'Textus BoK Codex SAR ready: %s/mcp\n' "$CNCF_HTTP_BASEURL" >"$READY_FILE"
  wait "$SERVER_PID"
}

_stop() {
  if ! _is_running; then
    rm -f "$PID_FILE" "$READY_FILE"
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
