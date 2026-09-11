#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
PYTHON_BIN="${PYTHON_BIN:-python3}"
SIMPLEMODELING_SOURCE_ROOT=""
KNOWLEDGEHUB_SOURCE_ROOT=""
OUTPUT_PATH="$PROJECT_ROOT/.textus/config.yaml"

usage() {
  printf '%s\n' "Usage: $0 --simplemodeling-source-root PATH --knowledgehub-source-root PATH [--output PATH]"
  printf '%s\n' "  Validate two generated website.d roots and atomically write the marked private catalog configuration."
}

while (($# > 0)); do
  case "$1" in
    --simplemodeling-source-root)
      [[ $# -ge 2 ]] || { echo "--simplemodeling-source-root requires a path" >&2; exit 2; }
      SIMPLEMODELING_SOURCE_ROOT="$2"
      shift 2
      ;;
    --knowledgehub-source-root)
      [[ $# -ge 2 ]] || { echo "--knowledgehub-source-root requires a path" >&2; exit 2; }
      KNOWLEDGEHUB_SOURCE_ROOT="$2"
      shift 2
      ;;
    --output|--output-path)
      [[ $# -ge 2 ]] || { echo "--output requires a path" >&2; exit 2; }
      OUTPUT_PATH="$2"
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

[[ -n "$SIMPLEMODELING_SOURCE_ROOT" ]] || { echo "--simplemodeling-source-root is required" >&2; usage >&2; exit 2; }
[[ -n "$KNOWLEDGEHUB_SOURCE_ROOT" ]] || { echo "--knowledgehub-source-root is required" >&2; usage >&2; exit 2; }
command -v "$PYTHON_BIN" >/dev/null 2>&1 || { echo "Python interpreter is unavailable: $PYTHON_BIN" >&2; exit 1; }

exec "$PYTHON_BIN" - "$PROJECT_ROOT" "$SIMPLEMODELING_SOURCE_ROOT" "$KNOWLEDGEHUB_SOURCE_ROOT" "$OUTPUT_PATH" <<'PY'
import hashlib
import json
import os
import stat
import sys
import tempfile
from pathlib import Path, PurePosixPath
from typing import NoReturn
from urllib.parse import urlparse


MARKER = "# textus-bok-managed-two-source-acceptance-config: v1"
LEGACY_DEFAULT_MARKER = "# textus-bok-managed-profile-config: v1"
PROFILES = (
    ("development", None, "simplemodeling-development", "simplemodeling-bok-development", "https://www.simplemodeling.org/"),
    ("project", "knowledgehub", "knowledgehub", "knowledgehub-bok", "https://www.asamioffice.com/kokubunji/knowledgehub/"),
)


def fail(message: str) -> NoReturn:
    raise SystemExit(f"configuration refused: {message}")


def regular_file(path: Path, label: str) -> bytes:
    if path.is_symlink() or not path.is_file():
        fail(f"{label} must be a regular non-symlink file: {path}")
    try:
        return path.read_bytes()
    except OSError as error:
        fail(f"{label} is unreadable: {error}")


def real_directory(path: Path, label: str) -> Path:
    if not path.is_absolute():
        fail(f"{label} must be an explicit absolute path: {path}")
    if path.is_symlink() or not path.is_dir():
        fail(f"{label} must be a real directory: {path}")
    try:
        resolved = path.resolve(strict=True)
    except OSError as error:
        fail(f"{label} is unavailable: {error}")
    if not resolved.is_dir():
        fail(f"{label} must be a directory: {path}")
    return resolved


def safe_resource_path(root: Path, href: object) -> tuple[str, Path]:
    if not isinstance(href, str) or not href:
        fail("each KnowledgeSource resource href must be a non-empty string")
    if href.startswith("/") or "\\" in href:
        fail(f"resource href is not a safe relative path: {href!r}")
    parts = href.split("/")
    if any(part in ("", ".", "..") for part in parts):
        fail(f"resource href is not a safe relative path: {href!r}")
    relative = PurePosixPath(href)
    if str(relative) != href or not relative.parts:
        fail(f"resource href is not a canonical relative path: {href!r}")
    candidate = root.joinpath(*relative.parts)
    current = root
    for part in relative.parts:
        current = current / part
        if current.is_symlink():
            fail(f"resource path contains a symbolic link: {href!r}")
    try:
        resolved = candidate.resolve(strict=True)
        resolved.relative_to(root)
    except (OSError, ValueError) as error:
        fail(f"resource href does not resolve below its source root: {href!r}: {error}")
    data = regular_file(resolved, "KnowledgeSource resource")
    if not data:
        fail(f"KnowledgeSource resource is empty: {href!r}")
    return href, resolved


def content_generation(manifest_bytes: bytes, resources: list[tuple[str, bytes]]) -> str:
    digest = hashlib.sha256()

    def add(name: str, data: bytes) -> None:
        encoded = name.encode("utf-8")
        digest.update(len(encoded).to_bytes(4, "big"))
        digest.update(encoded)
        digest.update(len(data).to_bytes(8, "big"))
        digest.update(data)

    add("metadata/cncf/knowledge-source.json", manifest_bytes)
    for href, data in sorted(resources, key=lambda entry: entry[0]):
        add(href, data)
    return f"sha256:{digest.hexdigest()}"


def evidence_uri(manifest: dict, fallback: str) -> str:
    source_ref = manifest.get("sourceRef")
    value = source_ref.get("uri") if isinstance(source_ref, dict) else None
    if not isinstance(value, str) or "\n" in value or "\r" in value:
        return fallback
    parsed = urlparse(value)
    if parsed.scheme in {"http", "https"} and parsed.netloc and parsed.username is None:
        return value
    return fallback


def validate_source(root: Path, source_id: str, dataset_id: str, fallback_evidence_uri: str) -> dict:
    _, manifest_path = safe_resource_path(root, "metadata/cncf/knowledge-source.json")
    manifest_bytes = regular_file(manifest_path, "KnowledgeSource manifest")
    try:
        manifest = json.loads(manifest_bytes.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        fail(f"KnowledgeSource manifest is not valid UTF-8 JSON: {error}")
    if not isinstance(manifest, dict) or manifest.get("schemaVersion") != "cncf.knowledge-source.v1":
        fail("KnowledgeSource manifest schemaVersion must be cncf.knowledge-source.v1")
    resources = manifest.get("resources")
    if not isinstance(resources, list) or not resources:
        fail("KnowledgeSource manifest resources must be a non-empty list")

    resource_bytes: list[tuple[str, bytes]] = []
    seen_hrefs: set[str] = set()
    glossary_count = 0
    for resource in resources:
        if not isinstance(resource, dict):
            fail("each KnowledgeSource resource must be an object")
        kind = resource.get("kind")
        if not isinstance(kind, str) or not kind:
            fail("each KnowledgeSource resource must have a non-empty kind")
        href, path = safe_resource_path(root, resource.get("href"))
        if href in seen_hrefs:
            fail(f"KnowledgeSource resource href is duplicated: {href!r}")
        seen_hrefs.add(href)
        data = regular_file(path, "KnowledgeSource resource")
        if "json" in str(resource.get("mediaType", "")).lower() or href.lower().endswith(".json"):
            try:
                json.loads(data.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as error:
                fail(f"JSON KnowledgeSource resource is malformed: {href!r}: {error}")
        if kind == "glossary-terms":
            glossary_count += 1
            glossary = json.loads(data.decode("utf-8"))
            if not isinstance(glossary, dict) or not isinstance(glossary.get("terms"), list) or not glossary["terms"]:
                fail("glossary terms resource must contain one or more terms")
        resource_bytes.append((href, data))
    if glossary_count != 1:
        fail("KnowledgeSource manifest must declare exactly one glossary-terms resource")

    return {
        "source_id": source_id,
        "dataset_id": dataset_id,
        "generation": content_generation(manifest_bytes, resource_bytes),
        "resource": root.as_uri() + "/",
        "evidence_uri": evidence_uri(manifest, fallback_evidence_uri),
        "root": str(root),
        "resource_count": len(resource_bytes),
    }


def build_config(sources: list[dict]) -> str:
    profile_lines: list[str] = []
    for source, (profile, project_id, _, _, _) in zip(sources, PROFILES, strict=True):
        profile_lines.extend([
            f"    - profile: {profile}",
        ])
        if project_id is not None:
            profile_lines.append(f"      projectId: {json.dumps(project_id)}")
        profile_lines.extend([
            "      source:",
            f"        sourceId: {json.dumps(source['source_id'])}",
            f"        datasetId: {json.dumps(source['dataset_id'])}",
            f"        generation: {json.dumps(source['generation'])}",
            f"        resource: {json.dumps(source['resource'])}",
            "      evidence:",
            f"        uri: {json.dumps(source['evidence_uri'])}",
            f"        sourceId: {json.dumps(source['source_id'])}",
            f"      freshnessGeneration: {json.dumps(source['generation'])}",
        ])
    roots = ",".join(source["root"] for source in sources)
    return "\n".join([
        MARKER,
        "# This private configuration is generated only from the two explicit accepted website.d roots.",
        "# It contains no ambient source discovery and no Semantic Integration Engine provider policy.",
        "textus.bok.profile-registry:",
        "  profiles:",
        *profile_lines,
        "  catalogs:",
        "    - catalogId: documentation",
        "      members:",
        "        - development",
        "        - project:knowledgehub",
        "textus.bok.bootstrap:",
        "  profiles:",
        "    - development",
        "    - project:knowledgehub",
        "textus.bok.knowledge-backend:",
        "  backend: memory",
        f"textus.resource.url.file.roots: {json.dumps(roots)}",
        "",
    ])


def output_ancestors_are_real(project_root: Path, output: Path) -> None:
    relative = output.relative_to(project_root)
    current = project_root
    for part in relative.parts[:-1]:
        current = current / part
        if current.is_symlink():
            fail(f"output path contains a symbolic-link ancestor: {current}")
        if current.exists() and not current.is_dir():
            fail(f"output path ancestor must be a directory: {current}")


def output_path(project_root: Path, raw_output: str) -> tuple[Path, bool]:
    if any(part in {".", ".."} for part in raw_output.split(os.sep)):
        fail(f"output path must not contain dot segments: {raw_output}")
    raw = Path(raw_output)
    candidate = raw if raw.is_absolute() else project_root / raw
    default_config = project_root / ".textus" / "config.yaml"
    target_root = project_root / "target"
    is_default_config = candidate == default_config
    try:
        target_relative = candidate.relative_to(target_root)
        is_target_output = bool(target_relative.parts)
    except ValueError:
        is_target_output = False
    if not is_default_config and not is_target_output:
        fail("output must be the project .textus/config.yaml or a file below the project target directory")
    output_ancestors_are_real(project_root, candidate)
    return candidate, is_default_config


def write_atomically(project_root: Path, output: Path, content: str, is_default_config: bool) -> None:
    if output.exists() or output.is_symlink():
        if output.is_symlink() or not output.is_file():
            fail(f"output must be a regular file when it already exists: {output}")
        existing = regular_file(output, "existing output")
        accepted_markers = [MARKER]
        if is_default_config:
            accepted_markers.append(LEGACY_DEFAULT_MARKER)
        if not existing.startswith(tuple((marker + "\n").encode("utf-8") for marker in accepted_markers)):
            fail(f"refusing to overwrite an unmarked configuration: {output}")
        mode = stat.S_IMODE(output.stat().st_mode)
    else:
        mode = 0o600
    parent = output.parent
    parent.mkdir(parents=True, exist_ok=True)
    output_ancestors_are_real(project_root, output)
    if parent.is_symlink() or not parent.is_dir():
        fail(f"output parent must be a real directory: {parent}")
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{output.name}.", suffix=".tmp", dir=str(parent))
    try:
        with os.fdopen(descriptor, "wb") as temporary:
            temporary.write(content.encode("utf-8"))
            temporary.flush()
            os.fsync(temporary.fileno())
        os.chmod(temporary_name, mode)
        os.replace(temporary_name, output)
    except OSError as error:
        try:
            os.unlink(temporary_name)
        except OSError:
            pass
        fail(f"atomic configuration write failed: {error}")


project = real_directory(Path(sys.argv[1]), "project root")
simplemodeling_root = real_directory(Path(sys.argv[2]), "SimpleModeling source root")
knowledgehub_root = real_directory(Path(sys.argv[3]), "KnowledgeHub source root")
if simplemodeling_root == knowledgehub_root:
    fail("the two source roots must be distinct")
sources = [
    validate_source(simplemodeling_root, PROFILES[0][2], PROFILES[0][3], PROFILES[0][4]),
    validate_source(knowledgehub_root, PROFILES[1][2], PROFILES[1][3], PROFILES[1][4]),
]
output, is_default_config = output_path(project, sys.argv[4])
write_atomically(project, output, build_config(sources), is_default_config)
print(
    "configured two-source documentation catalog: "
    f"simplemodeling-generation={sources[0]['generation']} knowledgehub-generation={sources[1]['generation']} "
    f"resources={sources[0]['resource_count'] + sources[1]['resource_count']} output={output}"
)
PY
