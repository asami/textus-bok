#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIE_ROOT="${TEXTUS_SIE_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-semantic-integration-engine}"
SCRAPER_ROOT="${TEXTUS_SCRAPER_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-scraper}"
CNCF_BIN="${CNCF_BIN:-$(command -v cncf || true)}"
LSOF_BIN="$(command -v lsof || true)"
PYTHON_BIN="${PYTHON_BIN:-$(command -v python3 || true)}"
CNCF_VERSION="${CNCF_VERSION:-0.5.2-SNAPSHOT}"
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19547}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
STARTUP_TIMEOUT_SECONDS="${BOK_PROFILE_SELECTION_SAR_STARTUP_TIMEOUT_SECONDS:-120}"
SHUTDOWN_TIMEOUT_SECONDS="${BOK_PROFILE_SELECTION_SAR_SHUTDOWN_TIMEOUT_SECONDS:-30}"
SIE_CAR="${TEXTUS_SIE_CAR:-$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car}"
BOK_CAR="${TEXTUS_BOK_CAR:-$PROJECT_ROOT/target/textus-bok-0.1.0-SNAPSHOT.car}"
SCRAPER_CAR="${TEXTUS_SCRAPER_CAR:-$SCRAPER_ROOT/target/textus-scraper-0.1.1-SNAPSHOT.car}"
SAR_DESCRIPTOR="${TEXTUS_BOK_PROFILE_SELECTION_SAR_DESCRIPTOR:-$PROJECT_ROOT/examples/bok-profile-selection-sar/subsystem-descriptor.yaml}"
FIXTURE_ROOT="${TEXTUS_BOK_PROFILE_SELECTION_SAR_FIXTURE_ROOT:-$PROJECT_ROOT/examples/bok-profile-selection-sar/fixtures}"
CNCF_RUNTIME_ARGS=(--runtime "$CNCF_VERSION")
if [[ -n "${CNCF_RUNTIME_DEV_DIR:-}" ]]; then
  CNCF_RUNTIME_ARGS+=(--runtime-dev-dir "$CNCF_RUNTIME_DEV_DIR")
fi

if [[ ! "$CNCF_SERVER_PORT" =~ ^[0-9]+$ ]] || ((10#$CNCF_SERVER_PORT < 1 || 10#$CNCF_SERVER_PORT > 65535)); then
  echo "The profile-selection SAR requires CNCF_SERVER_PORT to be a decimal integer from 1 through 65535: $CNCF_SERVER_PORT" >&2
  exit 1
fi

if [[ ! "$STARTUP_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || ((10#$STARTUP_TIMEOUT_SECONDS < 1)); then
  echo "The profile-selection SAR requires a positive integer startup timeout: $STARTUP_TIMEOUT_SECONDS" >&2
  exit 1
fi

if [[ ! "$SHUTDOWN_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || ((10#$SHUTDOWN_TIMEOUT_SECONDS < 1)); then
  echo "The profile-selection SAR requires a positive integer shutdown timeout: $SHUTDOWN_TIMEOUT_SECONDS" >&2
  exit 1
fi

if [[ "$CNCF_HTTP_BASEURL" != "http://127.0.0.1:$CNCF_SERVER_PORT" && "$CNCF_HTTP_BASEURL" != "http://localhost:$CNCF_SERVER_PORT" ]]; then
  echo "The profile-selection SAR requires CNCF_HTTP_BASEURL to be exactly http://127.0.0.1:$CNCF_SERVER_PORT or http://localhost:$CNCF_SERVER_PORT: $CNCF_HTTP_BASEURL" >&2
  exit 1
fi

CNCF_BIN="$(command -v "$CNCF_BIN" || true)"
if [[ -z "$CNCF_BIN" || ! -x "$CNCF_BIN" ]]; then
  echo "The profile-selection SAR requires an executable CNCF launcher; set CNCF_BIN if cncf is not suitable." >&2
  exit 1
fi

if [[ -z "$LSOF_BIN" || ! -x "$LSOF_BIN" ]]; then
  echo "The profile-selection SAR requires an executable lsof command." >&2
  exit 1
fi

PYTHON_BIN="$(command -v "$PYTHON_BIN" || true)"
if [[ -z "$PYTHON_BIN" || ! -x "$PYTHON_BIN" ]]; then
  echo "The profile-selection SAR requires an executable Python interpreter; set PYTHON_BIN if python3 is not suitable." >&2
  exit 1
fi

if ! "$PYTHON_BIN" -c 'import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)'; then
  echo "The profile-selection SAR requires Python 3.10 or newer: $PYTHON_BIN" >&2
  exit 1
fi

for required in \
  "$SIE_CAR" \
  "$BOK_CAR" \
  "$SCRAPER_CAR" \
  "$SAR_DESCRIPTOR" \
  "$FIXTURE_ROOT/official/metadata/cncf/knowledge-source.json" \
  "$FIXTURE_ROOT/official/metadata/glossary/terms.json" \
  "$FIXTURE_ROOT/official/metadata/rdf/graph.json" \
  "$FIXTURE_ROOT/development/metadata/cncf/knowledge-source.json" \
  "$FIXTURE_ROOT/development/metadata/glossary/terms.json" \
  "$FIXTURE_ROOT/development/metadata/rdf/graph.json" \
  "$FIXTURE_ROOT/project-alpha/metadata/cncf/knowledge-source.json" \
  "$FIXTURE_ROOT/project-alpha/metadata/glossary/terms.json" \
  "$FIXTURE_ROOT/project-alpha/metadata/rdf/graph.json" \
  "$FIXTURE_ROOT/project-beta/metadata/cncf/knowledge-source.json" \
  "$FIXTURE_ROOT/project-beta/metadata/glossary/terms.json" \
  "$FIXTURE_ROOT/project-beta/metadata/rdf/graph.json"; do
  if [[ ! -e "$required" ]]; then
    echo "Required profile-selection SAR input is missing: $required" >&2
    exit 1
  fi
done

_listening_pids() {
  local output
  local status

  if output="$("$LSOF_BIN" -nP -tiTCP:"$CNCF_SERVER_PORT" -sTCP:LISTEN 2>&1)"; then
    status=0
  else
    status=$?
  fi
  if ((status == 1)) && [[ -z "$output" ]]; then
    return 0
  fi
  if ((status != 0)); then
    echo "lsof failed while checking listeners on port $CNCF_SERVER_PORT: $output" >&2
    return 1
  fi
  if [[ -z "$output" || ! "$output" =~ ^[0-9]+($'\n'[0-9]+)*$ ]]; then
    echo "lsof returned invalid listener output on port $CNCF_SERVER_PORT: $output" >&2
    return 1
  fi
  printf '%s\n' "$output"
}

if ! occupied_port="$(_listening_pids)"; then
  echo "Could not determine whether port $CNCF_SERVER_PORT is vacant." >&2
  exit 1
fi
if [[ -n "$occupied_port" ]]; then
  echo "Port $CNCF_SERVER_PORT is already occupied; refusing to reuse it." >&2
  exit 1
fi

fixture_root="$(cd "$FIXTURE_ROOT" && pwd -P)"
runtime_root="$PROJECT_ROOT/target"
mkdir -p "$runtime_root"
runtime_dir="$(mktemp -d "$runtime_root/textus-bok-profile-selection-sar.XXXXXX")"
component_dir="$runtime_dir/component.d"
sar_root="$runtime_dir/textus-bok-profile-selection.sar.d"
sar_file="$component_dir/textus-bok-profile-selection.sar"
config_file="$runtime_dir/profile-selection-config.yaml"
server_log="$runtime_dir/server.log"
server_pid=""
server_pid_signature=""
server_listener_pid=""
server_listener_pid_signature=""

_show_diagnostics() {
  if [[ -s "$server_log" ]]; then
    tail -n 300 "$server_log" >&2
  fi
}

_process_signature() {
  local pid="$1"
  local signature

  if [[ ! "$pid" =~ ^[0-9]+$ ]]; then
    return 1
  fi
  signature="$(ps -o lstart= -p "$pid" 2>/dev/null || true)"
  signature="${signature#"${signature%%[![:space:]]*}"}"
  signature="${signature%"${signature##*[![:space:]]}"}"
  if [[ -z "$signature" ]]; then
    return 1
  fi
  printf '%s\n' "$signature"
}

_terminate_captured_process() {
  local role="$1"
  local pid="$2"
  local signature="$3"
  local current

  if [[ -z "$pid" || -z "$signature" ]]; then
    return 0
  fi
  current="$(_process_signature "$pid" || true)"
  if [[ -z "$current" ]]; then
    return 0
  fi
  if [[ "$current" != "$signature" ]]; then
    echo "Refusing to stop $role PID $pid because its captured identity no longer matches." >&2
    return 1
  fi
  if ! kill "$pid" >/dev/null 2>&1; then
    echo "Could not stop captured $role PID $pid." >&2
    return 1
  fi
}

_listener_belongs_to_server() {
  local candidate="$1"
  local parent
  local steps=0
  local max_steps=128

  while [[ "$candidate" =~ ^[0-9]+$ ]] && ((steps < max_steps)); do
    if [[ "$candidate" == "$server_pid" ]]; then
      return 0
    fi
    parent="$(ps -o ppid= -p "$candidate" 2>/dev/null || true)"
    parent="${parent//[[:space:]]/}"
    if [[ ! "$parent" =~ ^[0-9]+$ ]] || [[ "$parent" == "$candidate" ]] || [[ "$parent" == "0" ]]; then
      return 1
    fi
    candidate="$parent"
    steps=$((steps + 1))
  done

  return 1
}

_captured_server_state() {
  local current
  local process_state

  if [[ -z "$server_pid" || -z "$server_pid_signature" ]]; then
    printf '%s\n' "absent"
    return 0
  fi
  current="$(_process_signature "$server_pid" || true)"
  if [[ -z "$current" ]]; then
    printf '%s\n' "absent"
    return 0
  fi
  if [[ "$current" != "$server_pid_signature" ]]; then
    printf '%s\n' "identity-mismatch"
    return 0
  fi
  process_state="$(ps -o stat= -p "$server_pid" 2>/dev/null || true)"
  process_state="${process_state#"${process_state%%[![:space:]]*}"}"
  process_state="${process_state%"${process_state##*[![:space:]]}"}"
  if [[ "$process_state" == Z* ]]; then
    printf '%s\n' "zombie"
  else
    printf '%s\n' "live"
  fi
}

_reap_captured_server() {
  local state

  state="$(_captured_server_state)"
  case "$state" in
    absent|zombie)
      # The captured child is already absent or a zombie, so wait cannot block.
      if [[ -n "$server_pid" ]]; then
        wait "$server_pid" >/dev/null 2>&1 || true
      fi
      server_pid=""
      server_pid_signature=""
      return 0
      ;;
    live)
      return 1
      ;;
    identity-mismatch)
      echo "Refusing to reap profile-selection SAR server PID $server_pid because its captured identity no longer matches." >&2
      return 1
      ;;
    *)
      echo "Could not determine the captured profile-selection SAR server state: $state" >&2
      return 1
      ;;
  esac
}

_stop_server() {
  local deadline
  local listener_pids
  local server_state
  local stop_failed=0

  if [[ -z "$server_pid" && -z "$server_listener_pid" ]]; then
    return 0
  fi

  deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
  _terminate_captured_process "profile-selection SAR listener" "$server_listener_pid" "$server_listener_pid_signature" || stop_failed=1
  _terminate_captured_process "profile-selection SAR server" "$server_pid" "$server_pid_signature" || stop_failed=1
  while :; do
    server_state="$(_captured_server_state)"
    case "$server_state" in
      absent|zombie)
        if ! _reap_captured_server; then
          return 1
        fi
        server_state="absent"
        ;;
      live)
        ;;
      identity-mismatch)
        echo "Captured profile-selection SAR server PID $server_pid changed identity during shutdown; refusing to wait for or stop the reused PID." >&2
        return 1
        ;;
      *)
        echo "Could not determine the captured profile-selection SAR server state: $server_state" >&2
        return 1
        ;;
    esac
    if ! listener_pids="$(_listening_pids)"; then
      return 1
    fi
    if [[ -z "$listener_pids" && "$server_state" == "absent" ]]; then
      if ((stop_failed != 0)); then
        return 1
      fi
      server_listener_pid=""
      server_listener_pid_signature=""
      return 0
    fi
    if ((SECONDS >= deadline)); then
      echo "Timed out shutting down profile-selection SAR: captured server state=$server_state; listener PID(s) on port $CNCF_SERVER_PORT=${listener_pids:-none}." >&2
      return 1
    fi
    sleep 0.25
  done
}

_cleanup() {
  local result=$?
  local stop_result=0

  _stop_server || stop_result=$?
  if [[ -n "$runtime_dir" && -d "$runtime_dir" ]]; then
    rm -rf "$runtime_dir" || {
      echo "Could not remove profile-selection SAR runtime directory: $runtime_dir" >&2
      if ((result == 0)); then
        result=1
      fi
    }
  fi
  if ((result == 0 && stop_result != 0)); then
    result=$stop_result
  fi
  trap - EXIT
  exit "$result"
}
trap _cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p "$component_dir" "$sar_root/component"
cp "$SIE_CAR" "$BOK_CAR" "$SCRAPER_CAR" "$sar_root/component/"
cp "$SAR_DESCRIPTOR" "$sar_root/subsystem-descriptor.yaml"
(cd "$sar_root" && zip -qr "$sar_file" subsystem-descriptor.yaml component)

cat >"$config_file" <<EOF
textus.bok.profile-registry:
  profiles:
    - profile: official
      source:
        sourceId: profile-official-source
        datasetId: profile-official-dataset
        generation: "2026-08-15T00:00:00Z"
        resource: "file://${fixture_root}/official/"
      evidence:
        uri: "https://evidence.example/textus-bok/profile-official"
        sourceId: profile-official-source
    - profile: development
      source:
        sourceId: profile-development-source
        datasetId: profile-development-dataset
        generation: "2026-08-15T01:00:00Z"
        resource: "file://${fixture_root}/development/"
      evidence:
        uri: "https://evidence.example/textus-bok/profile-development"
        sourceId: profile-development-source
    - profile: project
      projectId: project-alpha
      source:
        sourceId: profile-project-alpha-source
        datasetId: profile-project-alpha-dataset
        generation: "2026-08-15T02:00:00Z"
        resource: "file://${fixture_root}/project-alpha/"
      evidence:
        uri: "https://evidence.example/textus-bok/profile-project-alpha"
        sourceId: profile-project-alpha-source
    - profile: project
      projectId: project-beta
      source:
        sourceId: profile-project-beta-source
        datasetId: profile-project-beta-dataset
        generation: "2026-08-15T03:00:00Z"
        resource: "file://${fixture_root}/project-beta/"
      evidence:
        uri: "https://evidence.example/textus-bok/profile-project-beta"
        sourceId: profile-project-beta-source
EOF

env \
  CNCF_SERVER_PORT="$CNCF_SERVER_PORT" \
  CNCF_HTTP_BASEURL="$CNCF_HTTP_BASEURL" \
  TEXTUS_SIE_RDF_DB="in-memory" \
  TEXTUS_SIE_VECTOR_DB="in-memory" \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=in-memory -Dtextus.sie.vector-db=in-memory" \
  "$CNCF_BIN" \
    "${CNCF_RUNTIME_ARGS[@]}" \
    --textus.config.file "$config_file" \
    "--textus.resource.url.file.roots=$fixture_root" \
    "--textus.subsystem=textus-bok-profile-selection" \
    server \
    --no-project-classpath \
    --no-default-components \
    --component-dir "$component_dir" >"$server_log" 2>&1 &
server_pid=$!
server_pid_signature="$(_process_signature "$server_pid" || true)"
if [[ -z "$server_pid_signature" ]]; then
  echo "Could not capture the launched profile-selection SAR server identity." >&2
  _show_diagnostics
  exit 1
fi

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
while :; do
  if ! kill -0 "$server_pid" >/dev/null 2>&1; then
    echo "The profile-selection SAR server exited before readiness." >&2
    _show_diagnostics
    exit 1
  fi
  remaining=$((deadline - SECONDS))
  if ((remaining <= 0)); then
    echo "Timed out waiting for the profile-selection SAR server." >&2
    _show_diagnostics
    exit 1
  fi
  attempt_timeout=$remaining
  if ((attempt_timeout > 5)); then
    attempt_timeout=5
  fi
  if curl -fsS --connect-timeout "$attempt_timeout" --max-time "$attempt_timeout" "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    break
  fi
  remaining=$((deadline - SECONDS))
  if ((remaining <= 0)); then
    echo "Timed out waiting for the profile-selection SAR server." >&2
    _show_diagnostics
    exit 1
  fi
  if ((remaining > 1)); then
    sleep 0.5
  fi
done

if ! listener_pid="$(_listening_pids)"; then
  _show_diagnostics
  exit 1
fi
if [[ ! "$listener_pid" =~ ^[0-9]+$ ]]; then
  echo "Could not identify exactly one numeric profile-selection SAR listener on port $CNCF_SERVER_PORT." >&2
  _show_diagnostics
  exit 1
fi
if ! _listener_belongs_to_server "$listener_pid"; then
  echo "Listener PID $listener_pid on port $CNCF_SERVER_PORT is not owned by launched server PID $server_pid." >&2
  _show_diagnostics
  exit 1
fi
server_listener_pid="$listener_pid"
server_listener_pid_signature="$(_process_signature "$server_listener_pid" || true)"
if [[ -z "$server_listener_pid_signature" ]]; then
  echo "Could not capture the owned profile-selection SAR listener identity." >&2
  _show_diagnostics
  exit 1
fi

if ! "$PYTHON_BIN" "$SCRIPT_DIR/probe-bok-profile-selection-sar.py" \
  --base-url "$CNCF_HTTP_BASEURL" \
  --fixture-root "$fixture_root" \
  --server-pid "$server_pid" \
  --server-pid-signature "$server_pid_signature" \
  --listener-pid "$server_listener_pid" \
  --listener-pid-signature "$server_listener_pid_signature" \
  --configured-port "$CNCF_SERVER_PORT"; then
  _show_diagnostics
  exit 1
fi

if ! _stop_server; then
  _show_diagnostics
  exit 1
fi

echo "BOK_PROFILE_SELECTION_SAR_LIFECYCLE_OK profiles=4"
