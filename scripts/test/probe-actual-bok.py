#!/usr/bin/env python3

import argparse
import json
import urllib.error
import urllib.request
from pathlib import Path


EXPECTED_TOOLS = {
    "SemanticIntegrationEngine.SemanticRetrieval.query",
    "SemanticIntegrationEngine.SemanticRetrieval.explain",
    "SemanticIntegrationEngine.SemanticRetrieval.status",
    "Bok.BokRetrieval.searchTerms",
    "Bok.BokRetrieval.explainTerm",
    "Bok.BokRetrieval.searchComponentReferences",
    "Bok.BokRetrieval.getComponentReference",
}
MCP_PROTOCOL_VERSION = "2025-11-25"


def _request_json(request: urllib.request.Request, timeout: float) -> dict:
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"HTTP {error.code} from {request.full_url}: {detail}"
        ) from error


def _post_json(
    baseurl: str,
    path: str,
    data: dict,
    timeout: float,
    *,
    mcp: bool = False,
) -> dict:
    headers = {"Content-Type": "application/json"}
    if mcp:
        headers["MCP-Protocol-Version"] = MCP_PROTOCOL_VERSION
    request = urllib.request.Request(
        f"{baseurl}{path}",
        data=json.dumps(data).encode("utf-8"),
        headers=headers,
    )
    return _request_json(request, timeout)


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _mcp_call(
    baseurl: str, requestid: str, name: str, arguments: dict, timeout: float
) -> dict:
    envelope = _post_json(
        baseurl,
        "/mcp",
        {
            "jsonrpc": "2.0",
            "id": requestid,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        },
        timeout,
        mcp=True,
    )
    _require("error" not in envelope, f"MCP {requestid} failed: {envelope}")
    content = envelope.get("result", {}).get("content", [])
    _require(len(content) == 1, f"Unexpected MCP content: {envelope}")
    return json.loads(content[0]["text"])


def _source_counts(root: Path) -> tuple[int, dict[str, int]]:
    manifestpath = root / "metadata/cncf/knowledge-source.json"
    _require(manifestpath.is_file(), f"KnowledgeSource manifest is missing: {manifestpath}")
    manifest = json.loads(manifestpath.read_text(encoding="utf-8"))
    resources = manifest.get("resources", [])
    termsresources = [x for x in resources if x.get("kind") == "glossary-terms"]
    _require(len(termsresources) == 1, f"Expected one glossary resource: {resources}")
    termspath = root / termsresources[0]["href"]
    termcount = len(json.loads(termspath.read_text(encoding="utf-8")).get("terms", []))
    componentcounts = {"car": 0, "sar": 0}
    for resource in resources:
        if resource.get("kind") != "component-reference-index":
            continue
        indexpath = root / resource["href"]
        index = json.loads(indexpath.read_text(encoding="utf-8"))
        _require(
            index.get("schemaVersion") == "cncf.component-reference-index.v1",
            f"Unexpected component reference schema: {indexpath}",
        )
        kind = index.get("kind")
        _require(kind in componentcounts, f"Unexpected component kind: {kind}")
        componentcounts[kind] += len(index.get("entries", []))
    return termcount, componentcounts


def _run(
    baseurl: str,
    sourceroot: Path,
    sourceuri: str,
    sourceid: str,
    datasetid: str,
    generation: str,
    timeout: float,
) -> None:
    termcount, componentcounts = _source_counts(sourceroot)
    _require(termcount >= 2, f"Actual BoK source has too few terms: {termcount}")
    _require(componentcounts["car"] >= 1, f"Actual BoK source has no CAR: {componentcounts}")
    _require(componentcounts["sar"] >= 1, f"Actual BoK source has no SAR: {componentcounts}")

    replacement = _post_json(
        baseurl,
        "/rest/v1/bok/bok-retrieval/replace-knowledge-source",
        {
            "source": {
                "sourceId": sourceid,
                "datasetId": datasetid,
                "generation": generation,
                "resource": sourceuri,
            }
        },
        timeout,
    )
    _require(replacement.get("status") == "complete", f"Replacement is not complete: {replacement}")
    _require(replacement.get("term_count") == termcount, f"Term count mismatch: {replacement}")
    _require(
        replacement.get("component_count") == componentcounts["car"] + componentcounts["sar"],
        f"Component count mismatch: {replacement}",
    )

    listed = _post_json(
        baseurl,
        "/mcp",
        {"jsonrpc": "2.0", "id": "tools", "method": "tools/list", "params": {}},
        timeout,
        mcp=True,
    )
    toolnames = {x.get("name") for x in listed.get("result", {}).get("tools", [])}
    componenttools = {name for name in toolnames if name and not name.startswith("tool.")}
    platformtools = toolnames - componenttools
    _require(componenttools == EXPECTED_TOOLS, f"Unexpected component MCP tools: {componenttools}")
    _require(all(name and name.startswith("tool.") for name in platformtools),
             f"Unexpected platform MCP tools: {platformtools}")

    terms = _mcp_call(
        baseurl,
        "terms",
        "Bok.BokRetrieval.searchTerms",
        {"query": "technology:rdf", "category": "technology"},
        timeout,
    )
    _require(terms.get("status") == "matched", f"Actual term lookup failed: {terms}")

    for kind in ("car", "sar"):
        components = _mcp_call(
            baseurl,
            kind,
            "Bok.BokRetrieval.searchComponentReferences",
            {"query": "nict-knowledgehub", "kind": kind},
            timeout,
        )
        results = components.get("results", [])
        _require(components.get("status") == "matched", f"Actual {kind.upper()} lookup failed: {components}")
        references = [x.get("reference", {}) for x in results]
        matching = [x for x in references if x.get("name") == "nict-knowledgehub" and x.get("kind") == kind]
        _require(len(matching) == 1, f"Unexpected {kind.upper()} references: {components}")
        reference = matching[0]
        _require(reference.get("evidence", {}).get("uri"),
                 f"{kind.upper()} reference has no evidence: {reference}")
        _require("capabilities" not in reference, f"{kind.upper()} reference leaks CBD detail: {reference}")
        _require("dependencies" not in reference, f"{kind.upper()} reference leaks CBD detail: {reference}")

    print(
        "actual_bok="
        f"terms:{termcount},cars:{componentcounts['car']},sars:{componentcounts['sar']} "
        f"published_components:{replacement.get('component_count')}"
    )
    print("BOK_ACTUAL_KNOWLEDGE_SMOKE_OK")


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify the provider-backed BoK SAR with an actual BoK source.")
    parser.add_argument("--base-url", default="http://127.0.0.1:18005")
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--source-uri", required=True)
    parser.add_argument("--source-id", default="actual-bok-smoke")
    parser.add_argument("--dataset-id", default="actual-bok-smoke")
    parser.add_argument("--generation", required=True)
    parser.add_argument("--timeout", default=20.0, type=float)
    args = parser.parse_args()
    _run(
        args.base_url.rstrip("/"),
        args.source_root.resolve(),
        args.source_uri.rstrip("/") + "/",
        args.source_id,
        args.dataset_id,
        args.generation,
        args.timeout,
    )


if __name__ == "__main__":
    main()
