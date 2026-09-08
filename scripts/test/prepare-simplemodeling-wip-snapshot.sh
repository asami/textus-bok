#!/usr/bin/env bash
set -euo pipefail

SOURCE_REPOSITORY="${TEXTUS_SIMPLEMODELING_ORG_ROOT:-/Users/asami/src/dev2025/simplemodeling-org}"
OUTPUT_DIRECTORY=""
VERSION=""
DEFAULT_RUNTIME_EVIDENCE_ROOT="/Users/asami/src/dev2025/simplemodeling-org"
RUNTIME_EVIDENCE_ROOT="${TEXTUS_SIMPLEMODELING_RUNTIME_EVIDENCE_ROOT:-$DEFAULT_RUNTIME_EVIDENCE_ROOT}"
DEFAULT_COZY_LAUNCHER="/Users/asami/src/dev2025/cozy/out.d/bin/launcher"
DEFAULT_COZY_LAUNCHER_SHA256="2998aac2317fca4941467ca157abe93c8bcffc282ec7cb6e79af1ef108a3f885"
DEFAULT_COZY_CLASSPATH="/Users/asami/src/dev2025/cozy/target/classpath.txt"
DEFAULT_COZY_CLASSPATH_SHA256="c790d46829c0b6c683a44f48f31fd2f9ecbce2bfcbb85a32c92ade34583401f2"
DEFAULT_ARCADIA_CLASSPATH="/Users/asami/src/dev2025/arcadia/target/classpath.txt"
DEFAULT_ARCADIA_CLASSPATH_SHA256="71d017afca9c296ee6653fd7ebd7bd68c1cc51cb5128b8b3a5b035c3d3e6b8bc"
COZY_LAUNCHER="${TEXTUS_SIMPLEMODELING_COZY_LAUNCHER:-$DEFAULT_COZY_LAUNCHER}"
COZY_LAUNCHER_SHA256="${TEXTUS_SIMPLEMODELING_COZY_LAUNCHER_SHA256:-$DEFAULT_COZY_LAUNCHER_SHA256}"
COZY_CLASSPATH="${TEXTUS_SIMPLEMODELING_COZY_CLASSPATH:-$DEFAULT_COZY_CLASSPATH}"
COZY_CLASSPATH_SHA256="${TEXTUS_SIMPLEMODELING_COZY_CLASSPATH_SHA256:-$DEFAULT_COZY_CLASSPATH_SHA256}"
ARCADIA_CLASSPATH="${TEXTUS_SIMPLEMODELING_ARCADIA_CLASSPATH:-$DEFAULT_ARCADIA_CLASSPATH}"
ARCADIA_CLASSPATH_SHA256="${TEXTUS_SIMPLEMODELING_ARCADIA_CLASSPATH_SHA256:-$DEFAULT_ARCADIA_CLASSPATH_SHA256}"
OBJECT_MODELING_JA_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_OBJECT_MODELING_JA_FINAL_MP4_SHA256:-7af98dc34851b30e060e8c6e99b4f7e18e6ad61a7d3c9e8baf79e5e52f826135}"
OBJECT_MODELING_EN_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_OBJECT_MODELING_EN_FINAL_MP4_SHA256:-a880a4d4871c766e136d7a6edd5e7321826c119dbff4344fbc5d2f7226b71977}"
KNOWLEDGE_MODELING_JA_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_KNOWLEDGE_MODELING_JA_FINAL_MP4_SHA256:-123a2210df82ad98929c47059fce435b8f0e41961aafb43c66beb04f4af5afea}"
KNOWLEDGE_MODELING_EN_FINAL_MP4_SHA256="${TEXTUS_SIMPLEMODELING_KNOWLEDGE_MODELING_EN_FINAL_MP4_SHA256:-8f71997365f46ca9a53fef8aaf94fa95292d70a4cb7b45971f00b6b9f59b1afc}"

declare -a RUNTIME_EVIDENCE_RELATIVE_PATHS=(
  "target/media/development-process/object-modeling/ja/final.mp4"
  "target/media/development-process/object-modeling/en/final.mp4"
  "target/media/development-process/knowledge-modeling/ja/final.mp4"
  "target/media/development-process/knowledge-modeling/en/final.mp4"
)
declare -a RUNTIME_EVIDENCE_SHA256S=(
  "$OBJECT_MODELING_JA_FINAL_MP4_SHA256"
  "$OBJECT_MODELING_EN_FINAL_MP4_SHA256"
  "$KNOWLEDGE_MODELING_JA_FINAL_MP4_SHA256"
  "$KNOWLEDGE_MODELING_EN_FINAL_MP4_SHA256"
)

usage() {
  printf '%s\n' "Usage: $0 --version 1.0.56 [--source-repo PATH] [--output-dir PATH] [--runtime-evidence-root PATH] [--object-modeling-ja-final-mp4-sha256 DIGEST] [--object-modeling-en-final-mp4-sha256 DIGEST] [--knowledge-modeling-ja-final-mp4-sha256 DIGEST] [--knowledge-modeling-en-final-mp4-sha256 DIGEST] [--cozy-launcher PATH] [--cozy-launcher-sha256 DIGEST] [--cozy-classpath PATH] [--cozy-classpath-sha256 DIGEST] [--arcadia-classpath PATH] [--arcadia-classpath-sha256 DIGEST]"
  printf '%s\n' "  Export the exact accepted SimpleModeling.org commit and run its WIP producer in isolation."
}

while (($# > 0)); do
  case "$1" in
    --version)
      [[ $# -ge 2 ]] || { echo "--version requires a value" >&2; exit 2; }
      VERSION="$2"
      shift 2
      ;;
    --source-repo)
      [[ $# -ge 2 ]] || { echo "--source-repo requires a path" >&2; exit 2; }
      SOURCE_REPOSITORY="$2"
      shift 2
      ;;
    --output-dir)
      [[ $# -ge 2 ]] || { echo "--output-dir requires a path" >&2; exit 2; }
      OUTPUT_DIRECTORY="$2"
      shift 2
      ;;
    --runtime-evidence-root)
      [[ $# -ge 2 ]] || { echo "--runtime-evidence-root requires a path" >&2; exit 2; }
      RUNTIME_EVIDENCE_ROOT="$2"
      shift 2
      ;;
    --object-modeling-ja-final-mp4-sha256)
      [[ $# -ge 2 ]] || { echo "--object-modeling-ja-final-mp4-sha256 requires a digest" >&2; exit 2; }
      OBJECT_MODELING_JA_FINAL_MP4_SHA256="$2"
      RUNTIME_EVIDENCE_SHA256S[0]="$2"
      shift 2
      ;;
    --object-modeling-en-final-mp4-sha256)
      [[ $# -ge 2 ]] || { echo "--object-modeling-en-final-mp4-sha256 requires a digest" >&2; exit 2; }
      OBJECT_MODELING_EN_FINAL_MP4_SHA256="$2"
      RUNTIME_EVIDENCE_SHA256S[1]="$2"
      shift 2
      ;;
    --knowledge-modeling-ja-final-mp4-sha256)
      [[ $# -ge 2 ]] || { echo "--knowledge-modeling-ja-final-mp4-sha256 requires a digest" >&2; exit 2; }
      KNOWLEDGE_MODELING_JA_FINAL_MP4_SHA256="$2"
      RUNTIME_EVIDENCE_SHA256S[2]="$2"
      shift 2
      ;;
    --knowledge-modeling-en-final-mp4-sha256)
      [[ $# -ge 2 ]] || { echo "--knowledge-modeling-en-final-mp4-sha256 requires a digest" >&2; exit 2; }
      KNOWLEDGE_MODELING_EN_FINAL_MP4_SHA256="$2"
      RUNTIME_EVIDENCE_SHA256S[3]="$2"
      shift 2
      ;;
    --cozy-launcher)
      [[ $# -ge 2 ]] || { echo "--cozy-launcher requires a path" >&2; exit 2; }
      COZY_LAUNCHER="$2"
      shift 2
      ;;
    --cozy-launcher-sha256)
      [[ $# -ge 2 ]] || { echo "--cozy-launcher-sha256 requires a digest" >&2; exit 2; }
      COZY_LAUNCHER_SHA256="$2"
      shift 2
      ;;
    --cozy-classpath)
      [[ $# -ge 2 ]] || { echo "--cozy-classpath requires a path" >&2; exit 2; }
      COZY_CLASSPATH="$2"
      shift 2
      ;;
    --cozy-classpath-sha256)
      [[ $# -ge 2 ]] || { echo "--cozy-classpath-sha256 requires a digest" >&2; exit 2; }
      COZY_CLASSPATH_SHA256="$2"
      shift 2
      ;;
    --arcadia-classpath)
      [[ $# -ge 2 ]] || { echo "--arcadia-classpath requires a path" >&2; exit 2; }
      ARCADIA_CLASSPATH="$2"
      shift 2
      ;;
    --arcadia-classpath-sha256)
      [[ $# -ge 2 ]] || { echo "--arcadia-classpath-sha256 requires a digest" >&2; exit 2; }
      ARCADIA_CLASSPATH_SHA256="$2"
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

expected_commit=""
case "$VERSION" in
  1.0.56)
    expected_commit="f8ada934ad295f618d51a0d769847a9ffdd4bfbf"
    ;;
  *)
    echo "--version must be the accepted WIP snapshot: 1.0.56" >&2
    exit 2
    ;;
esac

if [[ ! -f "$COZY_LAUNCHER" || -L "$COZY_LAUNCHER" || ! -x "$COZY_LAUNCHER" ]]; then
  echo "Cozy launcher must be a regular executable non-symlink file: $COZY_LAUNCHER" >&2
  exit 1
fi
COZY_LAUNCHER="$(cd "$(dirname "$COZY_LAUNCHER")" && pwd -P)/$(basename "$COZY_LAUNCHER")"
if [[ ! "$COZY_LAUNCHER_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Cozy launcher SHA-256 must be 64 lowercase hexadecimal characters: $COZY_LAUNCHER_SHA256" >&2
  exit 1
fi
actual_cozy_launcher_sha256="$(shasum -a 256 "$COZY_LAUNCHER")"
actual_cozy_launcher_sha256="${actual_cozy_launcher_sha256%% *}"
if [[ "$actual_cozy_launcher_sha256" != "$COZY_LAUNCHER_SHA256" ]]; then
  echo "Cozy launcher SHA-256 mismatch: expected $COZY_LAUNCHER_SHA256, got $actual_cozy_launcher_sha256" >&2
  exit 1
fi

if [[ ! -f "$COZY_CLASSPATH" || -L "$COZY_CLASSPATH" || ! -s "$COZY_CLASSPATH" ]]; then
  echo "Cozy classpath must be a nonempty regular non-symlink file: $COZY_CLASSPATH" >&2
  exit 1
fi
COZY_CLASSPATH="$(cd "$(dirname "$COZY_CLASSPATH")" && pwd -P)/$(basename "$COZY_CLASSPATH")"
if [[ ! "$COZY_CLASSPATH_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Cozy classpath SHA-256 must be 64 lowercase hexadecimal characters: $COZY_CLASSPATH_SHA256" >&2
  exit 1
fi
actual_cozy_classpath_sha256="$(shasum -a 256 "$COZY_CLASSPATH")"
actual_cozy_classpath_sha256="${actual_cozy_classpath_sha256%% *}"
if [[ "$actual_cozy_classpath_sha256" != "$COZY_CLASSPATH_SHA256" ]]; then
  echo "Cozy classpath SHA-256 mismatch: expected $COZY_CLASSPATH_SHA256, got $actual_cozy_classpath_sha256" >&2
  exit 1
fi

if [[ ! -f "$ARCADIA_CLASSPATH" || -L "$ARCADIA_CLASSPATH" || ! -s "$ARCADIA_CLASSPATH" ]]; then
  echo "Arcadia classpath must be a nonempty regular non-symlink file: $ARCADIA_CLASSPATH" >&2
  exit 1
fi
ARCADIA_CLASSPATH="$(cd "$(dirname "$ARCADIA_CLASSPATH")" && pwd -P)/$(basename "$ARCADIA_CLASSPATH")"
if [[ ! "$ARCADIA_CLASSPATH_SHA256" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Arcadia classpath SHA-256 must be 64 lowercase hexadecimal characters: $ARCADIA_CLASSPATH_SHA256" >&2
  exit 1
fi
actual_arcadia_classpath_sha256="$(shasum -a 256 "$ARCADIA_CLASSPATH")"
actual_arcadia_classpath_sha256="${actual_arcadia_classpath_sha256%% *}"
if [[ "$actual_arcadia_classpath_sha256" != "$ARCADIA_CLASSPATH_SHA256" ]]; then
  echo "Arcadia classpath SHA-256 mismatch: expected $ARCADIA_CLASSPATH_SHA256, got $actual_arcadia_classpath_sha256" >&2
  exit 1
fi

if [[ ! -d "$SOURCE_REPOSITORY" || -L "$SOURCE_REPOSITORY" ]]; then
  echo "SimpleModeling.org source repository must be a real directory: $SOURCE_REPOSITORY" >&2
  exit 1
fi
SOURCE_REPOSITORY="$(cd "$SOURCE_REPOSITORY" && pwd -P)"

if [[ ! -d "$RUNTIME_EVIDENCE_ROOT" || -L "$RUNTIME_EVIDENCE_ROOT" ]]; then
  echo "SimpleModeling.org runtime-evidence root must be a real directory: $RUNTIME_EVIDENCE_ROOT" >&2
  exit 1
fi
RUNTIME_EVIDENCE_ROOT="$(cd "$RUNTIME_EVIDENCE_ROOT" && pwd -P)"

for index in "${!RUNTIME_EVIDENCE_RELATIVE_PATHS[@]}"; do
  runtime_evidence_source="$RUNTIME_EVIDENCE_ROOT/${RUNTIME_EVIDENCE_RELATIVE_PATHS[$index]}"
  runtime_evidence_sha256="${RUNTIME_EVIDENCE_SHA256S[$index]}"
  if [[ ! -f "$runtime_evidence_source" || -L "$runtime_evidence_source" || ! -s "$runtime_evidence_source" ]]; then
    echo "Runtime evidence must be a nonempty regular non-symlink file: $runtime_evidence_source" >&2
    exit 1
  fi
  if [[ ! "$runtime_evidence_sha256" =~ ^[0-9a-f]{64}$ ]]; then
    echo "Runtime evidence SHA-256 must be 64 lowercase hexadecimal characters: $runtime_evidence_sha256" >&2
    exit 1
  fi
  actual_runtime_evidence_sha256="$(shasum -a 256 "$runtime_evidence_source")"
  actual_runtime_evidence_sha256="${actual_runtime_evidence_sha256%% *}"
  if [[ "$actual_runtime_evidence_sha256" != "$runtime_evidence_sha256" ]]; then
    echo "Runtime evidence SHA-256 mismatch: expected $runtime_evidence_sha256, got $actual_runtime_evidence_sha256 ($runtime_evidence_source)" >&2
    exit 1
  fi
done

resolved_commit="$(git -C "$SOURCE_REPOSITORY" rev-parse --verify "${expected_commit}^{commit}")"
if [[ "$resolved_commit" != "$expected_commit" ]]; then
  echo "The selected SimpleModeling.org repository does not contain the accepted $VERSION commit." >&2
  echo "Expected $expected_commit, resolved $resolved_commit." >&2
  exit 1
fi
git -C "$SOURCE_REPOSITORY" cat-file -e "${expected_commit}:etc/runweb-wip.sh"

if [[ -z "$OUTPUT_DIRECTORY" ]]; then
  OUTPUT_DIRECTORY="$(mktemp -d /private/tmp/textus-bok-simplemodeling-wip.XXXXXX)"
else
  OUTPUT_DIRECTORY="$(cd "$(dirname "$OUTPUT_DIRECTORY")" && pwd -P)/$(basename "$OUTPUT_DIRECTORY")"
  if [[ -e "$OUTPUT_DIRECTORY" || -L "$OUTPUT_DIRECTORY" ]]; then
    [[ ! -L "$OUTPUT_DIRECTORY" && -d "$OUTPUT_DIRECTORY" ]] || {
      echo "--output-dir must be a real directory: $OUTPUT_DIRECTORY" >&2
      exit 1
    }
    if [[ -n "$(find "$OUTPUT_DIRECTORY" -mindepth 1 -maxdepth 1 -print -quit)" ]]; then
      echo "--output-dir must be empty: $OUTPUT_DIRECTORY" >&2
      exit 1
    fi
  else
    mkdir -p "$OUTPUT_DIRECTORY"
  fi
fi
if [[ -L "$OUTPUT_DIRECTORY" || ! -d "$OUTPUT_DIRECTORY" ]]; then
  echo "Snapshot output directory must be a real directory: $OUTPUT_DIRECTORY" >&2
  exit 1
fi
chmod 700 "$OUTPUT_DIRECTORY"

git -C "$SOURCE_REPOSITORY" archive --format=tar --worktree-attributes "$expected_commit" |
  tar -xf - -C "$OUTPUT_DIRECTORY"

producer_node_modules="$SOURCE_REPOSITORY/node_modules"
producer_root_lockfile="$SOURCE_REPOSITORY/package-lock.json"
snapshot_root_lockfile="$OUTPUT_DIRECTORY/package-lock.json"
producer_ui_lockfile="$SOURCE_REPOSITORY/src/main/antora-ui/package-lock.json"
snapshot_ui_lockfile="$OUTPUT_DIRECTORY/src/main/antora-ui/package-lock.json"
producer_ui_bundle="$SOURCE_REPOSITORY/src/main/antora-ui/build/ui-bundle.zip"
snapshot_ui_root="$OUTPUT_DIRECTORY/src/main/antora-ui"
snapshot_ui_build="$snapshot_ui_root/build"
snapshot_ui_bundle="$snapshot_ui_build/ui-bundle.zip"

if [[ ! -d "$producer_node_modules" || -L "$producer_node_modules" ]]; then
  echo "SimpleModeling.org producer node_modules must be a real directory: $producer_node_modules" >&2
  exit 1
fi

for lockfile in \
  "$producer_root_lockfile" \
  "$snapshot_root_lockfile" \
  "$producer_ui_lockfile" \
  "$snapshot_ui_lockfile"; do
  if [[ ! -f "$lockfile" || -L "$lockfile" ]]; then
    echo "Snapshot toolchain lockfile must be a regular file: $lockfile" >&2
    exit 1
  fi
done
if ! cmp -s "$producer_root_lockfile" "$snapshot_root_lockfile"; then
  echo "SimpleModeling.org root package-lock.json does not match the archived snapshot." >&2
  exit 1
fi
if ! cmp -s "$producer_ui_lockfile" "$snapshot_ui_lockfile"; then
  echo "SimpleModeling.org Antora UI package-lock.json does not match the archived snapshot." >&2
  exit 1
fi

if [[ ! -f "$producer_ui_bundle" || -L "$producer_ui_bundle" || ! -s "$producer_ui_bundle" ]]; then
  echo "SimpleModeling.org Antora UI bundle must be a nonempty regular file: $producer_ui_bundle" >&2
  exit 1
fi
for snapshot_directory in \
  "$OUTPUT_DIRECTORY/src" \
  "$OUTPUT_DIRECTORY/src/main" \
  "$snapshot_ui_root"; do
  if [[ ! -d "$snapshot_directory" || -L "$snapshot_directory" ]]; then
    echo "Archived snapshot directory must be a real directory: $snapshot_directory" >&2
    exit 1
  fi
done
if [[ -L "$snapshot_ui_build" || ( -e "$snapshot_ui_build" && ! -d "$snapshot_ui_build" ) ]]; then
  echo "Archived snapshot Antora UI build path is not a directory: $snapshot_ui_build" >&2
  exit 1
fi
mkdir -p -- "$snapshot_ui_build"
if [[ -e "$snapshot_ui_bundle" || -L "$snapshot_ui_bundle" ]]; then
  echo "Archived snapshot Antora UI bundle path is unexpectedly present: $snapshot_ui_bundle" >&2
  exit 1
fi
cp -- "$producer_ui_bundle" "$snapshot_ui_bundle"
if [[ ! -f "$snapshot_ui_bundle" || -L "$snapshot_ui_bundle" || ! -s "$snapshot_ui_bundle" ]]; then
  echo "Copied Antora UI bundle is not a nonempty regular file: $snapshot_ui_bundle" >&2
  exit 1
fi

for index in "${!RUNTIME_EVIDENCE_RELATIVE_PATHS[@]}"; do
  runtime_evidence_source="$RUNTIME_EVIDENCE_ROOT/${RUNTIME_EVIDENCE_RELATIVE_PATHS[$index]}"
  runtime_evidence_snapshot="$OUTPUT_DIRECTORY/${RUNTIME_EVIDENCE_RELATIVE_PATHS[$index]}"
  runtime_evidence_snapshot_directory="$(dirname "$runtime_evidence_snapshot")"
  mkdir -p -- "$runtime_evidence_snapshot_directory"
  if [[ -e "$runtime_evidence_snapshot" || -L "$runtime_evidence_snapshot" ]]; then
    echo "Archived snapshot runtime evidence path is unexpectedly present: $runtime_evidence_snapshot" >&2
    exit 1
  fi
  cp -- "$runtime_evidence_source" "$runtime_evidence_snapshot"
  if [[ ! -f "$runtime_evidence_snapshot" || -L "$runtime_evidence_snapshot" || ! -s "$runtime_evidence_snapshot" ]]; then
    echo "Copied runtime evidence is not a nonempty regular file: $runtime_evidence_snapshot" >&2
    exit 1
  fi
  if ! cmp -s "$runtime_evidence_source" "$runtime_evidence_snapshot"; then
    echo "Copied runtime evidence does not match its verified source: $runtime_evidence_snapshot" >&2
    exit 1
  fi
done

arcadia_website_root="$OUTPUT_DIRECTORY/src/main/website"
arcadia_website_web_inf="$arcadia_website_root/WEB-INF"
arcadia_material_kit_marker="$arcadia_website_web_inf/webapp.conf"
if [[ ! -d "$arcadia_website_root" || -L "$arcadia_website_root" ]]; then
  echo "Archived snapshot website directory must be a real directory: $arcadia_website_root" >&2
  exit 1
fi
if [[ -e "$arcadia_website_web_inf" || -L "$arcadia_website_web_inf" ]]; then
  [[ ! -L "$arcadia_website_web_inf" && -d "$arcadia_website_web_inf" ]] || {
    echo "Archived snapshot website WEB-INF path is not a directory: $arcadia_website_web_inf" >&2
    exit 1
  }
else
  mkdir -- "$arcadia_website_web_inf"
fi
if [[ -e "$arcadia_material_kit_marker" || -L "$arcadia_material_kit_marker" ]]; then
  echo "Archived snapshot material-kit-pro marker is unexpectedly present: $arcadia_material_kit_marker" >&2
  exit 1
fi
printf '%s\n' 'name: material-kit-pro' > "$arcadia_material_kit_marker"
if [[ ! -f "$arcadia_material_kit_marker" || -L "$arcadia_material_kit_marker" || ! -s "$arcadia_material_kit_marker" ]]; then
  echo "Snapshot material-kit-pro marker is not a nonempty regular file: $arcadia_material_kit_marker" >&2
  exit 1
fi

cozy_tool_bin="$OUTPUT_DIRECTORY/.cozy-tool-bin"
cozy_shim="$cozy_tool_bin/cozy"
arcadia_shim="$cozy_tool_bin/arcadia"
if [[ -e "$cozy_tool_bin" || -L "$cozy_tool_bin" ]]; then
  echo "Snapshot-local Cozy tool directory is unexpectedly present: $cozy_tool_bin" >&2
  exit 1
fi
mkdir -- "$cozy_tool_bin"
{
  printf '%s\n' '#!/usr/bin/env bash'
  printf '%s\n' 'set -euo pipefail'
  printf 'exec %q --classpath-file %q --main-class cozy.Cozy -- "$@"\n' "$COZY_LAUNCHER" "$COZY_CLASSPATH"
} > "$cozy_shim"
chmod 700 "$cozy_shim"
if [[ ! -f "$cozy_shim" || -L "$cozy_shim" || ! -x "$cozy_shim" ]]; then
  echo "Snapshot-local Cozy shim is not an executable regular non-symlink file: $cozy_shim" >&2
  exit 1
fi
{
  printf '%s\n' '#!/usr/bin/env bash'
  printf '%s\n' 'set -euo pipefail'
  printf '%s\n' '[[ "${1:-}" == "site" ]] || { echo "Unsupported Arcadia operation: ${1:-}" >&2; exit 2; }'
  printf '%s\n' 'shift'
  printf 'exec %q --classpath-file %q --main-class arcadia.standalone.Standalone -- site --library src/main/website,src/main/arcadialib "$@"\n' "$COZY_LAUNCHER" "$ARCADIA_CLASSPATH"
} > "$arcadia_shim"
chmod 700 "$arcadia_shim"
if [[ ! -f "$arcadia_shim" || -L "$arcadia_shim" || ! -x "$arcadia_shim" ]]; then
  echo "Snapshot-local Arcadia shim is not an executable regular non-symlink file: $arcadia_shim" >&2
  exit 1
fi

(cd "$OUTPUT_DIRECTORY" && PATH="$cozy_tool_bin:$PATH" NODE_PATH="$producer_node_modules" /bin/bash etc/runweb-wip.sh)

website_root="$OUTPUT_DIRECTORY/website.d"
manifest="$website_root/metadata/cncf/knowledge-source.json"
if [[ ! -s "$manifest" ]]; then
  echo "WIP producer did not create the KnowledgeSource manifest: $manifest" >&2
  exit 1
fi

printf 'simplemodeling_wip_snapshot=%s version=%s commit=%s\n' "$OUTPUT_DIRECTORY" "$VERSION" "$expected_commit"
