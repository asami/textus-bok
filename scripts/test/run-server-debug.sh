#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CNCF_BIN="${CNCF_BIN:-cncf}"
DEBUG_PORT="${DEBUG_PORT:-5005}"

export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
exec "$CNCF_BIN" "$PROJECT_ROOT" server "$@"
