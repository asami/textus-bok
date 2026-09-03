#!/usr/bin/env python3

import argparse
import html
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def _get(base_url: str, path: str, parameters: dict[str, str], timeout: float) -> tuple[dict, str]:
    query = urllib.parse.urlencode(parameters)
    url = f"{base_url}{path}?{query}" if query else f"{base_url}{path}"
    with urllib.request.urlopen(url, timeout=timeout) as response:
        content = response.read().decode(response.headers.get_content_charset() or "utf-8")
        return json.loads(content), url


def _get_text(base_url: str, path: str, parameters: dict[str, str], timeout: float) -> str:
    query = urllib.parse.urlencode(parameters)
    url = f"{base_url}{path}?{query}" if query else f"{base_url}{path}"
    with urllib.request.urlopen(url, timeout=timeout) as response:
        return response.read().decode(response.headers.get_content_charset() or "utf-8")


def _post_json(base_url: str, path: str, body: dict, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode(response.headers.get_content_charset() or "utf-8"))


def _body(value: dict) -> dict:
    body = value.get("body")
    return body if isinstance(body, dict) else value


def _field(value: dict, *names: str) -> object:
    for name in names:
        if name in value:
            return value[name]
    return None


def _script_model(page: str) -> dict:
    match = re.search(
        r'<script[^>]*id="bok-knowledge-map-data"[^>]*>(.*?)</script>',
        page,
        flags=re.DOTALL | re.IGNORECASE,
    )
    _require(match is not None, "Knowledge Map page has no operation-result data script")
    return _body(json.loads(html.unescape(match.group(1))))


def _node_id(node: dict) -> str:
    value = _field(node, "nodeId", "node_id", "id")
    return value.get("value") if isinstance(value, dict) else str(value)


def _relationship_id(relationship: dict) -> tuple[str, str, str]:
    return tuple(
        str(_value(_field(relationship, *fields)))
        for fields in (("subjectId", "subject_id"), ("predicate",), ("objectId", "object_id"))
    )


def _value(value: object) -> object:
    return value.get("value") if isinstance(value, dict) else value


def _run(base_url: str, source_root: Path, dataset_id: str, source_id: str, generation: str, timeout: float) -> None:
    graph = json.loads((source_root / "metadata/rdf/graph.json").read_text())
    _require(graph.get("schemaVersion") == "cozy.rdf-graph-summary.v1", "KnowledgeHub graph is not versioned Cozy topology")
    _require(graph.get("kind") == "rdf-graph-summary", "KnowledgeHub graph is not an RDF graph summary")
    _require(isinstance(graph.get("sourceRef"), dict), "KnowledgeHub graph has no sourceRef")
    replacement = _post_json(
        base_url,
        "/rest/v1/bok/bok-retrieval/replace-knowledge-source",
        {
            "source": {
                "sourceId": source_id,
                "datasetId": dataset_id,
                "generation": generation,
                "resource": source_root.as_uri() + "/",
            }
        },
        timeout,
    )
    _require(replacement.get("status") == "complete", f"Knowledge Map source replacement failed: {replacement}")

    request = {
        "datasetId": dataset_id,
        "sourceId": source_id,
        "nodeLimit": 128,
        "relationshipLimit": 256,
    }
    operation_url = f"{base_url}/rest/v1/bok/bok-retrieval/get-knowledge-map"
    operation = _post_json(base_url, "/rest/v1/bok/bok-retrieval/get-knowledge-map", request, timeout)
    model = _body(operation)
    _require(model.get("status") == "matched", f"Knowledge Map operation did not match: {operation}")
    _require(len(model.get("sources", [])) == 1, f"Knowledge Map selected an unexpected source set: {operation}")
    source = model["sources"][0]
    _require(_value(_field(source, "datasetId", "dataset_id")) == dataset_id, f"Dataset mismatch: {source}")
    _require(_value(_field(source, "sourceId", "source_id")) == source_id, f"Source mismatch: {source}")
    _require(_value(_field(source, "generation")) == generation, f"Generation mismatch: {source}")
    _require(_value(_field(source, "sourceTruncated", "source_truncated")) == graph.get("truncated"), f"Source truncation mismatch: {source}")
    source_reference = _field(source, "sourceReference", "source_reference")
    _require(isinstance(source_reference, dict), f"Knowledge Map source has no source reference: {source}")
    for field in ("kind", "value", "uri"):
        _require(
            _value(source_reference.get(field)) == graph["sourceRef"].get(field),
            f"Source reference {field} mismatch: {source_reference}",
        )

    expected_nodes = {_node_id(node) for node in graph.get("nodes", [])}
    expected_relationships = {
        (str(edge["source"]), str(edge["predicate"]), str(edge["target"]))
        for edge in graph.get("edges", [])
    }
    _require({_node_id(node) for node in model.get("nodes", [])} == expected_nodes, f"Node mismatch: {operation_url}")
    _require({_relationship_id(edge) for edge in model.get("relationships", [])} == expected_relationships, f"Relationship mismatch: {operation_url}")
    _require(_value(model.get("truncated")) == graph.get("truncated"), f"Result truncation mismatch: {operation}")

    graph_components = {
        str(node["id"]): node["componentRef"]
        for node in graph.get("nodes", [])
        if isinstance(node.get("componentRef"), dict)
    }
    response_components = {
        _node_id(node): [
            {key: _value(reference.get(key)) for key in ("kind", "name", "organization", "version") if reference.get(key) is not None}
            for reference in _field(node, "componentReferences", "component_references") or []
        ]
        for node in model.get("nodes", [])
    }
    for node_id, reference in graph_components.items():
        expected = {key: value for key, value in reference.items() if key in {"kind", "name", "organization", "version"}}
        _require(
            any(all(actual.get(key) == value for key, value in expected.items()) for actual in response_components.get(node_id, [])),
            f"CBD handoff mismatch for {node_id}: {response_components.get(node_id)}",
        )

    _require(all(node.get("evidence") for node in model.get("nodes", [])), "Knowledge Map nodes lack source evidence")
    _require(all(edge.get("evidence") for edge in model.get("relationships", [])), "Knowledge Map relationships lack source evidence")

    page = _get_text(base_url, "/web/bok/textus-bok/map", {key: str(value) for key, value in request.items()}, timeout)
    web = _script_model(page)
    _require(_value(web.get("status")) == _value(model.get("status")), "Web status differs from operation")
    _require({_node_id(node) for node in web.get("nodes", [])} == {_node_id(node) for node in model.get("nodes", [])}, "Web nodes differ from operation")
    _require({_relationship_id(edge) for edge in web.get("relationships", [])} == {_relationship_id(edge) for edge in model.get("relationships", [])}, "Web relationships differ from operation")
    _require(_value(web.get("truncated")) == _value(model.get("truncated")), "Web truncation differs from operation")
    _require(web.get("sources") == model.get("sources"), "Web generation/source evidence differs from operation")
    _require(
        {_node_id(node): node.get("evidence") for node in web.get("nodes", [])} ==
          {_node_id(node): node.get("evidence") for node in model.get("nodes", [])},
        "Web node evidence differs from operation",
    )
    _require(
        {_relationship_id(edge): edge.get("evidence") for edge in web.get("relationships", [])} ==
          {_relationship_id(edge): edge.get("evidence") for edge in model.get("relationships", [])},
        "Web relationship evidence differs from operation",
    )
    _require(
        {node_id: response_components.get(node_id, []) for node_id in graph_components} == {
            _node_id(node): [
                {key: _value(reference.get(key)) for key in ("kind", "name", "organization", "version") if reference.get(key) is not None}
                for reference in _field(node, "componentReferences", "component_references") or []
            ]
            for node in web.get("nodes", []) if _node_id(node) in graph_components
        },
        "Web CBD handoff differs from operation",
    )
    print(
        f"BOK_KNOWLEDGE_MAP_SAR_OK replacement=complete operation={operation_url} "
        f"generation={generation} nodes={len(expected_nodes)} relationships={len(expected_relationships)} "
        f"component_handoffs={len(graph_components)}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify the live Textus BoK Knowledge Map against a real Cozy source.")
    parser.add_argument("--base-url", default="http://127.0.0.1:18007")
    parser.add_argument("--source-root", required=True)
    parser.add_argument("--dataset-id", required=True)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--generation", required=True)
    parser.add_argument("--timeout", type=float, default=30.0)
    arguments = parser.parse_args()
    try:
        _run(
            arguments.base_url.rstrip("/"),
            Path(arguments.source_root).resolve(),
            arguments.dataset_id,
            arguments.source_id,
            arguments.generation,
            arguments.timeout,
        )
        return 0
    except (AssertionError, json.JSONDecodeError, OSError, KeyError, TypeError) as error:
        print(f"BOK_KNOWLEDGE_MAP_SAR_FAILED: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
