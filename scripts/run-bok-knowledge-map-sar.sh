#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_ROOT="${TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT:-}"
SOURCE_ID="${TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ID:-knowledgehub}"
DATASET_ID="${TEXTUS_BOK_KNOWLEDGE_MAP_DATASET_ID:-knowledgehub}"
GENERATION="${TEXTUS_BOK_KNOWLEDGE_MAP_GENERATION:-2026-07-23T00:00:00Z}"
PORT="${CNCF_SERVER_PORT:-18007}"
BASE_URL="${CNCF_HTTP_BASEURL:-http://127.0.0.1:$PORT}"
RUN_DIR="${TEXTUS_BOK_CODEX_RUN_DIR:-/private/tmp/textus-bok-knowledge-map-sar}"
LABEL="${TEXTUS_BOK_CODEX_LAUNCH_LABEL:-org.textus.bok-knowledge-map-sar}"

case "${1:-status}" in
  start | restart)
    if [[ -z "$SOURCE_ROOT" || ! -f "$SOURCE_ROOT/metadata/cncf/knowledge-source.json" || ! -f "$SOURCE_ROOT/metadata/rdf/graph.json" ]]; then
      echo "Set TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT to a generated Cozy website.d directory." >&2
      exit 2
    fi
    if [[ "${1:-}" == "restart" ]]; then
      CNCF_SERVER_PORT="$PORT" CNCF_HTTP_BASEURL="$BASE_URL" TEXTUS_BOK_CODEX_RUN_DIR="$RUN_DIR" TEXTUS_BOK_CODEX_LAUNCH_LABEL="$LABEL" "$SCRIPT_DIR/run-bok-codex-sar.sh" stop
    fi
      TEXTUS_BOK_CODEX_FIXTURE_ROOT="$SOURCE_ROOT" \
      TEXTUS_BOK_CODEX_SKIP_PROBE=true \
      TEXTUS_BOK_CODEX_EMBED_COMPONENTS=false \
      TEXTUS_BOK_CODEX_PROBE_QUERY="${TEXTUS_BOK_CODEX_PROBE_QUERY:-Embedding}" \
      TEXTUS_BOK_CODEX_PROBE_CATEGORY="${TEXTUS_BOK_CODEX_PROBE_CATEGORY:-technology}" \
      TEXTUS_BOK_CODEX_PROBE_SOURCE_ID="$SOURCE_ID" \
      TEXTUS_BOK_CODEX_PROBE_DATASET_ID="$DATASET_ID" \
      TEXTUS_BOK_CODEX_PROBE_GENERATION="$GENERATION" \
      CNCF_SERVER_PORT="$PORT" CNCF_HTTP_BASEURL="$BASE_URL" \
      TEXTUS_BOK_CODEX_RUN_DIR="$RUN_DIR" TEXTUS_BOK_CODEX_LAUNCH_LABEL="$LABEL" \
      "$SCRIPT_DIR/run-bok-codex-sar.sh" start
    "$SCRIPT_DIR/probe-bok-knowledge-map-sar.py" \
      --base-url "$BASE_URL" --source-root "$SOURCE_ROOT" \
      --dataset-id "$DATASET_ID" --source-id "$SOURCE_ID" --generation "$GENERATION"
    ;;
  stop | status)
    CNCF_SERVER_PORT="$PORT" CNCF_HTTP_BASEURL="$BASE_URL" TEXTUS_BOK_CODEX_RUN_DIR="$RUN_DIR" TEXTUS_BOK_CODEX_LAUNCH_LABEL="$LABEL" "$SCRIPT_DIR/run-bok-codex-sar.sh" "$1"
    ;;
  *)
    echo "Usage: $0 {start|restart|stop|status}" >&2
    exit 2
    ;;
esac
