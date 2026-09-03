#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SBT_SERIAL_RUNNER="${SBT_SERIAL_RUNNER:-/Users/asami/.codex/skills/cncf-sbt-serial-execution/scripts/run-sbt-serial.sh}"

if [[ ! -x "$SBT_SERIAL_RUNNER" ]]; then
  echo "The shared SBT serial runner is not executable: $SBT_SERIAL_RUNNER" >&2
  exit 1
fi

cd "$PROJECT_ROOT"
exec "$SBT_SERIAL_RUNNER" --batch cozyPrepareRuntime
