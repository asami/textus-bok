# BoK Knowledge Map Contract

Status: Phase 6 authoritative specification

## Cozy Graph-Summary Resource

An admitted topology resource is declared by the existing
`cncf.knowledge-source.v1` manifest as:

```json
{
  "kind": "rdf-graph-summary",
  "href": "metadata/rdf/graph.json",
  "mediaType": "application/json"
}
```

The resource itself MUST be a JSON object with:

| Field | Requirement |
| --- | --- |
| `schemaVersion` | Exactly `cozy.rdf-graph-summary.v1`. |
| `kind` | Exactly `rdf-graph-summary`. |
| `sourceRef` | Object with `kind` = `bok-site`, non-empty `value`, and optional attributable `uri`. |
| `nodes` | Array of source nodes. Each node has non-empty `id`, `label`, and `node_type`; `category`, `terms`, and `tags` are optional source metadata. |
| `edges` | Array of source edges. Each edge has non-empty `source`, `predicate`, and `target`; `label`, `category`, `terms`, and `tags` are optional source metadata. |
| `truncated` | Boolean. `true` is preserved as a typed map warning. |

Unknown fields are retained only when required for attributable evidence; they
do not create relationship kinds. Existing unversioned graph summaries are not
admitted topology input.

## Finite Limits

The 2026-07-23 representative Cozy BoK contained 131 nodes and 234 edges. The
initial contract fixes these importer limits:

| Item | Default query bound | Maximum admitted source bound |
| --- | ---: | ---: |
| nodes | 128 | 512 |
| relationships | 256 | 2,048 |
| identifier length | 256 | 1,024 |
| label length | 256 | 512 |
| term references per node or relationship | 16 | 32 |
| tags per node or relationship | 16 | 32 |

The source reader rejects a document that exceeds a maximum bound. A query
that needs fewer values reports its effective limits and truncation explicitly;
it never invents omitted relationships.

## Determinism and Atomicity

Node identity is `(datasetId, sourceNodeId)`. Relationship identity is the
dataset identity plus `(source, predicate, target)` and the deterministic
source edge identity where supplied. Nodes and relationships are sorted by
their stable identities. Exact duplicates may merge evidence deterministically;
conflicting duplicates and dangling endpoints fail normalization.

Terms, component references, nodes, and relationships commit to the catalog as
one selected generation. Failed or degraded publication retains the complete
prior generation and its warnings.

## Query and Web Consequences

The later `getKnowledgeMap` operation accepts bounded dataset, source,
category, term type, and focus selection. It returns explicit no-match and
truncation states. It is public read-only but not MCP-ready in Phase 6 unless a
separate decision and projection specification authorize it.

The Static Form Web surface uses that one result for filters, graph, detail,
warnings, and the no-JavaScript list/table fallback. It presents component
nodes as existence-only with a CBD Support handoff.
