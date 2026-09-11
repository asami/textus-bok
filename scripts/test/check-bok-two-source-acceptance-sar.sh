#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
CONFIGURE_SCRIPT="$PROJECT_ROOT/scripts/configure-bok-two-source-acceptance.sh"
PROBE_SCRIPT="$SCRIPT_DIR/probe-bok-two-source.py"
SAR_DESCRIPTOR="$PROJECT_ROOT/examples/bok-two-source-acceptance-sar/subsystem-descriptor.yaml"
CNCF_BIN="${CNCF_BIN:-$(command -v cncf || true)}"
PYTHON_BIN="${PYTHON_BIN:-$(command -v python3 || true)}"
LSOF_BIN="${LSOF_BIN:-$(command -v lsof || true)}"
CURL_BIN="${CURL_BIN:-$(command -v curl || true)}"
CNCF_VERSION="${CNCF_VERSION:-0.5.3-SNAPSHOT}"
CNCF_SERVER_PORT="${CNCF_SERVER_PORT:-19548}"
CNCF_HTTP_BASEURL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$CNCF_SERVER_PORT}"
STARTUP_TIMEOUT_SECONDS="${BOK_TWO_SOURCE_ACCEPTANCE_STARTUP_TIMEOUT_SECONDS:-120}"
SHUTDOWN_TIMEOUT_SECONDS="${BOK_TWO_SOURCE_ACCEPTANCE_SHUTDOWN_TIMEOUT_SECONDS:-30}"
BOK_CAR="${TEXTUS_BOK_CAR:-$PROJECT_ROOT/target/textus-bok-0.1.0-SNAPSHOT.car}"
SIE_CAR="${TEXTUS_SIE_CAR:-/Users/asami/src/dev2026/textus-semantic-integration-engine/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car}"
SCRAPER_CAR="${TEXTUS_SCRAPER_CAR:-/Users/asami/src/dev2026/textus-scraper/target/textus-scraper-0.1.1-SNAPSHOT.car}"
SIMPLEMODELING_SOURCE_ROOT=""
KNOWLEDGEHUB_SOURCE_ROOT=""
CNCF_RUNTIME_ARGS=(--runtime "$CNCF_VERSION")
if [[ -n "${CNCF_RUNTIME_DEV_DIR:-}" ]]; then
  CNCF_RUNTIME_ARGS+=(--runtime-dev-dir "$CNCF_RUNTIME_DEV_DIR")
fi

usage() {
  printf '%s\n' "Usage: $0 --simplemodeling-source-root PATH --knowledgehub-source-root PATH"
  printf '%s\n' "  Run the bounded, loopback-only two-source BoK catalog acceptance SAR."
}

while (($# > 0)); do
  case "$1" in
    --simplemodeling-source-root)
      [[ $# -ge 2 ]] || { echo "--simplemodeling-source-root requires a path" >&2; exit 2; }
      SIMPLEMODELING_SOURCE_ROOT="$2"
      shift 2
      ;;
    --knowledgehub-source-root)
      [[ $# -ge 2 ]] || { echo "--knowledgehub-source-root requires a path" >&2; exit 2; }
      KNOWLEDGEHUB_SOURCE_ROOT="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

[[ -n "$SIMPLEMODELING_SOURCE_ROOT" ]] || { echo "--simplemodeling-source-root is required" >&2; usage >&2; exit 2; }
[[ -n "$KNOWLEDGEHUB_SOURCE_ROOT" ]] || { echo "--knowledgehub-source-root is required" >&2; usage >&2; exit 2; }
[[ "$CNCF_SERVER_PORT" =~ ^[0-9]+$ ]] && ((10#$CNCF_SERVER_PORT >= 1 && 10#$CNCF_SERVER_PORT <= 65535)) || { echo "CNCF_SERVER_PORT must be a decimal port from 1 through 65535" >&2; exit 1; }
[[ "$STARTUP_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] && ((10#$STARTUP_TIMEOUT_SECONDS > 0)) || { echo "startup timeout must be positive" >&2; exit 1; }
[[ "$SHUTDOWN_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] && ((10#$SHUTDOWN_TIMEOUT_SECONDS > 0)) || { echo "shutdown timeout must be positive" >&2; exit 1; }
[[ "$CNCF_HTTP_BASEURL" == "http://127.0.0.1:$CNCF_SERVER_PORT" ]] || { echo "CNCF_HTTP_BASEURL must be exactly http://127.0.0.1:$CNCF_SERVER_PORT" >&2; exit 1; }

for command_path in "$CNCF_BIN" "$PYTHON_BIN" "$LSOF_BIN" "$CURL_BIN"; do
  [[ -n "$command_path" ]] || { echo "A required command is unavailable" >&2; exit 1; }
  command -v "$command_path" >/dev/null 2>&1 || { echo "Required command is unavailable: $command_path" >&2; exit 1; }
done
CNCF_BIN="$(command -v "$CNCF_BIN")"
PYTHON_BIN="$(command -v "$PYTHON_BIN")"
LSOF_BIN="$(command -v "$LSOF_BIN")"
CURL_BIN="$(command -v "$CURL_BIN")"
"$PYTHON_BIN" -c 'import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)' || { echo "Python 3.10 or newer is required" >&2; exit 1; }

_regular_file() {
  [[ -f "$1" && ! -L "$1" ]] || { echo "Required input must be a regular non-symlink file: $1" >&2; exit 1; }
}

_real_directory() {
  [[ -d "$1" && ! -L "$1" ]] || { echo "Required source root must be a real directory: $1" >&2; exit 1; }
  "$PYTHON_BIN" - "$1" <<'PY'
import sys
from pathlib import Path

root = Path(sys.argv[1])
if not root.is_absolute() or root.is_symlink() or not root.is_dir():
    raise SystemExit(1)
print(root.resolve(strict=True))
PY
}

_regular_file "$CONFIGURE_SCRIPT"
_regular_file "$PROBE_SCRIPT"
_regular_file "$SAR_DESCRIPTOR"
_regular_file "$BOK_CAR"
_regular_file "$SIE_CAR"
_regular_file "$SCRAPER_CAR"
SIMPLEMODELING_SOURCE_ROOT="$(_real_directory "$SIMPLEMODELING_SOURCE_ROOT")" || { echo "SimpleModeling source root must be an explicit absolute real directory" >&2; exit 1; }
KNOWLEDGEHUB_SOURCE_ROOT="$(_real_directory "$KNOWLEDGEHUB_SOURCE_ROOT")" || { echo "KnowledgeHub source root must be an explicit absolute real directory" >&2; exit 1; }
[[ "$SIMPLEMODELING_SOURCE_ROOT" != "$KNOWLEDGEHUB_SOURCE_ROOT" ]] || { echo "The two source roots must be distinct" >&2; exit 1; }

_listening_pids() {
  local output status
  if output="$($LSOF_BIN -nP -tiTCP:"$CNCF_SERVER_PORT" -sTCP:LISTEN 2>&1)"; then
    status=0
  else
    status=$?
  fi
  if ((status == 1)) && [[ -z "$output" ]]; then
    return 0
  fi
  ((status == 0)) || { echo "lsof failed while checking port $CNCF_SERVER_PORT: $output" >&2; return 1; }
  [[ "$output" =~ ^[0-9]+($'\n'[0-9]+)*$ ]] || { echo "lsof returned invalid listener output: $output" >&2; return 1; }
  printf '%s\n' "$output"
}

occupied_port="$(_listening_pids)" || exit 1
[[ -z "$occupied_port" ]] || { echo "Port $CNCF_SERVER_PORT is already occupied; refusing to reuse it" >&2; exit 1; }

_verified_runtime_root() {
  "$PYTHON_BIN" - "$PROJECT_ROOT" "$1" <<'PY'
import sys
from pathlib import Path

project_root = Path(sys.argv[1]).resolve(strict=True)
runtime_root = Path(sys.argv[2])
expected_root = project_root / "target"
if runtime_root != expected_root or runtime_root.is_symlink() or not runtime_root.is_dir():
    raise SystemExit(1)
resolved_root = runtime_root.resolve(strict=True)
if resolved_root != expected_root:
    raise SystemExit(1)
print(resolved_root)
PY
}

_verified_runtime_directory() {
  "$PYTHON_BIN" - "$1" "$2" <<'PY'
import sys
from pathlib import Path

runtime_root = Path(sys.argv[1])
runtime_dir = Path(sys.argv[2])
if runtime_root.is_symlink() or not runtime_root.is_dir():
    raise SystemExit(1)
resolved_root = runtime_root.resolve(strict=True)
if resolved_root != runtime_root:
    raise SystemExit(1)
if runtime_dir.is_symlink() or not runtime_dir.is_dir():
    raise SystemExit(1)
resolved_dir = runtime_dir.resolve(strict=True)
if resolved_dir.parent != resolved_root or not resolved_dir.name.startswith("textus-bok-two-source-acceptance-sar."):
    raise SystemExit(1)
print(resolved_dir)
PY
}

runtime_root="$PROJECT_ROOT/target"
if [[ -L "$runtime_root" || (-e "$runtime_root" && ! -d "$runtime_root") ]]; then
  echo "Two-source SAR target must be a non-symlink directory: $runtime_root" >&2
  exit 1
fi
mkdir -p "$runtime_root"
runtime_root="$(_verified_runtime_root "$runtime_root")" || {
  echo "Two-source SAR target must physically be the repository target directory" >&2
  exit 1
}
runtime_dir="$(mktemp -d "$runtime_root/textus-bok-two-source-acceptance-sar.XXXXXX")"
runtime_dir="$(_verified_runtime_directory "$runtime_root" "$runtime_dir")" || {
  echo "Two-source SAR runtime directory escaped the verified target root" >&2
  exit 1
}
component_dir="$runtime_dir/component.d"
sar_root="$runtime_dir/textus-bok-two-source-acceptance.sar.d"
sar_file="$component_dir/textus-bok-two-source-acceptance.sar"
config_file="$runtime_dir/two-source-config.yaml"
server_log="$runtime_dir/server.log"
server_pid=""
server_signature=""
listener_pid=""
listener_signature=""

_process_signature() {
  local signature
  [[ "$1" =~ ^[0-9]+$ ]] || return 1
  signature="$(ps -o lstart= -p "$1" 2>/dev/null || true)"
  signature="${signature#"${signature%%[![:space:]]*}"}"
  signature="${signature%"${signature##*[![:space:]]}"}"
  [[ -n "$signature" ]] || return 1
  printf '%s\n' "$signature"
}

_captured_process_is_live() {
  local current
  [[ -n "$1" && -n "$2" ]] || return 1
  current="$(_process_signature "$1" || true)"
  [[ "$current" == "$2" ]]
}

_listener_belongs_to_server() {
  local candidate="$1" parent steps=0
  while [[ "$candidate" =~ ^[0-9]+$ ]] && ((steps < 128)); do
    [[ "$candidate" == "$server_pid" ]] && return 0
    parent="$(ps -o ppid= -p "$candidate" 2>/dev/null || true)"
    parent="${parent//[[:space:]]/}"
    [[ "$parent" =~ ^[0-9]+$ && "$parent" != "$candidate" && "$parent" != 0 ]] || return 1
    candidate="$parent"
    steps=$((steps + 1))
  done
  return 1
}

_stop_captured_process() {
  local deadline current_listener
  deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
  if _captured_process_is_live "$server_pid" "$server_signature"; then
    kill "$server_pid" >/dev/null 2>&1 || true
  fi
  while ((SECONDS < deadline)); do
    if ! _captured_process_is_live "$server_pid" "$server_signature"; then
      wait "$server_pid" >/dev/null 2>&1 || true
      server_pid=""
      server_signature=""
      break
    fi
    sleep 0.25
  done
  if [[ -n "$server_pid" ]]; then
    echo "Timed out stopping captured two-source SAR server PID $server_pid" >&2
    return 1
  fi
  current_listener="$(_listening_pids)" || return 1
  if [[ -n "$current_listener" ]]; then
    if _captured_process_is_live "$listener_pid" "$listener_signature"; then
      kill "$listener_pid" >/dev/null 2>&1 || true
      deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
      while _captured_process_is_live "$listener_pid" "$listener_signature" && ((SECONDS < deadline)); do
        sleep 0.25
      done
      current_listener="$(_listening_pids)" || return 1
    fi
    [[ -z "$current_listener" ]] || { echo "The captured two-source SAR listener did not stop on port $CNCF_SERVER_PORT: $current_listener" >&2; return 1; }
  fi
  listener_pid=""
  listener_signature=""
}

_cleanup() {
  local status=$? stop_status=0 cleanup_dir
  _stop_captured_process || stop_status=$?
  if [[ -e "$runtime_dir" || -L "$runtime_dir" ]]; then
    cleanup_dir="$(_verified_runtime_directory "$runtime_root" "$runtime_dir" || true)"
    if [[ -n "$cleanup_dir" ]]; then
      rm -rf -- "$cleanup_dir" || stop_status=1
    else
      echo "Refusing to remove runtime directory outside the verified target root: $runtime_dir" >&2
      stop_status=1
    fi
  fi
  trap - EXIT
  if ((status == 0 && stop_status != 0)); then
    exit "$stop_status"
  fi
  exit "$status"
}
trap _cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

"$CONFIGURE_SCRIPT" \
  --simplemodeling-source-root "$SIMPLEMODELING_SOURCE_ROOT" \
  --knowledgehub-source-root "$KNOWLEDGEHUB_SOURCE_ROOT" \
  --output "$config_file"
grep -Fqx 'textus.bok.knowledge-backend:' "$config_file" || { echo "Two-source runtime configuration does not select the BoK knowledge backend" >&2; exit 1; }
grep -Fqx '  backend: memory' "$config_file" || { echo "Two-source runtime configuration does not select the memory backend" >&2; exit 1; }
if grep -Fq 'textus.sie' "$config_file"; then
  echo "Two-source runtime configuration contains a textus.sie policy" >&2
  exit 1
fi

mkdir -p "$component_dir" "$sar_root/component"
cp "$SIE_CAR" "$sar_root/component/"
cp "$BOK_CAR" "$sar_root/component/"
cp "$SCRAPER_CAR" "$sar_root/component/"
cp "$SAR_DESCRIPTOR" "$sar_root/subsystem-descriptor.yaml"
(cd "$sar_root" && zip -qr "$sar_file" subsystem-descriptor.yaml component)

"$PYTHON_BIN" - "$sar_file" <<'PY'
import io
import json
import re
import sys
import zipfile

sar_path = sys.argv[1]
sie_component_path = "component/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car"
bok_component_path = "component/textus-bok-0.1.0-SNAPSHOT.car"
scraper_component_path = "component/textus-scraper-0.1.1-SNAPSHOT.car"
expected_components = sorted([sie_component_path, bok_component_path, scraper_component_path])


def component_descriptor(car, expected_id, expected_version):
    descriptor = json.loads(car.read("component-descriptor.json").decode("utf-8"))
    component = descriptor.get("component")
    expected = {
        "namespace": "org.simplemodeling.textus",
        "id": expected_id,
        "version": expected_version,
    }
    if component != expected:
        raise SystemExit(f"packaged {expected_id} component descriptor is unexpected: {component}")


with zipfile.ZipFile(sar_path) as sar:
    names = set(sar.namelist())
    descriptor = sar.read("subsystem-descriptor.yaml").decode("utf-8")
    components = sorted(name for name in names if name.startswith("component/") and name.endswith(".car"))
    if components != expected_components:
        raise SystemExit(f"packaged SAR components are unexpected: {components}")
    descriptor_components = re.findall(r"^    id:\s*(\S+)\s*$", descriptor, re.MULTILINE)
    if descriptor_components != ["SemanticIntegrationEngine", "Bok", "Scraper"]:
        raise SystemExit(f"SAR descriptor components are unexpected: {descriptor_components}")
    sie_component = re.search(
        r"^  - namespace: org\.simplemodeling\.textus\n    id: SemanticIntegrationEngine\n.*?(?=^  - |\\Z)",
        descriptor,
        re.MULTILINE | re.DOTALL,
    )
    if sie_component is None or re.search(
        r'^      cncf\.mcp\.enabled:\s*"false"\s*$',
        sie_component.group(0),
        re.MULTILINE,
    ) is None:
        raise SystemExit('packaged SAR descriptor does not give the SIE sibling cncf.mcp.enabled: "false"')
    with zipfile.ZipFile(io.BytesIO(sar.read(sie_component_path))) as sie_car:
        component_descriptor(sie_car, "SemanticIntegrationEngine", "0.2.0-SNAPSHOT")
        sie_api = json.loads(sie_car.read("component-api-descriptor.json").decode("utf-8"))
    with zipfile.ZipFile(io.BytesIO(sar.read(bok_component_path))) as bok_car:
        component_descriptor(bok_car, "Bok", "0.1.0-SNAPSHOT")
        bok_api = json.loads(bok_car.read("component-api-descriptor.json").decode("utf-8"))
    with zipfile.ZipFile(io.BytesIO(sar.read(scraper_component_path))) as scraper_car:
        component_descriptor(scraper_car, "Scraper", "0.1.1-SNAPSHOT")
        scraper_api = json.loads(scraper_car.read("component-api-descriptor.json").decode("utf-8"))

provided = sie_api.get("provided")
if not isinstance(provided, list) or len(provided) != 1:
    raise SystemExit("packaged SIE descriptor has an unexpected provided SPI set")
federation_api = provided[0]
if (
    federation_api.get("componentName") != "SemanticIntegrationEngine"
    or federation_api.get("service") != "KnowledgeFederation"
    or federation_api.get("apiClass") != "org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi"
    or federation_api.get("artifactPath") != "spi/textus-semantic-integration-engine-api.jar"
):
    raise SystemExit("packaged SIE descriptor did not provide the federation API artifact")
socket_set = "org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi$SocketSet"
public_types = federation_api.get("publicTypes")
socket_set_types = [
    entry
    for entry in public_types
    if isinstance(entry, dict) and entry.get("className") == socket_set
] if isinstance(public_types, list) else []
if len(socket_set_types) != 1 or socket_set_types[0].get("kind") != "socket-set":
    raise SystemExit("packaged SIE federation API artifact does not provide SocketSet")

required = bok_api.get("required")
if not isinstance(required, list) or len(required) != 1:
    raise SystemExit("packaged BoK descriptor has an unexpected required SPI set")
spi = required[0]
if (
    spi.get("service") != "SemanticIntegrationFederation"
    or spi.get("apiClass") != "org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi"
    or spi.get("multiplicity") != "*"
    or spi.get("required") is not False
):
    raise SystemExit("packaged BoK descriptor did not retain SemanticIntegrationFederation as optional")

sie_required = sie_api.get("required")
if not isinstance(sie_required, list) or len(sie_required) != 1:
    raise SystemExit("packaged SIE descriptor has an unexpected required SPI set")
scrapers_spi = sie_required[0]
if (
    scrapers_spi.get("componentName") != "SemanticIntegrationEngine"
    or scrapers_spi.get("service") != "Scrapers"
    or scrapers_spi.get("apiClass") != "org.simplemodeling.textus.scraper.api.TextusScraperApi"
    or scrapers_spi.get("multiplicity") != "*"
    or scrapers_spi.get("required") is not True
):
    raise SystemExit("packaged SIE descriptor did not retain the required Scrapers SPI")

scraper_provided = scraper_api.get("provided")
if not isinstance(scraper_provided, list) or len(scraper_provided) != 1:
    raise SystemExit("packaged Scraper descriptor has an unexpected provided SPI set")
scraper_spi = scraper_provided[0]
if (
    scraper_spi.get("componentName") != "Scraper"
    or scraper_spi.get("service") != "Scraping"
    or scraper_spi.get("apiClass") != scrapers_spi.get("apiClass")
    or scraper_spi.get("artifactPath") != "spi/textus-scraper-api.jar"
):
    raise SystemExit("packaged Scraper descriptor did not provide SIE's required Scrapers API")
PY

env \
  CNCF_SERVER_PORT="$CNCF_SERVER_PORT" \
  CNCF_HTTP_BASEURL="$CNCF_HTTP_BASEURL" \
  TEXTUS_SIE_RDF_DB="in-memory" \
  TEXTUS_SIE_VECTOR_DB="in-memory" \
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dcncf.server.port=$CNCF_SERVER_PORT -Dcncf.http.baseurl=$CNCF_HTTP_BASEURL -Dtextus.sie.rdf-db=in-memory -Dtextus.sie.vector-db=in-memory" \
  "$CNCF_BIN" \
    "${CNCF_RUNTIME_ARGS[@]}" \
    --textus.config.file "$config_file" \
    --textus.subsystem=textus-bok-two-source-acceptance \
    server \
    --no-project-classpath \
    --no-default-components \
    --component-dir "$component_dir" >"$server_log" 2>&1 &
server_pid=$!
server_signature="$(_process_signature "$server_pid" || true)"
[[ -n "$server_signature" ]] || { echo "Could not capture two-source SAR server identity" >&2; exit 1; }

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
while :; do
  _captured_process_is_live "$server_pid" "$server_signature" || { echo "Two-source SAR server exited before readiness" >&2; tail -n 300 "$server_log" >&2 || true; exit 1; }
  if "$CURL_BIN" -fsS --connect-timeout 2 --max-time 2 "$CNCF_HTTP_BASEURL/openapi.json" >/dev/null 2>&1; then
    break
  fi
  ((SECONDS < deadline)) || { echo "Timed out waiting for two-source SAR server" >&2; tail -n 300 "$server_log" >&2 || true; exit 1; }
  sleep 0.5
done

listener_pid="$(_listening_pids)" || { tail -n 300 "$server_log" >&2 || true; exit 1; }
[[ "$listener_pid" =~ ^[0-9]+$ ]] || { echo "Expected exactly one numeric loopback listener on port $CNCF_SERVER_PORT" >&2; exit 1; }
_listener_belongs_to_server "$listener_pid" || { echo "Loopback listener does not belong to the captured server" >&2; exit 1; }
listener_signature="$(_process_signature "$listener_pid" || true)"
[[ -n "$listener_signature" ]] || { echo "Could not capture loopback listener identity" >&2; exit 1; }

"$PYTHON_BIN" "$PROBE_SCRIPT" --base-url "$CNCF_HTTP_BASEURL"
_stop_captured_process
echo "BOK_TWO_SOURCE_ACCEPTANCE_SAR_OK components=3 sources=2 backend=memory"
