#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd -P)"
TEST_ROOT="$PROJECT_ROOT/target"

mkdir -p "$TEST_ROOT"
temporary_project="$(mktemp -d "$TEST_ROOT/configure-bok-two-source-acceptance.XXXXXX")"

cleanup() {
  rm -rf "$temporary_project"
}
trap cleanup EXIT

write_source() {
  local source_root="$1"
  mkdir -p "$source_root/metadata/cncf" "$source_root/metadata/glossary"
  printf '%s\n' \
    '{"schemaVersion":"cncf.knowledge-source.v1","resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json","mediaType":"application/json"}]}' \
    > "$source_root/metadata/cncf/knowledge-source.json"
  printf '%s\n' \
    '{"terms":[{"id":"architecture:component","title":"Component","definition_text":"A reusable unit.","category":"architecture","term_type":"concept"}]}' \
    > "$source_root/metadata/glossary/terms.json"
}

mkdir -p "$temporary_project/scripts" "$temporary_project/.textus"
cp "$PROJECT_ROOT/scripts/configure-bok-two-source-acceptance.sh" "$temporary_project/scripts/"
writer="$temporary_project/scripts/configure-bok-two-source-acceptance.sh"
simplemodeling_source_root="$temporary_project/simplemodeling/website.d"
knowledgehub_source_root="$temporary_project/knowledgehub/website.d"
write_source "$simplemodeling_source_root"
write_source "$knowledgehub_source_root"

printf '%s\n' '# textus-bok-managed-profile-config: v1' > "$temporary_project/.textus/config.yaml"
"$writer" \
  --simplemodeling-source-root "$simplemodeling_source_root" \
  --knowledgehub-source-root "$knowledgehub_source_root"
grep -Fqx '# textus-bok-managed-two-source-acceptance-config: v1' "$temporary_project/.textus/config.yaml"
grep -Fqx 'textus.bok.knowledge-backend:' "$temporary_project/.textus/config.yaml"
grep -Fqx '  backend: memory' "$temporary_project/.textus/config.yaml"
if grep -Fq 'textus.sie' "$temporary_project/.textus/config.yaml"; then
  echo "generated two-source configuration contains a textus.sie policy" >&2
  exit 1
fi

legacy_target_output="$temporary_project/target/legacy-marker.yaml"
mkdir -p "${legacy_target_output%/*}"
printf '%s\n' '# textus-bok-managed-profile-config: v1' > "$legacy_target_output"
if "$writer" \
  --simplemodeling-source-root "$simplemodeling_source_root" \
  --knowledgehub-source-root "$knowledgehub_source_root" \
  --output "$legacy_target_output"; then
  echo "legacy marker was accepted outside the exact default configuration" >&2
  exit 1
fi
grep -Fqx '# textus-bok-managed-profile-config: v1' "$legacy_target_output"

dot_segment_output="$temporary_project/target/../p10-escape.yaml"
if "$writer" \
  --simplemodeling-source-root "$simplemodeling_source_root" \
  --knowledgehub-source-root "$knowledgehub_source_root" \
  --output "$dot_segment_output"; then
  echo "dot-segment output was accepted" >&2
  exit 1
fi
[[ ! -e "$temporary_project/p10-escape.yaml" ]]

mkdir -p "$temporary_project/target" "$temporary_project/escape-directory"
ln -s ../escape-directory "$temporary_project/target/symlinked-output"
if "$writer" \
  --simplemodeling-source-root "$simplemodeling_source_root" \
  --knowledgehub-source-root "$knowledgehub_source_root" \
  --output "$temporary_project/target/symlinked-output/config.yaml"; then
  echo "symbolic-link ancestor output was accepted" >&2
  exit 1
fi
[[ ! -e "$temporary_project/escape-directory/config.yaml" ]]

printf '%s\n' 'configure-bok-two-source-acceptance containment checks passed'
