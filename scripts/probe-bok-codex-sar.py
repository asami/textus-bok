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


def _run(base_url: str, source_uri: str, timeout: float) -> None:
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
                "sourceId": "codex-bok",
                "datasetId": "codex-bok",
                "generation": "2026-07-21T00:00:00Z",
                "resource": source_uri,
            }
        },
        timeout,
    )
    _require(replacement.get("status") == "complete", f"BoK replacement failed: {replacement}")

    search = _call_tool(
        base_url,
        "Bok.BokRetrieval.searchTerms",
        {"query": "Component", "category": "architecture", "limit": 10},
        timeout,
    )
    _require(search.get("status") == "matched", f"BoK terminology search failed: {search}")
    _require(search.get("results"), f"BoK terminology search returned no result: {search}")
    print(
        f"BOK_CODEX_SAR_OK endpoint={base_url}/mcp "
        f"bok_tools={len(BOK_TOOLS)} legacy_sie_bok_tools=0"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Bootstrap and verify the BoK Codex SAR.")
    parser.add_argument("--base-url", default="http://127.0.0.1:18005")
    parser.add_argument("--source-uri", required=True)
    parser.add_argument("--timeout", type=float, default=10.0)
    arguments = parser.parse_args()
    try:
        _run(arguments.base_url.rstrip("/"), arguments.source_uri, arguments.timeout)
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, KeyError) as error:
        print(f"BOK_CODEX_SAR_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
