# BoK Knowledge Map Design

Status: accepted Phase 6 design

## Purpose

Textus BoK provides a read-only, bounded Knowledge Map over the last completely
selected Cozy BoK generation. The map is a Textus BoK projection, not a Cozy
dashboard, RDF browser, SIE result browser, or CBD catalog detail page.

## Ownership

| Owner | Responsibility |
| --- | --- |
| Cozy | Generates the versioned BoK graph-summary resource and BoK metadata. |
| Textus BoK | Admits safe structured resources, normalizes factual topology, owns the selected-generation catalog, query, and Web projection. |
| SIE | Owns generic federation and semantic candidates; candidates never become map relationships. |
| Textus CBD Support | Owns component capability, dependency, compatibility, operation, and usage details. |
| CNCF | Owns resource access, operation dispatch and authorization, and Static Form routing and assets. |

## Producer and Consumer Boundary

Cozy publishes the optional `rdf-graph-summary` manifest resource at
`metadata/rdf/graph.json`. For Phase 6, Textus BoK admits that resource only
when its top-level `schemaVersion` is `cozy.rdf-graph-summary.v1` and its
`kind` is `rdf-graph-summary`.

The published document retains `nodes`, `edges`, and `truncated`, and adds the
attributable `sourceRef` copied from the site identity. Node and relationship
metadata remains source data. Textus BoK does not fetch a node identifier,
rendered page, RDF document, or provider result in order to enrich it.

Articles, scenarios, projects, bibliography entries, tags, and RDF resources
are reference nodes. They retain their declared identifier, label, category,
term references, tags, and source evidence only; their full domain models stay
with their source owners. CAR/SAR nodes remain existence-only and hand readers
to CBD Support for detail.

## Knowledge Semantics

A map relationship is factual only when an admitted source document explicitly
declares an edge or retained reference. Its evidence identifies the selected
source generation and the source resource. Semantic candidates, score-ranked
results, inferred relationships, and co-occurrence are not map edges. Unknown
or malformed topology is rejected rather than inferred.

## Selected Generation Invariant

`NormalizedBokSource` contains terms, component references, map nodes, and map
relationships as one immutable source result. `BokKnowledgeCatalog` replaces
all four only after complete SIE publication. A degraded publication retains
the complete preceding generation, including its topology. Every query and Web
result is derived from exactly one such catalog generation.

## Safety and Presentation

Normalization uses only logical child references resolved through CNCF resource
access. It rejects unsafe references, unsupported schema versions, malformed
or oversized documents, dangling edges, and conflicting duplicate identities.
The Web application is read-only, uses one bounded query result for both the
progressive graph and no-JavaScript fallback, and opens only admitted HTTP(S)
evidence links through explicit reader actions.
