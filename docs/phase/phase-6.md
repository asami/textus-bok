# Phase 6: Knowledge Map Web Application

Stage Status:
- Current status: OPEN
- Current step: P6-01 design and specification promotion
- Owner: Textus BoK development
- Update rule: Update this block and `phase-6-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Objective

Provide a component-owned, read-only Web application that projects the last
completely selected Cozy BoK generation as a bounded, attributable Knowledge
Map. The application must let a reader navigate from categories and terms to
related BoK resources, evidence, and existence-only CAR/SAR references without
presenting retrieval candidates as facts or crossing SIE and CBD ownership
boundaries.

## Inputs

- Consideration record:
  `docs/journal/2026/07/textus-bok-knowledge-map-web-application-consideration-2026-07-23.md`
- Provisional implementation contract:
  `docs/notes/textus-bok-knowledge-map-web-application-spec-draft.md`
- Existing domain contract: `docs/spec/bok-domain-model.md`
- Existing operator and developer boundaries in `src/main/car/manual` and
  `docs/developer-guide.md`

The journal and note are not normative. P6-01 promotes the accepted behavior
into stable design and specification documents before implementation.

## Decision Record

P6-DEC-01 was accepted on 2026-07-23: Cozy publishes a versioned
`cozy.rdf-graph-summary.v1` producer contract before Textus BoK consumes graph
topology. The accepted design and specification are
`docs/design/bok-knowledge-map.md` and
`docs/spec/bok-knowledge-map-contract.md`.

## Scope

1. Freeze the Cozy structured topology handoff and Textus BoK ownership
   boundary.
2. Extend source normalization and selected-generation state with deterministic
   evidence-bearing knowledge topology.
3. Add one bounded public Knowledge Map query without automatically publishing
   it through MCP.
4. Add authored Static Form pages and local assets for overview, map, fallback
   list/table, filters, and selected-node evidence.
5. Verify source safety, atomic generation visibility, operation semantics,
   Web packaging, accessibility, authorization, and representative live use.

## Subphases and Steps

### P6-A: Contract Promotion

Focus: P6-01 and P6-02.

- Promote ownership, factual relationship semantics, and selected-generation
  invariants to design/spec.
- Freeze a versioned Cozy graph-summary handoff and finite resource limits.

### P6-B: Source and Catalog Read Model

Focus: P6-03.

- Normalize admitted topology through the CNCF resource DSL.
- Commit terms, components, nodes, and relationships atomically after complete
  SIE publication.

### P6-C: Public Query Contract

Focus: P6-04.

- Generate and implement a bounded Knowledge Map query with explicit filtering,
  focus, truncation, warnings, and no-match behavior.
- Keep initial MCP readiness unchanged unless a separate bounded-agent-use
  decision is accepted and verified.

### P6-D: Component Web Application

Focus: P6-05.

- Package component-authored pages, private layout resources, CSS, and
  progressive JavaScript under the existing Static Form app.
- Render one operation result as both an accessible no-JavaScript view and an
  enhanced graph/detail interaction.

### P6-E: Representative Verification and Closure

Focus: P6-06.

- Validate a real Cozy BoK source in a representative SAR.
- Complete full tests, CAR build/lint, documentation, review, and phase closure
  evidence.

## Boundaries

- No direct filesystem, network, Fuseki, Chroma, embedding, or rendered HTML
  access is added to Textus BoK normalization or Web behavior.
- No semantic candidate becomes a factual Knowledge Map relationship.
- No CBD capability, dependency, compatibility, operation, or usage detail is
  imported into the BoK model.
- No source replacement or other mutation is exposed by the reader Web app or
  MCP.
- No generated Cozy dashboard or RDF viewer implementation is copied into the
  CAR; only admitted structured metadata crosses the source boundary.
- CNCF continues to own resource access, operation dispatch, authorization,
  Web routing, templates, and asset serving.

## Completion Conditions

Phase 6 closes only when every item in `phase-6-checklist.md` is checked,
representative live evidence is recorded, the complete selected generation is
shown consistently across the domain query and Web view, and final review finds
no actionable Phase 6 issue.
