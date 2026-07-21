#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.parse
import urllib.request


OPERATION_PAIRS = (
    (
        "SemanticIntegrationEngine.SemanticRetrieval.searchTerms",
        "Bok.BokRetrieval.searchTerms",
        {"query": "Runtime", "category": "architecture", "limit": 10},
        ("status", "results"),
    ),
    (
        "SemanticIntegrationEngine.SemanticRetrieval.explainTerm",
        "Bok.BokRetrieval.explainTerm",
        {"term": "Runtime"},
        ("status", "results"),
    ),
    (
        "SemanticIntegrationEngine.SemanticRetrieval.searchComponentReferences",
        "Bok.BokRetrieval.searchComponentReferences",
        {"query": "textus-runtime", "kind": "car", "limit": 10},
        ("status", "results"),
    ),
    (
        "SemanticIntegrationEngine.SemanticRetrieval.getComponentReference",
        "Bok.BokRetrieval.getComponentReference",
        {"name": "textus-runtime", "version": "1.0.0", "kind": "car"},
        ("status", "results"),
    ),
)


def _request(request: urllib.request.Request, timeout: float) -> dict:
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            if response.headers.get_content_type() != "application/json":
                raise RuntimeError(
                    f"Unexpected content type from {request.full_url}: "
                    f"{response.headers.get_content_type()}"
                )
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"HTTP {error.code} from {request.full_url}: {detail}"
        ) from error


def _post_json(base_url: str, path: str, body: dict, timeout: float) -> dict:
    return _request(
        urllib.request.Request(
            f"{base_url}{path}",
            data=json.dumps(body).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        ),
        timeout,
    )


def _post_form(base_url: str, path: str, body: dict, timeout: float) -> dict:
    return _request(
        urllib.request.Request(
            f"{base_url}{path}",
            data=urllib.parse.urlencode(body).encode("utf-8"),
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        ),
        timeout,
    )


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _call_tool(
    base_url: str,
    request_id: str,
    name: str,
    arguments: dict,
    timeout: float,
) -> dict:
    envelope = _post_json(
        base_url,
        "/mcp",
        {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        },
        timeout,
    )
    _require("error" not in envelope, f"MCP {request_id} failed: {envelope}")
    content = envelope.get("result", {}).get("content", [])
    _require(len(content) == 1, f"MCP {request_id} returned unexpected content: {envelope}")
    text = content[0].get("text")
    _require(isinstance(text, str), f"MCP {request_id} returned no text: {envelope}")
    return json.loads(text)


def _semantic_projection(response: dict, fields: tuple[str, ...]) -> dict:
    projected = {field: response.get(field) for field in fields}
    results = projected.get("results")
    if not isinstance(results, list):
        if isinstance(response.get("result"), dict):
            results = [response["result"]]
        elif isinstance(response.get("reference"), dict):
            results = [{**response["reference"], "match_kind": "exact"}]
    if isinstance(results, list):
        normalized = []
        for item in results:
            nested = item.get("term") or item.get("reference")
            source = {**nested, **item} if isinstance(nested, dict) else item
            normalized.append(
                {
                    key: source.get(key)
                    for key in (
                        "title",
                        "definition",
                        "category",
                        "name",
                        "version",
                        "kind",
                        "match_kind",
                    )
                    if key in source
                }
            )
        projected["results"] = normalized
    return projected


def _run(base_url: str, source_uri: str, timeout: float) -> None:
    listed = _post_json(
        base_url,
        "/mcp",
        {"jsonrpc": "2.0", "id": "migration-list", "method": "tools/list", "params": {}},
        timeout,
    )
    _require("error" not in listed, f"MCP tools/list failed: {listed}")
    tools = listed.get("result", {}).get("tools", [])
    names = [tool.get("name") for tool in tools]
    _require(names == sorted(names), f"Tool identities are not deterministic: {names}")
    _require(len(names) == len(set(names)), f"Tool identities collide: {names}")
    tools_by_name = {tool["name"]: tool for tool in tools}

    expected = {name for pair in OPERATION_PAIRS for name in pair[:2]}
    _require(expected <= set(names), f"Migration tools are missing: {sorted(expected - set(names))}")
    for old_name, new_name, _, _ in OPERATION_PAIRS:
        _require(
            tools_by_name[old_name].get("inputSchema")
            == tools_by_name[new_name].get("inputSchema"),
            f"Input schema parity failed for {old_name} and {new_name}",
        )

    old_ingestion = _post_form(
        base_url,
        "/rest/v1/semantic-integration-engine/knowledge-store-admin/ingest-bok-knowledge-source",
        {
            "baseUri": source_uri,
            "registerKnowledgeSpace": "true",
            "includeKnowledgeFrame": "false",
        },
        timeout,
    )
    _require(old_ingestion.get("term_count") == 1, f"Old SIE ingestion failed: {old_ingestion}")

    new_replacement = _post_json(
        base_url,
        "/rest/v1/bok/bok-retrieval/replace-knowledge-source",
        {
            "source": {
                "sourceId": "migration-bok",
                "datasetId": "migration-bok",
                "generation": "2026-07-21T00:00:00Z",
                "resource": source_uri,
            }
        },
        timeout,
    )
    _require(new_replacement.get("status") == "complete", f"BoK replacement failed: {new_replacement}")
    _require(new_replacement.get("term_count") == 1, f"Unexpected BoK term count: {new_replacement}")
    _require(new_replacement.get("component_count") == 1, f"Unexpected BoK component count: {new_replacement}")

    for index, (old_name, new_name, arguments, fields) in enumerate(OPERATION_PAIRS):
        old_response = _call_tool(base_url, f"old-{index}", old_name, arguments, timeout)
        new_response = _call_tool(base_url, f"new-{index}", new_name, arguments, timeout)
        old_projection = _semantic_projection(old_response, fields)
        new_projection = _semantic_projection(new_response, fields)
        _require(
            old_projection == new_projection,
            f"Operation parity failed for {old_name} and {new_name}: "
            f"old={old_projection} new={new_projection}",
        )

    print(f"endpoint={base_url}/mcp old_tools=4 new_tools=4")
    print("tool_identity_order=stable collisions=none input_schema_parity=complete")
    print("semantic_result_parity=complete metadata_mode=default")
    print("BOK_MIGRATION_SAR_OK")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Verify old SIE and new Textus BoK operation parity in the migration SAR."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:19545")
    parser.add_argument("--source-uri", required=True)
    parser.add_argument("--timeout", type=float, default=10.0)
    arguments = parser.parse_args()

    try:
        _run(arguments.base_url.rstrip("/"), arguments.source_uri, arguments.timeout)
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, RuntimeError) as error:
        print(f"BOK_MIGRATION_SAR_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
