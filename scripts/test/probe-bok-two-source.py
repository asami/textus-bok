#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.parse
import urllib.request
from typing import NoReturn


MCP_PROTOCOL_VERSION = "2025-11-25"
BOK_TOOL_PREFIX = "org.simplemodeling.textus.Bok."
SIE_TOOL_PREFIX = "org.simplemodeling.textus.SemanticIntegrationEngine."
SCRAPER_TOOL_PREFIX = "org.simplemodeling.textus.Scraper."
EXPLAIN_TERM = "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm"
KNOWLEDGE_MAP = "org.simplemodeling.textus.Bok.BokRetrieval.getKnowledgeMap"
REPLACE_PATH = "/rest/v1/bok/bok-retrieval/replace-knowledge-source"
READ_TOOLS = {
    "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms",
    EXPLAIN_TERM,
    "org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences",
    "org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference",
    KNOWLEDGE_MAP,
    "org.simplemodeling.textus.Bok.BokRetrieval.searchSemanticKnowledge",
    "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticManifest",
    "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticResource",
    "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticSection",
    "org.simplemodeling.textus.Bok.BokRetrieval.discoverSemanticKnowledge",
}
SIMPLEMODELING = {
    "source_id": "simplemodeling-development",
    "dataset_id": "simplemodeling-bok-development",
    "generation": "sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f",
    "evidence_uri": "https://www.simplemodeling.org/",
    "evidence_source_id": "simplemodeling-development",
    "term_id": "literate-modeling:story",
}
KNOWLEDGEHUB = {
    "source_id": "knowledgehub",
    "dataset_id": "knowledgehub-bok",
    "generation": "sha256:ce991b30a90b6bb6c65f4b3593124e78d2e2ecd28d1cacbcd2258abb30e16608",
    "evidence_uri": "https://www.asamioffice.com/kokubunji/knowledgehub/",
    "evidence_source_id": "knowledgehub",
    "term_id": "technology:embedding",
}
MEMBERS = tuple(sorted((SIMPLEMODELING, KNOWLEDGEHUB), key=lambda source: (
    source["source_id"], source["dataset_id"], source["generation"]
)))


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        raise RuntimeError(f"loopback-only probe refused redirect to {newurl}")


NO_REDIRECT_OPENER = urllib.request.build_opener(NoRedirectHandler())


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def value(item: object) -> object:
    if isinstance(item, dict) and set(item) == {"value"}:
        return item["value"]
    return item


def field(item: object, *names: str) -> object:
    if not isinstance(item, dict):
        return None
    for name in names:
        if name in item:
            return item[name]
    return None


def body(item: object) -> dict:
    if isinstance(item, dict) and isinstance(item.get("body"), dict):
        return item["body"]
    return item if isinstance(item, dict) else {}


def decode_json(raw: bytes, label: str) -> dict:
    try:
        decoded = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise RuntimeError(f"{label} did not return JSON: {error}") from error
    if not isinstance(decoded, dict):
        raise RuntimeError(f"{label} did not return a JSON object")
    return decoded


def request_json(request: urllib.request.Request, timeout: float) -> tuple[int, dict]:
    try:
        with NO_REDIRECT_OPENER.open(request, timeout=timeout) as response:
            return response.status, decode_json(response.read(), request.full_url)
    except urllib.error.HTTPError as error:
        return error.code, decode_json(error.read(), request.full_url)
    except urllib.error.URLError as error:
        raise RuntimeError(f"could not reach loopback BoK runtime at {request.full_url}: {error}") from error


def post_json(base_url: str, path: str, payload: dict, timeout: float, *, mcp: bool = False) -> tuple[int, dict]:
    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    if mcp:
        headers["MCP-Protocol-Version"] = MCP_PROTOCOL_VERSION
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
    )
    return request_json(request, timeout)


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
        mcp=True,
    )
    require(200 <= status < 300, f"MCP {request_id} returned HTTP {status}: {envelope}")
    require(envelope.get("jsonrpc") == "2.0" and envelope.get("id") == request_id, f"MCP {request_id} returned an invalid envelope: {envelope}")
    require("error" not in envelope, f"MCP {request_id} returned a protocol error: {envelope}")
    result = envelope.get("result")
    require(isinstance(result, dict) and result.get("isError") is not True, f"MCP {request_id} failed: {envelope}")
    content = result.get("content") if isinstance(result, dict) else None
    require(isinstance(content, list) and len(content) == 1 and isinstance(content[0], dict), f"MCP {request_id} returned unexpected content: {envelope}")
    text = content[0].get("text")
    require(isinstance(text, str), f"MCP {request_id} returned no JSON text: {envelope}")
    return decode_json(text.encode("utf-8"), f"MCP {request_id}")


def member_tuples(response: dict) -> tuple[tuple[str, str, str, str, str], ...]:
    selection = field(body(response), "selection")
    require(isinstance(selection, dict), f"catalog response has no selection: {response}")
    require(value(field(selection, "resolvedProfile", "resolved_profile")) == "catalog", f"response did not resolve catalog: {response}")
    require(value(field(selection, "catalogId", "catalog_id")) == "documentation", f"response did not resolve documentation: {response}")
    for name in ("datasetId", "dataset_id", "sourceId", "source_id", "generation", "evidence"):
        require(name not in selection, f"catalog selection must not carry direct-only {name}: {response}")
    members = field(selection, "members")
    require(isinstance(members, list), f"catalog response has no members: {response}")
    result = tuple(sorted((
        (
            str(value(field(member, "sourceId", "source_id"))),
            str(value(field(member, "datasetId", "dataset_id"))),
            str(value(field(member, "generation"))),
            str(value(field(field(member, "evidence"), "uri"))),
            str(value(field(field(member, "evidence"), "sourceId", "source_id"))),
        )
        for member in members
        if isinstance(member, dict)
    )))
    expected = tuple((
        source["source_id"],
        source["dataset_id"],
        source["generation"],
        source["evidence_uri"],
        source["evidence_source_id"],
    ) for source in MEMBERS)
    require(result == expected, f"catalog member attribution mismatch: expected={expected} actual={result} response={response}")
    return result


def assert_term(response: dict, source: dict, label: str) -> None:
    response_body = body(response)
    require(value(field(response_body, "status")) == "matched", f"{label} did not match: {response}")
    result = field(response_body, "result")
    term = field(result, "term") if isinstance(result, dict) else None
    require(isinstance(term, dict), f"{label} has no term result: {response}")
    evidence = field(term, "evidence")
    require(value(field(term, "termId", "term_id")) == source["term_id"], f"{label} term identity mismatch: {response}")
    require(value(field(term, "datasetId", "dataset_id")) == source["dataset_id"], f"{label} dataset attribution mismatch: {response}")
    require(value(field(evidence, "sourceId", "source_id")) == source["source_id"], f"{label} source attribution mismatch: {response}")
    members = member_tuples(response)
    require(len(members) == 2, f"{label} did not retain two catalog members: {response}")


def assert_map(response: dict, label: str) -> None:
    response_body = body(response)
    require(value(field(response_body, "status")) == "matched", f"{label} did not match: {response}")
    members = member_tuples(response)
    sources = field(response_body, "sources")
    require(isinstance(sources, list), f"{label} has no Knowledge Map sources: {response}")
    actual = tuple(sorted((
        (
            str(value(field(source, "sourceId", "source_id"))),
            str(value(field(source, "datasetId", "dataset_id"))),
            str(value(field(source, "generation"))),
        )
        for source in sources
        if isinstance(source, dict)
    )))
    expected = tuple(member[:3] for member in members)
    require(actual == expected, f"{label} did not return both selected sources together: expected={expected} actual={actual} response={response}")


def list_tools(base_url: str, timeout: float) -> tuple[tuple[str, ...], tuple[str, ...]]:
    status, listed = post_json(
        base_url,
        "/mcp",
        {"jsonrpc": "2.0", "id": "two-source-tools", "method": "tools/list", "params": {}},
        timeout,
        mcp=True,
    )
    require(200 <= status < 300, f"MCP tools/list returned HTTP {status}: {listed}")
    tools = listed.get("result", {}).get("tools", [])
    require(isinstance(tools, list), f"MCP tools/list returned invalid tools: {listed}")
    require(all(isinstance(tool, dict) and isinstance(tool.get("name"), str) for tool in tools), f"MCP tools/list returned unnamed tool: {listed}")
    names = tuple(tool["name"] for tool in tools)
    require(len(names) == len(set(names)), f"MCP tools/list returned duplicate names: {names}")
    bok_tools = tuple(name for name in names if name.startswith(BOK_TOOL_PREFIX))
    require(
        set(bok_tools) == READ_TOOLS,
        f"MCP tools/list published an unexpected BoK projection: expected={sorted(READ_TOOLS)} actual={sorted(bok_tools)} names={sorted(names)}",
    )
    require(
        not any(name.startswith(SIE_TOOL_PREFIX) for name in names),
        f"MCP tools/list published an SIE-qualified tool: {sorted(names)}",
    )
    require(
        not any(name.startswith(SCRAPER_TOOL_PREFIX) for name in names),
        f"MCP tools/list published a Scraper-qualified tool: {sorted(names)}",
    )
    require(
        not any(name == "replaceKnowledgeSource" or name.endswith(".replaceKnowledgeSource") for name in names),
        f"MCP tools/list published replaceKnowledgeSource: {sorted(names)}",
    )
    return names, bok_tools


def replace_knowledgehub(base_url: str, timeout: float) -> None:
    status, replacement = post_json(
        base_url,
        REPLACE_PATH,
        {
            "source": {
                "sourceId": KNOWLEDGEHUB["source_id"],
                "datasetId": KNOWLEDGEHUB["dataset_id"],
                "generation": KNOWLEDGEHUB["generation"],
                "resource": "urn:textus:bok:request-side-resource-must-not-be-read",
            }
        },
        timeout,
    )
    replacement_body = body(replacement)
    require(200 <= status < 300, f"protected KnowledgeHub replacement returned HTTP {status}: {replacement}")
    require(value(field(replacement_body, "status")) == "complete", f"protected KnowledgeHub replacement was not complete: {replacement}")
    require(value(field(replacement_body, "sourceId", "source_id")) == KNOWLEDGEHUB["source_id"], f"protected replacement source mismatch: {replacement}")
    require(value(field(replacement_body, "datasetId", "dataset_id")) == KNOWLEDGEHUB["dataset_id"], f"protected replacement dataset mismatch: {replacement}")
    require(value(field(replacement_body, "generation")) == KNOWLEDGEHUB["generation"], f"protected replacement generation mismatch: {replacement}")


def run(base_url: str, timeout: float) -> None:
    parsed = urllib.parse.urlparse(base_url)
    require(parsed.scheme == "http" and parsed.hostname in {"127.0.0.1", "localhost", "::1"}, "--base-url must be an HTTP loopback URL")
    require(parsed.username is None and parsed.password is None and parsed.path in {"", "/"}, "--base-url must not contain credentials or a path")
    normalized = base_url.rstrip("/")
    endpoint_tools, bok_tools = list_tools(normalized, timeout)
    story = mcp_call(normalized, "two-source-story", EXPLAIN_TERM, {"term": SIMPLEMODELING["term_id"], "profile": "catalog", "catalogId": "documentation"}, timeout)
    embedding = mcp_call(normalized, "two-source-embedding", EXPLAIN_TERM, {"term": KNOWLEDGEHUB["term_id"], "profile": "catalog", "catalogId": "documentation"}, timeout)
    assert_term(story, SIMPLEMODELING, "SimpleModeling Story")
    assert_term(embedding, KNOWLEDGEHUB, "KnowledgeHub Embedding")
    initial_map = mcp_call(normalized, "two-source-map", KNOWLEDGE_MAP, {"profile": "catalog", "catalogId": "documentation", "nodeLimit": 256, "relationshipLimit": 512}, timeout)
    assert_map(initial_map, "initial catalog Knowledge Map")
    replace_knowledgehub(normalized, timeout)
    remaining_story = mcp_call(normalized, "two-source-story-after-replacement", EXPLAIN_TERM, {"term": SIMPLEMODELING["term_id"], "profile": "catalog", "catalogId": "documentation"}, timeout)
    assert_term(remaining_story, SIMPLEMODELING, "SimpleModeling Story after KnowledgeHub replacement")
    replacement_map = mcp_call(normalized, "two-source-map-after-replacement", KNOWLEDGE_MAP, {"profile": "catalog", "catalogId": "documentation", "nodeLimit": 256, "relationshipLimit": 512}, timeout)
    assert_map(replacement_map, "catalog Knowledge Map after KnowledgeHub replacement")
    print(f"BOK_TWO_SOURCE_PROBE_OK bok_tools={len(bok_tools)} endpoint_tools={len(endpoint_tools)} members=2 mcp_explanations=2 maps=2 protected_replacements=1")


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify loopback-only two-source BoK catalog MCP acceptance.")
    parser.add_argument("--base-url", default="http://127.0.0.1:19548")
    parser.add_argument("--timeout", default=30.0, type=float)
    arguments = parser.parse_args()
    try:
        run(arguments.base_url, arguments.timeout)
        return 0
    except (AssertionError, KeyError, OSError, RuntimeError, TypeError, ValueError) as error:
        print(f"BOK_TWO_SOURCE_PROBE_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
