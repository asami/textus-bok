#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
PYTHON_BIN="${PYTHON_BIN:-python3}"
SOURCE_ROOT=""
OUTPUT_PATH="$PROJECT_ROOT/.textus/config.yaml"

usage() {
  printf '%s\n' "Usage: $0 --source-root PATH [--output PATH]"
  printf '%s\n' "  Validate a generated website.d root and write the marked private development config."
}

while (($# > 0)); do
  case "$1" in
    --source-root)
      [[ $# -ge 2 ]] || { echo "--source-root requires a path" >&2; exit 2; }
      SOURCE_ROOT="$2"
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

[[ -n "$SOURCE_ROOT" ]] || {
  echo "--source-root is required" >&2
  usage >&2
  exit 2
}

command -v "$PYTHON_BIN" >/dev/null 2>&1 || {
  echo "Python interpreter is unavailable: $PYTHON_BIN" >&2
  exit 1
}

exec "$PYTHON_BIN" - "$SOURCE_ROOT" "$OUTPUT_PATH" <<'PY'
import hashlib
import json
import os
import stat
import sys
import tempfile
from pathlib import Path, PurePosixPath
from urllib.parse import urlparse


MARKER = "# textus-bok-managed-development-config: v1"
SOURCE_ID = "simplemodeling-development"
DATASET_ID = "simplemodeling-bok-development"
DEFAULT_EVIDENCE_URI = "https://www.simplemodeling.org/"


def fail(message: str) -> "NoReturn":
    raise SystemExit(f"configuration refused: {message}")


def regular_file(path: Path, label: str) -> bytes:
    if path.is_symlink() or not path.is_file():
        fail(f"{label} must be a regular non-symlink file: {path}")
    try:
        return path.read_bytes()
    except OSError as error:
        fail(f"{label} is unreadable: {error}")


def safe_resource_path(root: Path, href: object) -> tuple[str, Path]:
    if not isinstance(href, str) or not href:
        fail("each KnowledgeSource resource href must be a non-empty string")
    if href.startswith("/") or "\\" in href:
        fail(f"resource href is not a safe relative path: {href!r}")
    raw_parts = href.split("/")
    if any(part in ("", ".", "..") for part in raw_parts):
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
    except OSError as error:
        fail(f"resource href does not resolve: {href!r}: {error}")
    try:
        resolved.relative_to(root)
    except ValueError:
        fail(f"resource href escapes the source root: {href!r}")
    data = regular_file(resolved, "KnowledgeSource resource")
    if not data:
        fail(f"KnowledgeSource resource is empty: {href!r}")
    return href, resolved


def content_generation(manifest_bytes: bytes, resources: list[tuple[str, bytes]]) -> str:
    digest = hashlib.sha256()

    def add(name: str, data: bytes) -> None:
        encoded_name = name.encode("utf-8")
        digest.update(len(encoded_name).to_bytes(4, "big"))
        digest.update(encoded_name)
        digest.update(len(data).to_bytes(8, "big"))
        digest.update(data)

    add("metadata/cncf/knowledge-source.json", manifest_bytes)
    for href, data in sorted(resources, key=lambda item: item[0]):
        add(href, data)
    return f"sha256:{digest.hexdigest()}"


def evidence_uri(manifest: dict) -> str:
    source_ref = manifest.get("sourceRef")
    value = source_ref.get("uri") if isinstance(source_ref, dict) else None
    if not isinstance(value, str) or "\n" in value or "\r" in value:
        return DEFAULT_EVIDENCE_URI
    parsed = urlparse(value)
    if parsed.scheme in {"http", "https"} and parsed.netloc and parsed.username is None:
        return value
    return DEFAULT_EVIDENCE_URI


def build_config(root: Path) -> tuple[str, int, int]:
    manifest_path = root / "metadata/cncf/knowledge-source.json"
    manifest_bytes = regular_file(manifest_path, "KnowledgeSource manifest")
    try:
        manifest = json.loads(manifest_bytes.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        fail(f"KnowledgeSource manifest is not valid UTF-8 JSON: {error}")
    if not isinstance(manifest, dict):
        fail("KnowledgeSource manifest must be a JSON object")
    if manifest.get("schemaVersion") != "cncf.knowledge-source.v1":
        fail("KnowledgeSource manifest schemaVersion must be cncf.knowledge-source.v1")
    resources = manifest.get("resources")
    if not isinstance(resources, list) or not resources:
        fail("KnowledgeSource manifest resources must be a non-empty list")

    resource_bytes: list[tuple[str, bytes]] = []
    glossary_count = 0
    term_count = 0
    seen_hrefs: set[str] = set()
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
        data = path.read_bytes()
        if "json" in str(resource.get("mediaType", "")).lower() or href.lower().endswith(".json"):
            try:
                json.loads(data.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as error:
                fail(f"JSON KnowledgeSource resource is malformed: {href!r}: {error}")
        resource_bytes.append((href, data))
        if kind == "glossary-terms":
            glossary_count += 1
            try:
                glossary = json.loads(data.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as error:
                fail(f"glossary terms resource is malformed: {error}")
            if not isinstance(glossary, dict) or not isinstance(glossary.get("terms"), list):
                fail("glossary terms resource must contain a terms list")
            term_count = len(glossary["terms"])
    if glossary_count != 1:
        fail("KnowledgeSource manifest must declare exactly one glossary-terms resource")
    if term_count < 1:
        fail("the development glossary must contain at least one term")

    generation = content_generation(manifest_bytes, resource_bytes)
    root_uri = root.as_uri() + "/"
    config = "\n".join(
        [
            MARKER,
            "# This file is generated from an accepted SimpleModeling.org website.d snapshot.",
            "# Regenerate it after a build; it is private project-local runtime configuration.",
            "textus.bok.profile-registry:",
            "  profiles:",
            "    - profile: development",
            "      source:",
            f"        sourceId: {json.dumps(SOURCE_ID)}",
            f"        datasetId: {json.dumps(DATASET_ID)}",
            f"        generation: {json.dumps(generation)}",
            f"        resource: {json.dumps(root_uri)}",
            "      evidence:",
            f"        uri: {json.dumps(evidence_uri(manifest))}",
            f"        sourceId: {json.dumps(SOURCE_ID)}",
            f"      freshnessGeneration: {json.dumps(generation)}",
            "textus.bok.bootstrap:",
            "  profiles:",
            "    - development",
            "textus.sie:",
            "  rdf-db: in-memory",
            "  vector-db: in-memory",
            f"textus.resource.url.file.roots: {json.dumps(str(root))}",
            "",
        ]
    )
    return config, term_count, len(resource_bytes)


def write_atomically(output: Path, content: str) -> None:
    if output.exists() or output.is_symlink():
        if output.is_symlink() or not output.is_file():
            fail(f"output must be a regular file when it already exists: {output}")
        existing = regular_file(output, "existing output")
        if not existing.startswith((MARKER + "\n").encode("utf-8")):
            fail(f"refusing to overwrite an unmarked configuration: {output}")
        mode = stat.S_IMODE(output.stat().st_mode)
    else:
        mode = 0o600

    parent = output.parent
    parent.mkdir(parents=True, exist_ok=True)
    if parent.is_symlink() or not parent.is_dir():
        fail(f"output parent must be a real directory: {parent}")
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{output.name}.", suffix=".tmp", dir=str(parent)
    )
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


source_argument = Path(sys.argv[1])
output_argument = Path(sys.argv[2])
if not source_argument.exists() or source_argument.is_symlink() or not source_argument.is_dir():
    fail(f"source root must be a real directory: {source_argument}")
try:
    source_root = source_argument.resolve(strict=True)
except OSError as error:
    fail(f"source root is unavailable: {error}")
if not source_root.is_dir():
    fail(f"source root must be a directory: {source_root}")
try:
    output_path = output_argument.expanduser().absolute()
except OSError as error:
    fail(f"output path is invalid: {error}")

configuration, terms, resources = build_config(source_root)
write_atomically(output_path, configuration)
print(f"configured development profile: terms={terms} resources={resources} generation={configuration.split('generation: ', 1)[1].splitlines()[0]} output={output_path}")
PY
