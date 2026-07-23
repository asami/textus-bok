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
| `nodes` | Array of source nodes. Each node has non-empty `id`, `label`, and `node_type`; `category`, `terms`, and `tags` are optional source metadata. A `component-reference` node may carry `componentRef` with `kind` (`car` or `sar`) and `name`. |
| `edges` | Array of source edges. Each edge has non-empty `source`, `predicate`, and `target`; `label`, `category`, `terms`, and `tags` are optional source metadata. |
| `truncated` | Boolean. `true` is preserved as a typed map warning. |

Unknown fields are retained only when required for attributable evidence; they
do not create relationship kinds. Existing unversioned graph summaries are not
admitted topology input.

`componentRef` is a direct existence-only handoff, not a component lookup hint.
Textus BoK accepts it only when its exact `(kind, name)` matches exactly one
CAR/SAR entry in the same selected source generation's component index. It
does not derive a component identity from a node ID, label, tag, title, or
candidate score. An unmatched or malformed handoff rejects the source
generation; matched identities are projected as `ComponentReference` values
without CBD capability, dependency, compatibility, operation, or usage detail.

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

`BokRetrieval.getKnowledgeMap` is a public, read-only operation. Its generated
request accepts optional `datasetId`, `sourceId`, `category`, `termType`, and
`focus` values plus optional `nodeLimit` and `relationshipLimit` values. An
omitted source selector means all selected complete generations. A category or
term-type match supplies the neighborhood seed; the returned result includes
the factual relationships directly incident to a seed and their endpoints. A
focus matches a source node identifier, retained term identifier, or normalized
node label after the other selectors have been applied.

The result has selected source/dataset/generation summaries, including the
admitted source-reference kind, value, and optional URI. Each node retains its
source term identifiers and the selected complete `BokTerm` records that match
those identifiers, plus only its validated `ComponentReference` handoffs, so
one result can provide attributed definitions and portable CBD handoff identity
without a second retrieval. Nodes and relationships are deterministic, with effective
limits, a result `truncated` flag, per-source truncation, and typed warnings.
For selector-scoped queries, matching seed nodes come first, followed by their
sorted adjacent endpoints; unscoped nodes and all relationships are sorted by
dataset identity and stable source identity.
Requested limits are clamped to
the Phase 6 defaults of 128 nodes and 256 relationships, with an explicit
warning when clamping or result omission occurs. A focus or selector that
matches no selected node returns `no-match` with empty node and relationship
collections; it does not synthesize a node or relationship.

The operation reads only the selected `BokKnowledgeCatalog` generation. It
does not invoke SIE candidate retrieval, and no candidate score, inferred
association, or shared tag becomes a factual edge. It is not MCP-ready in
Phase 6 unless a separate decision and projection specification authorize it.

The Static Form Web surface uses that one result for filters, graph, detail,
warnings, and the no-JavaScript list/table fallback. It presents component
nodes as existence-only with a CBD Support handoff and never invokes CBD
Support from the page.
