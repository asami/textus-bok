#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIE_ROOT="${TEXTUS_SIE_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-semantic-integration-engine}"
SCRAPER_ROOT="${TEXTUS_SCRAPER_ROOT:-$(cd "$PROJECT_ROOT/.." && pwd)/textus-scraper}"
RUNTIME_DIR="$(mktemp -d "${TMPDIR:-/tmp}/bok-provider-config.XXXXXX")"
CAPTURE_BIN="$RUNTIME_DIR/capture-cncf.sh"

cleanup() {
  rm -rf "$RUNTIME_DIR"
}
trap cleanup EXIT

cat >"$CAPTURE_BIN" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$@" >"${CAPTURE_ARGS_FILE:?}"
exit 1
EOF
chmod +x "$CAPTURE_BIN"

run_capture() {
  local name="$1"
  shift
  local run_dir="$RUNTIME_DIR/$name"
  local args_file="$RUNTIME_DIR/$name.args"
  mkdir -p "$run_dir"
  set +e
  env \
    CNCF_BIN="$CAPTURE_BIN" \
    CAPTURE_ARGS_FILE="$args_file" \
    CNCF_SERVER_PORT=19661 \
    CNCF_HTTP_BASEURL=http://127.0.0.1:19661 \
    TEXTUS_BOK_CODEX_RUN_DIR="$run_dir" \
    "$@" \
    "$SCRIPT_DIR/run-bok-codex-sar.sh" serve
  local status=$?
  set -e
  [[ $status -ne 0 ]] || {
    echo "Capture CNCF unexpectedly succeeded for $name." >&2
    return 1
  }
  [[ -s "$args_file" ]] || {
    echo "Capture arguments are missing for $name." >&2
    return 1
  }
}

require_arg() {
  local file="$1"
  local value="$2"
  rg -F -x -- "$value" "$file" >/dev/null || {
    echo "Expected CNCF argument is missing: $value" >&2
    return 1
  }
}

for car in \
  "$PROJECT_ROOT/target/textus-bok-0.1.0-SNAPSHOT.car" \
  "$SIE_ROOT/target/textus-semantic-integration-engine-0.2.0-SNAPSHOT.car" \
  "$SCRAPER_ROOT/target/textus-scraper-0.1.1-SNAPSHOT.car"; do
  [[ -f "$car" ]] || {
    echo "Build the required CAR before running this check: $car" >&2
    exit 1
  }
done

run_capture defaults
require_arg "$RUNTIME_DIR/defaults.args" "--textus.sie.rdf-db=in-memory"
require_arg "$RUNTIME_DIR/defaults.args" "--textus.sie.vector-db=in-memory"

run_capture provider \
  TEXTUS_SIE_RDF_DB=fuseki \
  TEXTUS_SIE_VECTOR_DB=chroma \
  TEXTUS_SIE_FUSEKI_ENDPOINT=http://fuseki.test:9030 \
  TEXTUS_SIE_CHROMA_ENDPOINT=http://chroma.test:8081 \
  TEXTUS_SIE_EMBEDDING_MODEL=test-model
require_arg "$RUNTIME_DIR/provider.args" "--textus.sie.rdf-db=fuseki"
require_arg "$RUNTIME_DIR/provider.args" "--textus.sie.vector-db=chroma"
require_arg "$RUNTIME_DIR/provider.args" "--textus.sie.fuseki.endpoint=http://fuseki.test:9030"
require_arg "$RUNTIME_DIR/provider.args" "--textus.sie.chroma.endpoint=http://chroma.test:8081"
require_arg "$RUNTIME_DIR/provider.args" "--textus.sie.embedding.model=test-model"

echo "BOK_CODEX_PROVIDER_CONFIG_OK"
