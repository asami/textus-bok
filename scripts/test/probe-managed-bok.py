#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path, PurePosixPath


MCP_PROTOCOL_VERSION = "2025-11-25"
SEARCH_TERMS = "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms"
EXPLAIN_TERM = "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm"
READ_TOOLS = {
    SEARCH_TERMS,
    EXPLAIN_TERM,
    "org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences",
    "org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference",
}
MUTATION_SUFFIXES = ("replaceKnowledgeSource", "replace-knowledge-source")


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, file, code, msg, headers, newurl):
        return None


NO_REDIRECT_OPENER = urllib.request.build_opener(NoRedirectHandler())


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def field(value: object, *names: str) -> object:
    if not isinstance(value, dict):
        return None
    for name in names:
        if name in value:
            return value[name]
    return None


def unwrap(value: object) -> object:
    if isinstance(value, dict) and set(value) == {"value"}:
        return value["value"]
    return value


def body(value: object) -> dict:
    candidate = field(value, "body")
    return candidate if isinstance(candidate, dict) else value if isinstance(value, dict) else {}


def decode_json(raw: bytes, label: str) -> dict:
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise RuntimeError(f"{label} did not return a JSON object: {error}") from error
    require(isinstance(value, dict), f"{label} did not return a JSON object")
    return value


def request_json(request: urllib.request.Request, timeout: float) -> tuple[int, dict]:
    try:
        with NO_REDIRECT_OPENER.open(request, timeout=timeout) as response:
            return response.status, decode_json(response.read(), request.full_url)
    except urllib.error.HTTPError as error:
        detail = error.read()
        try:
            return error.code, decode_json(detail, request.full_url)
        except RuntimeError as decode_error:
            raise RuntimeError(f"HTTP {error.code} from {request.full_url}: {decode_error}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Could not reach local BoK runtime at {request.full_url}: {error}") from error


def get_json(base_url: str, path: str, timeout: float) -> tuple[int, dict]:
    request = urllib.request.Request(f"{base_url}{path}", headers={"Accept": "application/json"})
    return request_json(request, timeout)


def post_json(base_url: str, path: str, payload: dict, timeout: float) -> tuple[int, dict]:
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "MCP-Protocol-Version": MCP_PROTOCOL_VERSION,
        },
    )
    return request_json(request, timeout)


def source_term_count(source_root: Path) -> int:
    manifest_path = source_root / "metadata/cncf/knowledge-source.json"
    require(manifest_path.is_file() and not manifest_path.is_symlink(), f"KnowledgeSource manifest is missing: {manifest_path}")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise RuntimeError(f"KnowledgeSource manifest is invalid: {error}") from error
    require(isinstance(manifest, dict), "KnowledgeSource manifest must be an object")
    require(manifest.get("schemaVersion") == "cncf.knowledge-source.v1", "Unexpected KnowledgeSource schema")
    resources = manifest.get("resources")
    require(isinstance(resources, list), "KnowledgeSource manifest resources must be a list")
    glossary = [item for item in resources if isinstance(item, dict) and item.get("kind") == "glossary-terms"]
    require(len(glossary) == 1, f"Expected one glossary resource, found {len(glossary)}")
    href = glossary[0].get("href")
    require(isinstance(href, str) and href, "Glossary resource href is missing")
    require(not href.startswith("/") and "\\" not in href, f"Unsafe glossary href: {href!r}")
    parts = href.split("/")
    require(all(part not in ("", ".", "..") for part in parts), f"Unsafe glossary href: {href!r}")
    relative = PurePosixPath(href)
    require(str(relative) == href and relative.parts, f"Non-canonical glossary href: {href!r}")
    terms_path = source_root.joinpath(*relative.parts)
    require(terms_path.resolve(strict=False).is_relative_to(source_root), f"Glossary href escapes the source root: {href!r}")
    require(terms_path.is_file() and not terms_path.is_symlink(), f"Glossary resource is missing: {terms_path}")
    try:
        terms_document = json.loads(terms_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise RuntimeError(f"Glossary resource is invalid: {error}") from error
    terms = terms_document.get("terms") if isinstance(terms_document, dict) else None
    require(isinstance(terms, list), "Glossary resource must contain a terms list")
    return len(terms)


def selection_tuple(response: dict) -> tuple[object, object, object, object, object]:
    selected = field(body(response), "selection")
    require(isinstance(selected, dict), f"MCP response has no selected development profile: {response}")
    return (
        unwrap(field(selected, "resolvedProfile", "resolved_profile")),
        unwrap(field(selected, "datasetId", "dataset_id")),
        unwrap(field(selected, "sourceId", "source_id")),
        unwrap(field(selected, "generation")),
        unwrap(field(field(selected, "evidence"), "uri")),
    )


def term_identity(result: object) -> object:
    candidate = field(result, "term")
    return unwrap(field(candidate if isinstance(candidate, dict) else result, "termId", "term_id", "id"))


def mcp_call(base_url: str, request_id: str, tool_name: str, arguments: dict, timeout: float) -> dict:
    status, envelope = post_json(
        base_url,
        "/mcp",
        {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "tools/call",
            "params": {"name": tool_name, "arguments": arguments},
        },
        timeout,
    )
    require(200 <= status < 300, f"MCP {request_id} returned HTTP {status}: {envelope}")
    require(envelope.get("jsonrpc") == "2.0", f"MCP {request_id} returned an invalid JSON-RPC envelope")
    require(envelope.get("id") == request_id, f"MCP {request_id} returned a mismatched response id")
    require("error" not in envelope, f"MCP {request_id} returned a protocol error: {envelope}")
    result = envelope.get("result")
    require(isinstance(result, dict) and result.get("isError") is not True, f"MCP {request_id} failed: {envelope}")
    content = result.get("content") if isinstance(result, dict) else None
    require(isinstance(content, list) and len(content) == 1, f"MCP {request_id} returned unexpected content")
    text = content[0].get("text") if isinstance(content[0], dict) else None
    require(isinstance(text, str), f"MCP {request_id} returned no JSON text")
    try:
        decoded = json.loads(text)
    except json.JSONDecodeError as error:
        raise RuntimeError(f"MCP {request_id} returned malformed JSON: {error}") from error
    require(isinstance(decoded, dict), f"MCP {request_id} returned a non-object body")
    return decoded


def check_readiness(base_url: str, timeout: float) -> None:
    status, response = get_json(base_url, "/rest/v1/admin/system/status", timeout)
    require(200 <= status < 300, f"Managed BoK readiness returned HTTP {status}: {response}")
    state = unwrap(field(body(response), "status"))
    require(state in {"UP", "up", "ready", "READY"}, f"Managed BoK is not ready: {response}")


def run(arguments: argparse.Namespace) -> None:
    parsed = urllib.parse.urlparse(arguments.base_url)
    require(parsed.scheme == "http" and parsed.hostname in {"127.0.0.1", "localhost", "::1"}, "--base-url must be an HTTP loopback URL")
    base_url = arguments.base_url.rstrip("/")
    root = arguments.source_root.expanduser().resolve(strict=True)
    require(root.is_dir(), f"--source-root must be a directory: {root}")
    count = source_term_count(root)
    require(count == arguments.expected_term_count, f"Source term count mismatch: expected={arguments.expected_term_count} actual={count}")

    check_readiness(base_url, arguments.timeout)
    status, listed = post_json(
        base_url,
        "/mcp",
        {"jsonrpc": "2.0", "id": "managed-tools", "method": "tools/list", "params": {}},
        arguments.timeout,
    )
    require(200 <= status < 300, f"MCP tools/list returned HTTP {status}: {listed}")
    names = {
        item.get("name")
        for item in listed.get("result", {}).get("tools", [])
        if isinstance(item, dict) and isinstance(item.get("name"), str)
    }
    require(READ_TOOLS.issubset(names), f"Managed BoK read tools are incomplete: {sorted(names)}")
    require(not any(any(name.endswith(suffix) for suffix in MUTATION_SUFFIXES) for name in names), f"A mutation operation is MCP-ready: {sorted(names)}")

    expected_selection = (
        "development",
        arguments.dataset_id,
        arguments.source_id,
        arguments.generation,
        arguments.evidence_uri,
    )
    present = mcp_call(
        base_url,
        "managed-present",
        SEARCH_TERMS,
        {"query": arguments.present_term_id, "profile": "development", "limit": 10},
        arguments.timeout,
    )
    present_body = body(present)
    present_results = field(present_body, "results")
    require(unwrap(field(present_body, "status")) == "matched", f"Expected managed term was not found: {present}")
    require(isinstance(present_results, list) and len(present_results) == 1, f"Expected one managed term result: {present}")
    require(term_identity(present_results[0]) == arguments.present_term_id, f"Managed term identity mismatch: {present}")
    require(selection_tuple(present) == expected_selection, f"Managed development attribution mismatch: {present}")

    absent = mcp_call(
        base_url,
        "managed-absent",
        EXPLAIN_TERM,
        {"term": arguments.absent_term_id, "profile": "development"},
        arguments.timeout,
    )
    absent_body = body(absent)
    require(unwrap(field(absent_body, "status")) == "no-match", f"Unexpected managed term was present: {absent}")
    require("term" not in absent_body and "results" not in absent_body, f"Unexpected absent-term payload: {absent}")
    require("result" not in absent_body or absent_body["result"] is None, f"Unexpected absent-term result: {absent}")
    require(selection_tuple(absent) == expected_selection, f"Absent-term attribution mismatch: {absent}")

    print(
        f"MANAGED_BOK_PROBE_OK profile=development generation={arguments.generation} "
        f"term_count={count} present=1 absent=0 mutation_tools=0"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify a ready, explicitly attributed managed Textus BoK runtime through MCP reads.")
    parser.add_argument("--base-url", default="http://127.0.0.1:18014")
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--present-term-id", "--present-term", dest="present_term_id", required=True)
    parser.add_argument("--absent-term-id", "--absent-term", dest="absent_term_id", required=True)
    parser.add_argument("--expected-term-count", required=True, type=int)
    parser.add_argument("--dataset-id", default="simplemodeling-bok-development")
    parser.add_argument("--source-id", default="simplemodeling-development")
    parser.add_argument("--generation", required=True)
    parser.add_argument("--evidence-uri", default="https://www.simplemodeling.org/")
    parser.add_argument("--timeout", default=20.0, type=float)
    arguments = parser.parse_args()
    require(arguments.expected_term_count > 0, "--expected-term-count must be positive")
    require(arguments.timeout > 0, "--timeout must be positive")
    run(arguments)


if __name__ == "__main__":
    main()
