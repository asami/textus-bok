# Phase 6 Checklist: Knowledge Map Web Application

This checklist is the authoritative Phase 6 state ledger. The summary and
current step are recorded in `phase-6.md`.

## P6-01: Ownership and Knowledge Semantics

- [x] A stable design document fixes Textus BoK, Cozy, SIE, CBD Support, and
  CNCF responsibilities for the Knowledge Map.
- [x] An authoritative specification distinguishes admitted factual
  relationships, evidence, semantic candidates, and unsupported inference.
- [x] The selected-generation invariant covers terms, component references,
  Knowledge Map nodes, and relationships as one atomic catalog view.

## P6-02: Cozy Topology Handoff

- [x] A versioned Cozy source contract defines the graph-summary resource,
  schema compatibility, node/edge fields, truncation, and source evidence.
- [x] Default and maximum node, relationship, identifier, label, term, and tag
  limits are fixed from representative Cozy BoK evidence.
- [x] Article, scenario, project, bibliography, tag, and RDF resources have an
  explicit reference-node policy that does not transfer their domain ownership
  to Textus BoK.

## P6-03: Source and Catalog Read Model

- [x] `BokSourceReader` normalizes admitted topology only through CNCF resource
  access and retains required term/article/RDF/tag references.
- [x] Source normalization rejects unsafe paths, unsupported schema, dangling
  edges, conflicting duplicates, and finite-limit violations deterministically.
- [x] `BokKnowledgeCatalog` replaces topology atomically on complete
  publication and retains the complete prior generation after degraded
  publication.
- [x] Executable specifications prove deterministic identities/order,
  truncation warnings, stale topology removal, and absence of rendered-page,
  host-filesystem, provider, or referenced-URI reads.

## P6-04: Knowledge Map Query

- [x] CML defines typed Knowledge Map node, relationship, request, response,
  status, limit, and warning contracts.
- [x] A public read operation supports bounded dataset/source, category, term
  type, and focus selection with explicit no-match and truncation behavior.
- [x] Operation implementation reads the selected catalog generation and never
  turns provider candidate scores into factual edges.
- [x] MCP readiness remains unchanged, or a separately documented bounded MCP
  decision and executable projection evidence explicitly authorize the new
  operation.

## P6-05: Component Web Application

- [x] The `textus-bok` Static Form app packages authored overview and map pages,
  private layout/partials where needed, and local CSS/JavaScript assets.
- [x] One bounded operation result drives filters, graph, selected-node detail,
  warnings, and the complete no-JavaScript list/table fallback.
- [x] The Web view exposes definitions, relationship predicates, source
  generation, and evidence while keeping component nodes existence-only with
  an explicit CBD handoff.
- [x] Browser behavior is responsive, keyboard accessible, safe for HTML and
  JavaScript contexts, and permits navigation only for admitted URI schemes.
- [x] Normal CNCF operation authorization protects the query; the Web app has
  no source-replacement or other mutation control.

## P6-06: Verification, Documentation, and Closure

- [ ] Targeted domain, source, catalog, operation, Web descriptor, asset
  packaging, no-JavaScript, progressive-enhancement, security, and
  accessibility specifications pass.
- [ ] A representative SAR loads an actual Cozy BoK source and proves that the
  operation and Web view agree on generation, nodes, relationships, evidence,
  truncation, and CBD handoff.
- [ ] The full Textus BoK test suite, `cozyBuildCAR`, normal CAR lint, and
  relevant SAR checks pass with recorded evidence.
- [ ] README, strategy, domain/developer/operator documents, and phase evidence
  describe the delivered surface and its remaining boundaries accurately.
- [ ] Final review has no actionable Phase 6 finding, validated work is
  committed, and the Phase 6 Stage Status is closed.

Phase 6 closes only while every checklist item remains checked and validation
evidence remains recorded in `phase-6.md`.
