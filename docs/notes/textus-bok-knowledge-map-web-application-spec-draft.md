# Textus BoK Knowledge Map Web Application Specification Draft

Status: exploratory and non-normative

Date: 2026-07-23

This note proposes an implementation contract for the Textus BoK Knowledge Map
Web application. It must be reviewed and promoted into `docs/design` and
`docs/spec` before its behavior becomes authoritative. It does not by itself
authorize implementation or change the current public component contract.

## Purpose

Provide a component-owned, read-only Web application that projects an admitted
Cozy BoK source as navigable, evidence-bearing knowledge. A reader should be
able to move from a dataset or category to terms, related BoK resources,
component existence references, and source evidence without reading raw JSON
or treating semantic similarity as fact.

## Goals

- Visualize the last completely selected BoK generation held by Textus BoK.
- Make terms the primary entry point while retaining categories, source
  resources, and existence-only CAR/SAR references.
- Show why a node or relationship is present and where its evidence came from.
- Use structured Cozy metadata through the CNCF resource DSL.
- Keep the result deterministic, bounded, accessible, and useful without
  JavaScript.
- Use local Web assets packaged in the Textus BoK CAR.

## Non-Goals

- Editing or publishing a Cozy BoK.
- Replacing the Cozy dashboard or its complete RDF/triples viewer.
- Querying Fuseki, Chroma, a filesystem path, or a remote Web page directly.
- Inferring factual edges from embeddings or provider candidate scores.
- Importing CBD-owned component capability, dependency, compatibility,
  operation, manual, or usage detail.
- Making source replacement or another mutation available through the Web app
  or MCP.

## Current Baseline

The component currently normalizes `glossary-terms`,
`component-repository-index`, and `component-reference-index` resources. The
normalized catalog contains terms and existence-only component references. Its
four public read operations provide term and component lookup, while the
existing Static Form app has no custom authored page under `src/main/web`.

Cozy-generated term metadata contains relationship fields not retained by the
current reader. A Cozy BoK can also produce `metadata/rdf/graph.json` with a
node/edge summary. The precise versioned handoff contract for that graph is an
open design item and must be frozen with Cozy before implementation.

## Proposed Knowledge Model

### Node

A `BokKnowledgeNode` should contain at least:

| Field | Meaning |
|---|---|
| `nodeId` | Stable identity within the selected dataset |
| `kind` | Dataset, category, term, resource reference, component reference, or evidence |
| `label` | Human-readable label from source metadata |
| `datasetId` | Owning selected dataset |
| `sourceId` | Attributable source identity |
| `category` | Optional BoK category |
| `termType` | Optional BoK term classification |
| `resource` | Optional logical resource/URI shown as data, not opened implicitly |
| `evidence` | Zero or more attributable evidence values |

Article, scenario, project, bibliography, tag, and RDF resources should enter
the initial model as typed resource-reference nodes. Textus BoK does not assume
ownership of their complete domain models merely because they appear in the
map.

### Relationship

A `BokKnowledgeRelationship` should contain at least:

| Field | Meaning |
|---|---|
| `relationshipId` | Deterministic identity derived from dataset and edge identity |
| `subjectId` | Existing node identity |
| `predicate` | Stable source predicate or normalized BoK relation kind |
| `objectId` | Existing node identity |
| `label` | Human-readable predicate label |
| `relationshipKind` | Category membership, term reference, resource reference, component existence, evidence, or RDF relation |
| `evidence` | Attributable evidence for the relationship |

Every relationship endpoint must resolve to a returned node. Duplicate
identities with conflicting content must fail normalization. Exact duplicate
edges may be deduplicated only by a deterministic identity that preserves
evidence.

Candidate similarity, inferred association, and a shared tag without an
explicit source relation are not factual relationship kinds. If a later view
shows them, it must use a separate candidate model and visibly distinct state.

## Source Handoff

### Existing Resources

The existing resource kinds remain supported. The term decoder should retain
the bounded relationship metadata needed by the map, including term, article,
RDF, and tag references when those fields are present in the versioned Cozy
term document.

### RDF Graph Summary

The preferred topology input is an optional resource declared by the logical
KnowledgeSource manifest, provisionally named `rdf-graph-summary`. It should
resolve to the Cozy-generated `metadata/rdf/graph.json` shape only after the
schema name/version, size limits, node and edge fields, truncation meaning, and
compatibility policy are fixed.

The reader must:

- resolve the child reference as a safe relative CNCF resource;
- read it only through `ExecutionContext.resources`;
- reject malformed or unsupported schema versions;
- enforce finite node, edge, label, identifier, tag, and term-reference limits;
- preserve the source's `truncated` state as a typed warning;
- sort nodes and relationships by stable identity;
- reject dangling endpoints and conflicting duplicate identities; and
- avoid opening any referenced URI or rendered HTML page.

Unknown manifest resources remain ignorable compatibility inputs, but a graph
requested for the Knowledge Map must not be silently synthesized when no
recognized topology source exists.

## Selected-Generation Semantics

Knowledge topology should be part of `NormalizedBokSource` and committed to
`BokKnowledgeCatalog` only after SIE confirms the complete provider-neutral
dataset publication. A degraded publication retains the prior terms,
components, and topology together. Complete replacement removes stale nodes
and relationships together.

The Web query must never combine topology from one generation with terms or
component references from another generation.

## Proposed Query Contract

Add one public read operation, provisionally
`BokRetrieval.getKnowledgeMap`.

Proposed input:

| Field | Multiplicity | Meaning |
|---|---:|---|
| `datasetId` | `?` | Select one dataset; omission uses all admitted selected datasets |
| `sourceId` | `?` | Restrict by attributable source |
| `category` | `?` | Restrict to a category neighborhood |
| `termType` | `?` | Restrict term nodes by type |
| `focus` | `?` | Stable node, term identity, or title used as the map center |
| `nodeLimit` | `?` | Bounded requested node count |
| `relationshipLimit` | `?` | Bounded requested relationship count |

Proposed output:

- explicit query status;
- selected source/dataset generation summaries;
- deterministically ordered nodes and relationships;
- effective limits and truncation flags;
- typed warnings; and
- no synthetic empty node or relationship when the result is `no-match`.

The operation is required by the Web application. It should not become MCP
ready in the initial phase merely because it is a public query. MCP publication
requires a separate bounded-agent-use decision and executable projection
evidence.

## Web Application

### Route and Packaging

Retain the component-owned Static Form app identity `textus-bok`. Add authored
pages and assets under `src/main/web`, declare them in
`src/main/web-inf/web.yaml`, and package them in the CAR. The canonical
component route remains `/web/{component}/textus-bok`; an explicit subsystem
alias may provide `/web/textus-bok` where the assembly owns that route.

No CDN, remote script, or direct browser call to a provider is required.

### Pages

The initial page set should be small:

1. `index.html`: selected-generation summary, category/kind counts, search, and
   entry to the map.
2. `map.html`: filters, accessible result list/table, progressive graph, and a
   selected-node detail region.

A separate detail page may be added only if long evidence and relationship
sets cannot remain usable in the map page. The same query result must drive the
graph and its accessible fallback; the browser must not maintain a second
knowledge model.

### Interaction

- The first render provides a bounded useful map or a clear empty state.
- Dataset/source, category, term type, and focus filters submit through the
  normal operation/Form route.
- JavaScript progressively enhances the server-rendered result into an SVG
  graph and supports node selection, keyboard focus, and a reset-to-result
  action.
- The graph uses node kind plus labels/shapes so meaning never depends on color
  alone.
- Selecting a node updates one detail region with definition, typed
  relationships, source generation, and evidence.
- Evidence HTTP(S) links may be offered as explicit user actions. Other logical
  resource identifiers are displayed as identifiers and are not opened.
- Component nodes show existence identity and a CBD handoff action; they do not
  render CBD detail from BoK data.
- Truncation and degraded/retained-generation warnings remain visible near the
  result they qualify.

### Accessibility and Resilience

- The list/table view is complete enough to inspect the same result without
  JavaScript.
- Forms use native controls and labels; node actions use native buttons or
  links.
- The graph has a concise accessible description and mirrors selection in the
  detail region.
- The layout works at narrow widths by stacking filters, graph, and detail.
- Missing topology, no match, rejected input, and unavailable selected
  generation have distinct messages.

## Authorization and Data Safety

- `getKnowledgeMap` is read-only and uses normal CNCF operation authorization.
- `replaceKnowledgeSource` remains protected and has no map-page mutation
  control.
- Browser code receives only the bounded operation result needed for the view.
- Provider endpoints, credentials, configuration secrets, raw embedding
  vectors, and unrelated SIE documents are excluded.
- Source URI values are escaped for HTML and JavaScript contexts. URI values do
  not become links unless their scheme is explicitly admitted for navigation.

## Generated and Handwritten Boundaries

- CML owns operation, datatype, request, and response contracts.
- Generated Scala owns the generated component API and record conversion
  surfaces.
- `ComponentFactory` binds the operation to the catalog read model.
- `BokSourceReader` owns source decoding and normalization.
- `BokKnowledgeCatalog` owns selected-generation query semantics.
- `src/main/web-inf` owns descriptor/form metadata.
- `src/main/web` owns component-specific templates, local assets, and
  progressive visualization behavior.
- CNCF owns resource access, operation dispatch, authorization, form/result
  rendering, Web routing, and asset serving.

## Executable Specification Plan

### Domain and Source

- Decode a supported Cozy topology resource through an in-memory CNCF resource
  provider.
- Reject unsupported schema, unsafe child paths, dangling edges, conflicting
  duplicates, and configured limit violations.
- Prove deterministic sorting and stable relationship identities.
- Prove source truncation becomes a warning rather than an invented complete
  graph.
- Prove no rendered page, host file, provider endpoint, or referenced URI is
  opened.

### Catalog and Operation

- Prove topology changes become visible only with a complete publication.
- Prove degraded publication retains one internally consistent prior
  generation.
- Prove complete replacement removes stale nodes and edges.
- Prove filtering, focus, effective limits, truncation, and no-match behavior.
- Prove candidate retrieval results never appear as factual map edges.
- Prove component nodes remain existence-only.

### Web and Packaging

- Verify the generated Web descriptor declares the authored pages and local
  assets.
- Verify the CAR contains templates, CSS, JavaScript, and private layout
  resources at the expected paths.
- Verify the page works with a representative operation result without
  JavaScript and enhances the same result when JavaScript is enabled.
- Verify HTML/JavaScript escaping, admitted URI navigation, responsive layout,
  keyboard selection, empty state, warning state, and bounded large-result
  behavior.
- Run the app in a representative SAR using an actual Cozy BoK source and
  confirm that the displayed generation, node counts, edges, evidence, and CBD
  handoff agree with the operation response.

## Open Decisions Before Promotion

1. Freeze a versioned Cozy RDF graph-summary contract or select an already
   versioned equivalent source artifact.
2. Decide which RDF predicates become first-class map relationships and which
   remain available only in raw evidence.
3. Decide whether category and dataset are materialized virtual nodes or map
   grouping metadata.
4. Set default and maximum node/relationship limits from representative BoK
   sizes.
5. Define the precise CBD handoff URL/action available inside a representative
   SAR without coupling Textus BoK to CBD implementation classes.
6. Decide whether a later bounded MCP projection is useful; it is excluded from
   the initial phase.

## Promotion Requirement

Implementation should begin only after the ownership boundary and topology
semantics are promoted to `docs/design`, the operation/source/UI behavior is
promoted to `docs/spec`, and the Phase 6 P6-01 checklist items are checked with
review evidence.
