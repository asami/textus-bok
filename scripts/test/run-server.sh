#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CNCF_BIN="${CNCF_BIN:-cncf}"

exec "$CNCF_BIN" "$PROJECT_ROOT" server "$@"
