#!/usr/bin/env python3

import argparse
import json
import urllib.request


BOK_TOOLS = {
    "Bok.BokRetrieval.searchTerms",
    "Bok.BokRetrieval.explainTerm",
    "Bok.BokRetrieval.searchComponentReferences",
    "Bok.BokRetrieval.getComponentReference",
}

LEGACY_SIE_BOK_TOOLS = {
    "SemanticIntegrationEngine.SemanticRetrieval.searchTerms",
    "SemanticIntegrationEngine.SemanticRetrieval.explainTerm",
    "SemanticIntegrationEngine.SemanticRetrieval.searchComponentReferences",
    "SemanticIntegrationEngine.SemanticRetrieval.getComponentReference",
}


def _post_json(base_url: str, path: str, body: dict, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.load(response)


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _call_tool(base_url: str, name: str, arguments: dict, timeout: float) -> dict:
    envelope = _post_json(
        base_url,
        "/mcp",
        {
            "jsonrpc": "2.0",
            "id": name,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        },
        timeout,
    )
    _require("error" not in envelope, f"MCP call failed: {envelope}")
    content = envelope.get("result", {}).get("content", [])
    _require(len(content) == 1, f"Unexpected MCP content: {envelope}")
    return json.loads(content[0]["text"])


def _run(
    base_url: str,
    source_uri: str,
    expected_rdf_provider: str,
    expected_vector_provider: str,
    probe_query: str,
    probe_category: str,
    source_id: str,
    dataset_id: str,
    generation: str,
    timeout: float,
) -> None:
    listed = _post_json(
        base_url,
        "/mcp",
        {"jsonrpc": "2.0", "id": "list", "method": "tools/list", "params": {}},
        timeout,
    )
    tools = listed.get("result", {}).get("tools", [])
    names = {tool.get("name") for tool in tools}
    _require(BOK_TOOLS <= names, f"BoK tools are missing: {sorted(BOK_TOOLS - names)}")
    _require(
        not (LEGACY_SIE_BOK_TOOLS & names),
        f"Legacy SIE BoK tools remain visible: {sorted(LEGACY_SIE_BOK_TOOLS & names)}",
    )

    replacement = _post_json(
        base_url,
        "/rest/v1/bok/bok-retrieval/replace-knowledge-source",
        {
            "source": {
                "sourceId": source_id,
                "datasetId": dataset_id,
                "generation": generation,
                "resource": source_uri,
            }
        },
        timeout,
    )
    _require(replacement.get("status") == "complete", f"BoK replacement failed: {replacement}")
    _require(replacement.get("term_count", 0) > 0, f"BoK replacement normalized no terms: {replacement}")

    provider_status = _call_tool(
        base_url,
        "SemanticIntegrationEngine.SemanticRetrieval.status",
        {},
        timeout,
    )
    _require(
        provider_status.get("graph") == expected_rdf_provider,
        f"Unexpected RDF provider: {provider_status}",
    )
    _require(
        provider_status.get("vector") == expected_vector_provider,
        f"Unexpected Vector provider: {provider_status}",
    )
    _require(
        provider_status.get("overall") == "healthy",
        f"Provider status is not healthy: {provider_status}",
    )

    search_arguments = {"query": probe_query, "limit": 10}
    if probe_category:
        search_arguments["category"] = probe_category
    search = _call_tool(
        base_url,
        "Bok.BokRetrieval.searchTerms",
        search_arguments,
        timeout,
    )
    _require(search.get("status") == "matched", f"BoK terminology search failed: {search}")
    _require(search.get("results"), f"BoK terminology search returned no result: {search}")
    print(
        f"BOK_CODEX_SAR_OK endpoint={base_url}/mcp "
        f"bok_tools={len(BOK_TOOLS)} legacy_sie_bok_tools=0 "
        f"rdf={expected_rdf_provider} vector={expected_vector_provider}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Bootstrap and verify the BoK Codex SAR.")
    parser.add_argument("--base-url", default="http://127.0.0.1:18005")
    parser.add_argument("--source-uri", required=True)
    parser.add_argument("--expected-rdf-provider", default="in-memory-rdf")
    parser.add_argument("--expected-vector-provider", default="in-memory-vector")
    parser.add_argument("--probe-query", default="Component")
    parser.add_argument("--probe-category", default="architecture")
    parser.add_argument("--source-id", default="codex-bok")
    parser.add_argument("--dataset-id", default="codex-bok")
    parser.add_argument("--generation", default="2026-07-21T00:00:00Z")
    parser.add_argument("--timeout", type=float, default=10.0)
    arguments = parser.parse_args()
    try:
        _run(
            arguments.base_url.rstrip("/"),
            arguments.source_uri,
            arguments.expected_rdf_provider,
            arguments.expected_vector_provider,
            arguments.probe_query,
            arguments.probe_category,
            arguments.source_id,
            arguments.dataset_id,
            arguments.generation,
            arguments.timeout,
        )
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, KeyError) as error:
        print(f"BOK_CODEX_SAR_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
