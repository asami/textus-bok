#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SCRIPTS_ROOT="$PROJECT_ROOT/scripts"
README_PATH="$PROJECT_ROOT/README.md"
USER_GUIDE_PATH="$PROJECT_ROOT/src/main/car/manual/user-guide.md"

operational_wrappers=(
  "$SCRIPTS_ROOT/prepare-development-runtime.sh"
  "$SCRIPTS_ROOT/run-server.sh"
  "$SCRIPTS_ROOT/update-runtime-classpath.sh"
)
moved_test_entrypoints=(
  "$SCRIPT_DIR/check-bok-codex-provider-config.sh"
  "$SCRIPT_DIR/check-bok-migration-sar.sh"
  "$SCRIPT_DIR/check-bok-profile-selection-sar.sh"
  "$SCRIPT_DIR/check-managed-bootstrap-tools.sh"
  "$SCRIPT_DIR/prepare-simplemodeling-wip-snapshot.sh"
  "$SCRIPT_DIR/run-bok-codex-sar.sh"
  "$SCRIPT_DIR/run-bok-knowledge-map-sar.sh"
)
managed_bootstrap_tools=(
  "$SCRIPTS_ROOT/configure-simplemodeling-development.sh"
  "$SCRIPT_DIR/check-managed-bootstrap-tools.sh"
  "$SCRIPT_DIR/prepare-simplemodeling-wip-snapshot.sh"
)

for script in "${operational_wrappers[@]}" "${moved_test_entrypoints[@]}" "${managed_bootstrap_tools[@]}" "$SCRIPT_DIR/run-server.sh" "$SCRIPT_DIR/run-server-debug.sh"; do
  bash -n "$script"
done

! grep -F '1.0.57' "$README_PATH" >/dev/null
! grep -F '1.0.57' "$USER_GUIDE_PATH" >/dev/null

prepare_runtime="$SCRIPTS_ROOT/prepare-development-runtime.sh"
grep -F 'run-sbt-serial.sh' "$prepare_runtime" >/dev/null
grep -F 'cozyPrepareRuntime' "$prepare_runtime" >/dev/null
! grep -E '(^|[[:space:]])sbt([[:space:]]|$)' "$prepare_runtime" >/dev/null

production_server="$SCRIPTS_ROOT/run-server.sh"
grep -F 'TEXTUS_BIN' "$production_server" >/dev/null
grep -F 'TEXTUS_BOK_TARGET' "$production_server" >/dev/null
! grep -E 'cncf|(^|[[:space:]])sbt([[:space:]]|$)|(^|[[:space:]])java([[:space:]]|$)' "$production_server" >/dev/null

test_server="$SCRIPT_DIR/run-server.sh"
debug_server="$SCRIPT_DIR/run-server-debug.sh"
for script in "$test_server" "$debug_server"; do
  grep -F 'CNCF_BIN' "$script" >/dev/null
  ! grep -E 'textus|(^|[[:space:]])sbt([[:space:]]|$)|(^|[[:space:]])java([[:space:]]|$)' "$script" >/dev/null
done

for old_path in \
  "$SCRIPTS_ROOT/check-bok-codex-provider-config.sh" \
  "$SCRIPTS_ROOT/check-bok-migration-sar.sh" \
  "$SCRIPTS_ROOT/check-bok-profile-selection-sar.sh" \
  "$SCRIPTS_ROOT/probe-actual-bok.py" \
  "$SCRIPTS_ROOT/run-bok-codex-sar.sh" \
  "$SCRIPTS_ROOT/run-bok-knowledge-map-sar.sh" \
  "$SCRIPTS_ROOT/probe-bok-codex-sar.py" \
  "$SCRIPTS_ROOT/probe-bok-knowledge-map-sar.py" \
  "$SCRIPTS_ROOT/probe-bok-migration-sar.py" \
  "$SCRIPTS_ROOT/probe-bok-profile-selection-sar.py" \
  "$SCRIPTS_ROOT/run-server-debug.sh"; do
  [[ ! -e "$old_path" ]]
done

for new_path in \
  "$SCRIPTS_ROOT/configure-simplemodeling-development.sh" \
  "$SCRIPT_DIR/probe-actual-bok.py" \
  "$SCRIPT_DIR/probe-managed-bok.py" \
  "$SCRIPT_DIR/prepare-simplemodeling-wip-snapshot.sh" \
  "$SCRIPT_DIR/check-managed-bootstrap-tools.sh" \
  "$SCRIPT_DIR/lib/probe-bok-codex-sar.py" \
  "$SCRIPT_DIR/lib/probe-bok-knowledge-map-sar.py" \
  "$SCRIPT_DIR/lib/probe-bok-migration-sar.py" \
  "$SCRIPT_DIR/lib/probe-bok-profile-selection-sar.py"; do
  [[ -e "$new_path" ]]
done

echo "SCRIPT_ENTRYPOINT_LAYOUT_OK"
