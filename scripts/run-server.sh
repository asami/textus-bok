#!/usr/bin/env bash
set -euo pipefail

TEXTUS_BIN="${TEXTUS_BIN:-textus}"
TEXTUS_BOK_TARGET="${TEXTUS_BOK_TARGET:-textus-bok}"

exec "$TEXTUS_BIN" "$TEXTUS_BOK_TARGET" server "$@"
