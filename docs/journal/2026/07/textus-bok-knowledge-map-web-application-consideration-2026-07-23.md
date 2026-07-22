# Textus BoK Knowledge Map Web Application Consideration — 2026-07-23

## Context

The next Textus BoK capability should present the content produced by a Cozy
BoK as a Web application organized around knowledge relationships. The desired
surface is not an editor and is not a raw RDF administration console. It should
let a reader start from a term or category, see attributable relationships, and
follow the evidence back to the source BoK.

This entry records the consideration that followed the Phase 5 SAR and CBD
handoff closure. It is historical context, not a normative behavior contract.
The provisional implementation contract is recorded separately in
`docs/notes/textus-bok-knowledge-map-web-application-spec-draft.md`.

## Confirmed Baseline

- `src/main/web-inf/web.yaml` already declares a component-owned Static Form
  app at the `textus-bok` Web route.
- `src/main/web-inf/form.yaml` exposes source replacement and term search as
  forms, but it does not define a knowledge overview, graph, or evidence detail
  page.
- `BokRetrieval` currently owns one private source-replacement command and four
  public read operations for terms and existence-only CAR/SAR references.
- `BokKnowledgeCatalog` keeps only the last completely published normalized
  generation. This is the correct atomic visibility boundary for a Web read
  model as well as for the existing query operations.
- `BokSourceReader` reads structured metadata through the CNCF resource DSL and
  recognizes glossary terms, component repository indexes, and current Cozy
  component-reference indexes. It intentionally does not read rendered HTML.
- The current term decoder retains identity, title, definition, category, and
  term type. Cozy term metadata can carry `term_refs`, `article_refs`,
  `rdf_refs`, tags, and related source information that Textus BoK does not yet
  retain.
- The CNCF Static Form App contract permits component-authored HTML, private
  layouts and partials, and local CSS/JavaScript under `src/main/web`. A custom
  Textus BoK page therefore does not require turning Static Form into a general
  client framework or copying the Cozy site renderer.

## Actual Cozy BoK Observation

An actual KnowledgeHub BoK build was inspected on 2026-07-23. Its generated
`metadata/rdf/graph.json` contained 131 nodes and 234 edges with
`truncated=false`. Nodes carried identifiers, labels, node type, category,
degree, terms, and tags. Edges carried source, target, predicate, label,
category, terms, and tags.

The graph proves that Cozy already emits enough structured topology to support
a useful knowledge view. It also proves that rendering every generated triple
without a projection policy would be noisy: dates, localized labels, generated
page variants, schema classes, and other structural resources appear beside
domain knowledge. Textus BoK therefore needs a bounded, attributable Knowledge
Map projection rather than a direct dump of the RDF graph.

## Considered Options

### Reuse the generated Cozy Web site

This gives a rich dashboard and RDF viewer immediately, but it leaves the
feature outside the Textus BoK application and bypasses the selected complete
generation held by the component. It also makes the Web behavior depend on a
rendered-site location rather than the CNCF resource boundary.

### Render the complete RDF graph in Textus BoK

This preserves graph fidelity but mixes domain relationships with structural
and presentation triples. Large graphs also need limits, focus rules, and
evidence handling that a raw renderer does not provide.

### Add a Textus BoK Knowledge Map projection

This option retains selected Cozy metadata, constructs a typed and bounded
read model from the same complete generation as the catalog, and renders it in
the component-owned Web app. It can distinguish curated relationships from
retrieval candidates, retain evidence, and preserve the existing SIE and CBD
ownership boundaries.

## Direction

Proceed with the Textus BoK Knowledge Map projection.

The initial product surface should contain:

- an overview that summarizes the selected dataset by category and knowledge
  kind;
- a filterable map centered on terms and attributable references;
- a keyboard-accessible list/table fallback for the same result;
- a selected-item detail area showing definition, relationship predicates,
  source identity, and evidence; and
- an existence-only CAR/SAR node that hands component identity to Textus CBD
  Support instead of displaying CBD-owned detail.

The graph must show only relationships present in admitted structured source
metadata. Semantic retrieval candidates may be presented as candidates in a
separate search experience, but they must not become factual Knowledge Map
edges. The Web app is read-only in the initial phase. Source replacement stays
protected and remains absent from MCP.

## Ownership Boundary

- Textus BoK owns normalization of BoK-specific topology, the selected complete
  generation, Knowledge Map query semantics, and the Web projection.
- Cozy owns generation of the BoK source metadata and RDF graph summary.
- SIE owns generic federation, provider retrieval, and candidate scoring. It
  does not own the Textus BoK Web page.
- Textus CBD Support owns component capabilities, dependencies, compatibility,
  operations, and usage guidance.
- CNCF owns resource access, generated operation contracts, authorization, and
  Static Form App serving and template behavior.

## Documentation Result

Phase 5 remains closed. Phase 6, `Knowledge Map Web Application`, is opened as
the next strategy phase. Its first boundary is promotion of the provisional
note into stable design and specification documents before implementation
changes begin. No application implementation is claimed by this journal entry.
