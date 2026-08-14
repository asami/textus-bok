#!/usr/bin/env python3

import argparse
import html
import json
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


MCP_PROTOCOL_VERSION = "2025-11-25"
READ_TOOLS = {
    "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms",
    "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm",
    "org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences",
    "org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference",
}

_server_pid: int | None = None
_server_pid_signature: str | None = None
_listener_pid: int | None = None
_listener_pid_signature: str | None = None
_configured_port: int | None = None

PROFILES = (
    {
        "name": "official",
        "project_id": None,
        "source_id": "profile-official-source",
        "dataset_id": "profile-official-dataset",
        "generation": "2026-08-15T00:00:00Z",
        "evidence_uri": "https://evidence.example/textus-bok/profile-official",
        "term_id": "profile-official-marker",
        "isolation_query": "Official",
        "marker": "Official Profile Marker",
        "definition": "The official representative profile generation.",
        "node_id": "profile-official-node",
        "source_ref_value": "profile-official-source-ref",
        "source_ref_uri": "https://evidence.example/textus-bok/profile-official/source",
    },
    {
        "name": "development",
        "project_id": None,
        "source_id": "profile-development-source",
        "dataset_id": "profile-development-dataset",
        "generation": "2026-08-15T01:00:00Z",
        "evidence_uri": "https://evidence.example/textus-bok/profile-development",
        "term_id": "profile-development-marker",
        "isolation_query": "Development",
        "marker": "Development Profile Marker",
        "definition": "The development representative profile generation.",
        "node_id": "profile-development-node",
        "source_ref_value": "profile-development-source-ref",
        "source_ref_uri": "https://evidence.example/textus-bok/profile-development/source",
    },
    {
        "name": "project",
        "project_id": "project-alpha",
        "source_id": "profile-project-alpha-source",
        "dataset_id": "profile-project-alpha-dataset",
        "generation": "2026-08-15T02:00:00Z",
        "evidence_uri": "https://evidence.example/textus-bok/profile-project-alpha",
        "term_id": "profile-project-alpha-marker",
        "isolation_query": "Alpha",
        "marker": "Project Alpha Marker",
        "definition": "The project-alpha representative profile generation.",
        "node_id": "profile-project-alpha-node",
        "source_ref_value": "profile-project-alpha-source-ref",
        "source_ref_uri": "https://evidence.example/textus-bok/profile-project-alpha/source",
    },
    {
        "name": "project",
        "project_id": "project-beta",
        "source_id": "profile-project-beta-source",
        "dataset_id": "profile-project-beta-dataset",
        "generation": "2026-08-15T03:00:00Z",
        "evidence_uri": "https://evidence.example/textus-bok/profile-project-beta",
        "term_id": "profile-project-beta-marker",
        "isolation_query": "Beta",
        "marker": "Project Beta Marker",
        "definition": "The project-beta representative profile generation.",
        "node_id": "profile-project-beta-node",
        "source_ref_value": "profile-project-beta-source-ref",
        "source_ref_uri": "https://evidence.example/textus-bok/profile-project-beta/source",
    },
)


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _value(value: object) -> object:
    if isinstance(value, dict) and set(value) == {"value"}:
        return value["value"]
    return value


def _field(value: object, *names: str) -> object:
    if not isinstance(value, dict):
        return None
    for name in names:
        if name in value:
            return value[name]
    return None


def _body(value: object) -> dict:
    if isinstance(value, dict) and isinstance(value.get("body"), dict):
        return value["body"]
    return value if isinstance(value, dict) else {}


def _decode_json(raw: bytes, label: str) -> dict:
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise RuntimeError(f"{label} did not return a JSON object: {error}") from error
    if not isinstance(value, dict):
        raise RuntimeError(f"{label} did not return a JSON object")
    return value


def _process_signature(pid: int) -> str | None:
    result = subprocess.run(
        ["ps", "-o", "lstart=", "-p", str(pid)],
        check=False,
        capture_output=True,
        text=True,
    )
    signature = result.stdout.strip()
    return signature if signature else None


def _require_ownership_guard() -> None:
    _require(
        _server_pid is not None
        and _server_pid_signature is not None
        and _listener_pid is not None
        and _listener_pid_signature is not None
        and _configured_port is not None,
        "Profile-selection SAR ownership guard is not configured",
    )
    serverpid = _server_pid
    serverpidsignature = _server_pid_signature
    listenerpid = _listener_pid
    listenerpidsignature = _listener_pid_signature
    configuredport = _configured_port
    _require(serverpid > 0 and listenerpid > 0 and 1 <= configuredport <= 65535, "Profile-selection SAR ownership guard has invalid PID or port input")
    _require(_process_signature(serverpid) == serverpidsignature, f"Launched server PID {serverpid} no longer has its captured identity")
    _require(_process_signature(listenerpid) == listenerpidsignature, f"Listener PID {listenerpid} no longer has its captured identity")
    listener_result = subprocess.run(
        ["lsof", "-nP", f"-iTCP:{configuredport}", "-sTCP:LISTEN", "-t"],
        check=False,
        capture_output=True,
        text=True,
    )
    listener_lines = [line.strip() for line in listener_result.stdout.splitlines() if line.strip()]
    _require(len(listener_lines) == 1 and listener_lines[0].isdecimal(), f"Configured port {configuredport} does not have exactly one numeric listener: {listener_lines}")
    _require(int(listener_lines[0]) == listenerpid, f"Configured port {configuredport} listener is not the captured PID {listenerpid}: {listener_lines}")
    candidate = listenerpid
    for _ in range(128):
        if candidate == serverpid:
            return
        parent_result = subprocess.run(
            ["ps", "-o", "ppid=", "-p", str(candidate)],
            check=False,
            capture_output=True,
            text=True,
        )
        parent_text = parent_result.stdout.strip()
        _require(parent_text.isdecimal(), f"Could not resolve parent PID while proving listener {listenerpid} belongs to server {serverpid}")
        parentpid = int(parent_text)
        _require(parentpid not in {0, candidate}, f"Listener {listenerpid} does not belong to server {serverpid}")
        candidate = parentpid
    raise AssertionError(f"Listener {listenerpid} does not belong to server {serverpid} within the ownership bound")


def _request_json(request: urllib.request.Request, timeout: float) -> tuple[int, dict]:
    _require_ownership_guard()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, _decode_json(response.read(), request.full_url)
    except urllib.error.HTTPError as error:
        detail = error.read()
        # Structured HTTP failures are expected for invalid selections. Preserve
        # their body and status so callers can assert the domain failure code.
        try:
            return error.code, _decode_json(detail, request.full_url)
        except RuntimeError:
            text = detail.decode("utf-8", errors="replace")
            match = re.search(
                r"(invalid-selection|project-identity-required|unregistered|unavailable|stale|ambiguous|unauthorized|conflicting-selection)",
                text,
            )
            body = {"raw": text}
            if match is not None:
                body["code"] = match.group(1)
            return error.code, body
    finally:
        _require_ownership_guard()


def _post_json(
    base_url: str,
    path: str,
    payload: dict,
    timeout: float,
    *,
    mcp: bool = False,
) -> tuple[int, dict]:
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if mcp:
        headers["MCP-Protocol-Version"] = MCP_PROTOCOL_VERSION
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
    )
    return _request_json(request, timeout)


def _get_text(
    base_url: str,
    path: str,
    parameters: dict[str, str],
    timeout: float,
) -> tuple[int, str]:
    query = urllib.parse.urlencode(parameters)
    url = f"{base_url}{path}?{query}" if query else f"{base_url}{path}"
    request = urllib.request.Request(url, headers={"Accept": "text/html"})
    _require_ownership_guard()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read().decode(
                response.headers.get_content_charset() or "utf-8"
            )
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {error.code} from {url}: {detail}") from error
    finally:
        _require_ownership_guard()


def _require_mcp_success_envelope(envelope: dict, request_id: str) -> None:
    _require(envelope.get("jsonrpc") == "2.0", f"MCP {request_id} returned an invalid JSON-RPC version: {envelope}")
    _require(envelope.get("id") == request_id, f"MCP {request_id} returned a mismatched response id: {envelope}")
    _require("error" not in envelope, f"MCP {request_id} failed: {envelope}")


def _mcp_call(
    base_url: str,
    request_id: str,
    name: str,
    arguments: dict,
    timeout: float,
) -> dict:
    status, envelope = _post_json(
        base_url,
        "/mcp",
        {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        },
        timeout,
        mcp=True,
    )
    _require(200 <= status < 300, f"MCP {request_id} returned HTTP {status}: {envelope}")
    _require_mcp_success_envelope(envelope, request_id)
    content = _field(envelope.get("result", {}), "content") or []
    _require(len(content) == 1, f"MCP {request_id} returned unexpected content: {envelope}")
    text = _field(content[0], "text")
    _require(isinstance(text, str), f"MCP {request_id} returned no text: {envelope}")
    try:
        decoded = json.loads(text)
    except json.JSONDecodeError as error:
        raise RuntimeError(f"MCP {request_id} returned non-JSON text: {text}") from error
    _require(isinstance(decoded, dict), f"MCP {request_id} returned a non-object result: {decoded}")
    return decoded


def _selection_arguments(profile: dict) -> dict:
    arguments = {}
    if profile["name"] != "official":
        arguments["profile"] = profile["name"]
    if profile["project_id"] is not None:
        arguments["projectId"] = profile["project_id"]
    return arguments


def _selection_tuple(response: dict) -> tuple[object, object, object, object, object, object]:
    selection = _field(_body(response), "selection")
    _require(isinstance(selection, dict), f"Response has no resolved selection: {response}")
    evidence = _field(selection, "evidence")
    return (
        _value(_field(selection, "resolvedProfile", "resolved_profile")),
        _value(_field(selection, "projectId", "project_id")),
        _value(_field(selection, "datasetId", "dataset_id")),
        _value(_field(selection, "sourceId", "source_id")),
        _value(_field(selection, "generation")),
        (
            _value(_field(evidence, "uri")),
            _value(_field(evidence, "sourceId", "source_id")),
        ),
    )


def _expected_selection(profile: dict) -> tuple[object, object, object, object, object, object]:
    return (
        profile["name"],
        profile["project_id"],
        profile["dataset_id"],
        profile["source_id"],
        profile["generation"],
        (profile["evidence_uri"], profile["source_id"]),
    )


def _assert_selection(response: dict, profile: dict, label: str) -> None:
    actual = _selection_tuple(response)
    _require(
        actual == _expected_selection(profile),
        f"{label} resolved the wrong selection: expected={_expected_selection(profile)} actual={actual}",
    )


def _assert_no_foreign_content(
    response: dict,
    profile: dict,
    label: str,
    ignored: tuple[str, ...] = (),
) -> None:
    encoded = json.dumps(_body(response), ensure_ascii=False)
    for other in PROFILES:
        if other is profile:
            continue
        for marker in (
            other["marker"],
            other["term_id"],
            other["node_id"],
            other["source_id"],
            other["dataset_id"],
            other["generation"],
        ):
            if marker in ignored:
                continue
            _require(marker not in encoded, f"{label} leaked foreign marker {marker}: {response}")


def _term_record(response: dict, profile: dict, label: str) -> dict:
    body = _body(response)
    _require(_value(_field(body, "status")) == "matched", f"{label} did not match: {response}")
    results = _field(body, "results") or []
    _require(isinstance(results, list) and len(results) == 1, f"{label} returned non-isolated results: {response}")
    result = results[0]
    term = _field(result, "term")
    term = term if isinstance(term, dict) else result
    _require(_value(_field(term, "termId", "term_id")) == profile["term_id"], f"{label} term id mismatch: {response}")
    _require(_value(_field(term, "title")) == profile["marker"], f"{label} marker mismatch: {response}")
    _require(_value(_field(term, "definition")) == profile["definition"], f"{label} definition mismatch: {response}")
    evidence = _field(term, "evidence")
    _require(_value(_field(evidence, "sourceId", "source_id")) == profile["source_id"], f"{label} record evidence mismatch: {response}")
    _assert_selection(response, profile, label)
    _assert_no_foreign_content(response, profile, label)
    return term


def _assert_no_match_terms(
    response: dict,
    profile: dict,
    foreign: dict,
    label: str,
) -> None:
    body = _body(response)
    _require(_value(_field(body, "status")) == "no-match", f"{label} unexpectedly matched: {response}")
    results = _field(body, "results") or []
    _require(isinstance(results, list) and not results, f"{label} returned foreign results: {response}")
    _assert_selection(response, profile, label)
    _assert_no_foreign_content(response, profile, label, ignored=(foreign["marker"],))


def _canonical_evidence(value: object) -> dict:
    return {
        "uri": _value(_field(value, "uri")),
        "sourceId": _value(_field(value, "sourceId", "source_id")),
    }


def _canonical_term(value: object) -> dict:
    return {
        "termId": _value(_field(value, "termId", "term_id")),
        "title": _value(_field(value, "title")),
        "definition": _value(_field(value, "definition")),
        "evidence": _canonical_evidence(_field(value, "evidence")),
    }


def _canonical_source(value: object) -> dict:
    reference = _field(value, "sourceReference", "source_reference")
    return {
        "datasetId": _value(_field(value, "datasetId", "dataset_id")),
        "sourceId": _value(_field(value, "sourceId", "source_id")),
        "generation": _value(_field(value, "generation")),
        "sourceReference": {
            "kind": _value(_field(reference, "kind")),
            "value": _value(_field(reference, "value")),
            "uri": _value(_field(reference, "uri")),
        },
        "sourceTruncated": _value(_field(value, "sourceTruncated", "source_truncated")),
    }


def _canonical_node(value: object) -> dict:
    terms = _field(value, "terms") or []
    termids = _field(value, "termIds", "term_ids") or []
    return {
        "datasetId": _value(_field(value, "datasetId", "dataset_id")),
        "sourceId": _value(_field(value, "sourceId", "source_id")),
        "nodeId": _value(_field(value, "nodeId", "node_id")),
        "label": _value(_field(value, "label")),
        "kind": _value(_field(value, "kind")),
        "category": _value(_field(value, "category")),
        "termIds": [_value(item) for item in termids],
        "terms": [_canonical_term(item) for item in terms],
        "tags": [_value(item) for item in (_field(value, "tags") or [])],
        "evidence": _canonical_evidence(_field(value, "evidence")),
    }


def _canonical_relationship(value: object) -> dict:
    return {
        "datasetId": _value(_field(value, "datasetId", "dataset_id")),
        "sourceId": _value(_field(value, "sourceId", "source_id")),
        "subjectId": _value(_field(value, "subjectId", "subject_id")),
        "predicate": _value(_field(value, "predicate")),
        "objectId": _value(_field(value, "objectId", "object_id")),
        "evidence": _canonical_evidence(_field(value, "evidence")),
    }


def _map_projection(response: dict) -> dict:
    body = _body(response)
    selection = _selection_tuple(body)
    return {
        "status": _value(_field(body, "status")),
        "selection": selection,
        "sources": [_canonical_source(item) for item in (_field(body, "sources") or [])],
        "nodes": [_canonical_node(item) for item in (_field(body, "nodes") or [])],
        "relationships": [_canonical_relationship(item) for item in (_field(body, "relationships") or [])],
        "truncated": _value(_field(body, "truncated")),
    }


def _assert_map_response(response: dict, profile: dict, label: str) -> dict:
    body = _body(response)
    _require(_value(_field(body, "status")) == "matched", f"{label} did not match: {response}")
    _assert_selection(response, profile, label)
    sources = _field(body, "sources") or []
    _require(len(sources) == 1, f"{label} selected more than one source: {response}")
    source = sources[0]
    _require(_value(_field(source, "datasetId", "dataset_id")) == profile["dataset_id"], f"{label} dataset mismatch: {response}")
    _require(_value(_field(source, "sourceId", "source_id")) == profile["source_id"], f"{label} source mismatch: {response}")
    _require(_value(_field(source, "generation")) == profile["generation"], f"{label} generation mismatch: {response}")
    reference = _field(source, "sourceReference", "source_reference")
    _require(_value(_field(reference, "kind")) == "bok-site", f"{label} source reference kind mismatch: {response}")
    _require(_value(_field(reference, "value")) == profile["source_ref_value"], f"{label} source reference value mismatch: {response}")
    _require(_value(_field(reference, "uri")) == profile["source_ref_uri"], f"{label} source reference URI mismatch: {response}")
    nodes = _field(body, "nodes") or []
    _require(len(nodes) == 1, f"{label} returned non-isolated nodes: {response}")
    node = nodes[0]
    _require(_value(_field(node, "nodeId", "node_id")) == profile["node_id"], f"{label} node id mismatch: {response}")
    _require(_value(_field(node, "label")) == profile["marker"], f"{label} node marker mismatch: {response}")
    _require(profile["term_id"] in [_value(item) for item in (_field(node, "termIds", "term_ids") or [])], f"{label} node term identity mismatch: {response}")
    _require(not (_field(body, "relationships") or []), f"{label} returned an unexpected relationship: {response}")
    _assert_no_foreign_content(response, profile, label)
    return _map_projection(response)


def _assert_no_match_map_response(
    response: dict,
    profile: dict,
    foreign: dict,
    label: str,
) -> dict:
    body = _body(response)
    _require(_value(_field(body, "status")) == "no-match", f"{label} unexpectedly matched: {response}")
    _assert_selection(response, profile, label)
    sources = _field(body, "sources") or []
    _require(len(sources) == 1, f"{label} selected an unexpected source count: {response}")
    source = sources[0]
    _require(_value(_field(source, "datasetId", "dataset_id")) == profile["dataset_id"], f"{label} dataset mismatch: {response}")
    _require(_value(_field(source, "sourceId", "source_id")) == profile["source_id"], f"{label} source mismatch: {response}")
    _require(_value(_field(source, "generation")) == profile["generation"], f"{label} generation mismatch: {response}")
    nodes = _field(body, "nodes") or []
    relationships = _field(body, "relationships") or []
    _require(isinstance(nodes, list) and not nodes, f"{label} returned foreign nodes: {response}")
    _require(isinstance(relationships, list) and not relationships, f"{label} returned foreign relationships: {response}")
    _assert_no_foreign_content(response, profile, label, ignored=(foreign["node_id"],))
    return _map_projection(response)


def _script_model(page: str) -> dict:
    match = re.search(
        r'<script[^>]*id="bok-knowledge-map-data"[^>]*>(.*?)</script>',
        page,
        flags=re.DOTALL | re.IGNORECASE,
    )
    _require(match is not None, "Static Form map has no operation-result data script")
    return _body(json.loads(html.unescape(match.group(1))))


def _failure_code(value: object) -> str | None:
    if isinstance(value, dict):
        for name in ("code", "reason", "failureCode", "failure_code", "status"):
            candidate = value.get(name)
            if isinstance(candidate, str) and candidate in {
                "invalid-selection",
                "project-identity-required",
                "unregistered",
                "unavailable",
                "stale",
                "ambiguous",
                "unauthorized",
                "conflicting-selection",
            }:
                return candidate
        for candidate in value.values():
            found = _failure_code(candidate)
            if found is not None:
                return found
    elif isinstance(value, list):
        for candidate in value:
            found = _failure_code(candidate)
            if found is not None:
                return found
    return None


def _read_json(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    _require(isinstance(value, dict), f"Fixture is not a JSON object: {path}")
    return value


def _check_fixture(root: Path, profile: dict) -> None:
    profile_root = root / ("official" if profile["name"] == "official" else "development" if profile["name"] == "development" else profile["project_id"])
    manifest = _read_json(profile_root / "metadata/cncf/knowledge-source.json")
    resources = manifest.get("resources")
    _require(manifest.get("schemaVersion") == "cncf.knowledge-source.v1", f"Fixture manifest schema mismatch: {profile_root}")
    _require(
        resources == [
            {"kind": "glossary-terms", "href": "metadata/glossary/terms.json", "mediaType": "application/json"},
            {"kind": "rdf-graph-summary", "href": "metadata/rdf/graph.json", "mediaType": "application/json"},
        ],
        f"Fixture resources are not isolated glossary and graph inputs: {profile_root}",
    )
    terms = _read_json(profile_root / "metadata/glossary/terms.json").get("terms")
    _require(isinstance(terms, list) and len(terms) == 1, f"Fixture term count mismatch: {profile_root}")
    _require(terms[0].get("id") == profile["term_id"], f"Fixture term id mismatch: {profile_root}")
    _require(terms[0].get("title") == profile["marker"], f"Fixture term marker mismatch: {profile_root}")
    graph = _read_json(profile_root / "metadata/rdf/graph.json")
    _require(graph.get("schemaVersion") == "cozy.rdf-graph-summary.v1", f"Fixture graph schema mismatch: {profile_root}")
    _require(graph.get("sourceRef", {}).get("value") == profile["source_ref_value"], f"Fixture graph sourceRef mismatch: {profile_root}")
    _require(graph.get("nodes", [{}])[0].get("id") == profile["node_id"], f"Fixture node id mismatch: {profile_root}")
    _require(graph.get("nodes", [{}])[0].get("label") == profile["marker"], f"Fixture node marker mismatch: {profile_root}")


def _run(base_url: str, fixture_root: Path, timeout: float) -> None:
    parsed = urllib.parse.urlparse(base_url)
    _require(parsed.scheme == "http" and parsed.hostname in {"127.0.0.1", "localhost"}, f"Probe requires a loopback URL: {base_url}")
    for profile in PROFILES:
        _check_fixture(fixture_root, profile)

    for index, profile in enumerate(PROFILES):
        status, replacement = _post_json(
            base_url,
            "/rest/v1/bok/bok-retrieval/replace-knowledge-source",
            {
                "source": {
                    "sourceId": profile["source_id"],
                    "datasetId": profile["dataset_id"],
                    "generation": profile["generation"],
                    "resource": "urn:textus:bok:request-side-resource-must-not-be-read",
                }
            },
            timeout,
        )
        replacement_body = _body(replacement)
        _require(200 <= status < 300, f"Replacement for {profile['name']} {profile['project_id']} failed: {replacement}")
        _require(_value(_field(replacement_body, "status")) == "complete", f"Replacement was not complete: {replacement}")
        _require(_value(_field(replacement_body, "sourceId", "source_id")) == profile["source_id"], f"Replacement source mismatch: {replacement}")
        _require(_value(_field(replacement_body, "datasetId", "dataset_id")) == profile["dataset_id"], f"Replacement dataset mismatch: {replacement}")
        _require(_value(_field(replacement_body, "generation")) == profile["generation"], f"Replacement generation mismatch: {replacement}")

    status, listed = _post_json(
        base_url,
        "/mcp",
        {"jsonrpc": "2.0", "id": "profile-tools", "method": "tools/list", "params": {}},
        timeout,
        mcp=True,
    )
    _require(200 <= status < 300, f"MCP tools/list returned HTTP {status}: {listed}")
    _require_mcp_success_envelope(listed, "profile-tools")
    names = {tool.get("name") for tool in (_field(listed.get("result", {}), "tools") or [])}
    component_names = {name for name in names if isinstance(name, str) and name.startswith("org.simplemodeling.textus.Bok.BokRetrieval.")}
    _require(component_names == READ_TOOLS, f"Qualified BoK MCP read tools differ: {component_names}")
    _require(not any(name.endswith(".getKnowledgeMap") for name in names if isinstance(name, str)), f"Knowledge Map leaked into MCP: {names}")
    _require(not any(name.endswith(".replaceKnowledgeSource") for name in names if isinstance(name, str)), f"Source replacement leaked into MCP: {names}")

    for index, profile in enumerate(PROFILES):
        foreign = PROFILES[(index + 1) % len(PROFILES)]
        arguments = {"query": profile["marker"], "limit": 10}
        arguments.update(_selection_arguments(profile))
        status, rest_terms = _post_json(base_url, "/rest/v1/bok/bok-retrieval/search-terms", arguments, timeout)
        _require(200 <= status < 300, f"REST term search failed: {rest_terms}")
        _term_record(rest_terms, profile, f"REST {profile['marker']}")

        map_arguments = {"focus": profile["node_id"], "nodeLimit": 128, "relationshipLimit": 256}
        map_arguments.update(_selection_arguments(profile))
        status, rest_map = _post_json(base_url, "/rest/v1/bok/bok-retrieval/get-knowledge-map", map_arguments, timeout)
        _require(200 <= status < 300, f"REST Knowledge Map failed: {rest_map}")
        rest_projection = _assert_map_response(rest_map, profile, f"REST {profile['marker']} map")

        web_parameters = {"focus": profile["node_id"]}
        web_parameters.update({key: value for key, value in _selection_arguments(profile).items()})
        status, page = _get_text(base_url, "/web/bok/textus-bok/map", web_parameters, timeout)
        _require(200 <= status < 300, f"Static Form map failed with HTTP {status}")
        web_map = _script_model(page)
        web_projection = _assert_map_response(web_map, profile, f"Web {profile['marker']} map")
        _require(web_projection == rest_projection, f"Web and REST map data differ for {profile['marker']}: web={web_projection} rest={rest_projection}")

        mcp_arguments = {"query": profile["marker"], "limit": 10}
        mcp_arguments.update(_selection_arguments(profile))
        mcp_terms = _mcp_call(
            base_url,
            f"profile-terms-{index}",
            "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms",
            mcp_arguments,
            timeout,
        )
        _term_record(mcp_terms, profile, f"MCP {profile['marker']}")
        _require(
            _selection_tuple(mcp_terms) == _selection_tuple(rest_terms),
            f"MCP and REST term attribution differs for {profile['marker']}",
        )

        negative_arguments = {"query": foreign["isolation_query"], "limit": 10}
        negative_arguments.update(_selection_arguments(profile))
        status, negative_rest_terms = _post_json(
            base_url,
            "/rest/v1/bok/bok-retrieval/search-terms",
            negative_arguments,
            timeout,
        )
        _require(200 <= status < 300, f"REST negative term search failed: {negative_rest_terms}")
        _assert_no_match_terms(
            negative_rest_terms,
            profile,
            foreign,
            f"REST {profile['marker']} foreign {foreign['marker']}",
        )

        negative_mcp_terms = _mcp_call(
            base_url,
            f"profile-negative-terms-{index}",
            "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms",
            negative_arguments,
            timeout,
        )
        _assert_no_match_terms(
            negative_mcp_terms,
            profile,
            foreign,
            f"MCP {profile['marker']} foreign {foreign['marker']}",
        )
        _require(
            _selection_tuple(negative_mcp_terms) == _selection_tuple(negative_rest_terms),
            f"MCP and REST negative term attribution differs for {profile['marker']}",
        )

        negative_map_arguments = {"focus": foreign["node_id"], "nodeLimit": 128, "relationshipLimit": 256}
        negative_map_arguments.update(_selection_arguments(profile))
        status, negative_rest_map = _post_json(
            base_url,
            "/rest/v1/bok/bok-retrieval/get-knowledge-map",
            negative_map_arguments,
            timeout,
        )
        _require(200 <= status < 300, f"REST negative Knowledge Map failed: {negative_rest_map}")
        negative_rest_projection = _assert_no_match_map_response(
            negative_rest_map,
            profile,
            foreign,
            f"REST {profile['marker']} foreign map",
        )

        negative_web_parameters = {"focus": foreign["node_id"]}
        negative_web_parameters.update(_selection_arguments(profile))
        status, negative_page = _get_text(base_url, "/web/bok/textus-bok/map", negative_web_parameters, timeout)
        _require(200 <= status < 300, f"Static Form negative map failed with HTTP {status}")
        negative_web_map = _script_model(negative_page)
        negative_web_projection = _assert_no_match_map_response(
            negative_web_map,
            profile,
            foreign,
            f"Web {profile['marker']} foreign map",
        )
        _require(
            negative_web_projection == negative_rest_projection,
            f"Web and REST negative map data differ for {profile['marker']}: web={negative_web_projection} rest={negative_rest_projection}",
        )

    incomplete_status, incomplete = _post_json(
        base_url,
        "/rest/v1/bok/bok-retrieval/search-terms",
        {"query": "Official Profile Marker", "profile": "project"},
        timeout,
    )
    _require(incomplete_status >= 400, f"Incomplete project selection unexpectedly succeeded: {incomplete}")
    _require(_failure_code(incomplete) == "project-identity-required", f"Incomplete project failure is not structured: {incomplete}")

    unknown_status, unknown = _post_json(
        base_url,
        "/rest/v1/bok/bok-retrieval/search-terms",
        {"query": "Official Profile Marker", "profile": "project", "projectId": "project-missing"},
        timeout,
    )
    _require(unknown_status >= 400, f"Unknown project selection unexpectedly succeeded: {unknown}")
    _require(_failure_code(unknown) == "unregistered", f"Unknown project failure is not structured: {unknown}")

    print(
        "BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 "
        "web_maps=4 mcp_terms=4 negative_rest_terms=4 negative_mcp_terms=4 "
        "negative_rest_maps=4 negative_web_maps=4"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify one representative BoK profile-selection SAR.")
    parser.add_argument("--base-url", default="http://127.0.0.1:19547")
    parser.add_argument("--fixture-root", required=True, type=Path)
    parser.add_argument("--timeout", default=30.0, type=float)
    parser.add_argument("--server-pid", required=True, type=int)
    parser.add_argument("--server-pid-signature", required=True)
    parser.add_argument("--listener-pid", required=True, type=int)
    parser.add_argument("--listener-pid-signature", required=True)
    parser.add_argument("--configured-port", required=True, type=int)
    arguments = parser.parse_args()
    try:
        global _server_pid, _server_pid_signature, _listener_pid, _listener_pid_signature, _configured_port
        _server_pid = arguments.server_pid
        _server_pid_signature = arguments.server_pid_signature
        _listener_pid = arguments.listener_pid
        _listener_pid_signature = arguments.listener_pid_signature
        _configured_port = arguments.configured_port
        _require_ownership_guard()
        _run(arguments.base_url.rstrip("/"), arguments.fixture_root.resolve(), arguments.timeout)
        return 0
    except (AssertionError, KeyError, OSError, RuntimeError, TypeError, ValueError) as error:
        print(f"BOK_PROFILE_SELECTION_SAR_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
