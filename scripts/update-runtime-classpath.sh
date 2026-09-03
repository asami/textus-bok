#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "update-runtime-classpath.sh is deprecated; it now prepares complete development runtime evidence." >&2
exec "$SCRIPT_DIR/prepare-development-runtime.sh" "$@"
