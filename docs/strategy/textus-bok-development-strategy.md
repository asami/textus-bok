# Textus BoK Development Strategy

## Purpose

Textus BoK owns BoK terminology, component-existence knowledge, source
interpretation, and BoK-specific MCP operations. Generic knowledge federation
belongs to Textus Semantic Integration Engine, while detailed CBD usage
guidance belongs to Textus CBD Support.

## Development Principles

- Keep BoK domain models and match semantics inside this CAR.
- Access SIE only through its public component contract.
- Read source material through the CNCF resource DSL.
- Keep generated component surfaces separate from handwritten domain behavior.
- Project only attributable source relationships as knowledge facts; keep
  provider-backed candidates visibly distinct from curated topology.
- Keep Web, MCP, CLI, and future projections on the same BoK-owned read
  semantics without making a public operation MCP ready implicitly.
- Preserve deterministic behavior with executable specifications before
  migrating MCP consumers.

## Phase Overview

1. Phase 1: CAR Baseline
2. Phase 2: BoK Domain and Federation Baseline
3. Phase 3: Operational Documentation
4. Phase 4: BoK MCP Migration
5. Phase 5: SAR and CBD Handoff Verification
6. Phase 6: Knowledge Map Web Application

## Current Priority

Phase 5 is closed. It verified the representative SIE, BoK, CBD Support, and
Scraper SAR, the four BoK-owned MCP reads, source ownership, and the
existence-to-detail CBD handoff without crossing component boundaries.

Phase 6 is open. Its objective is a component-owned, read-only Knowledge Map
Web application over the last completely selected Cozy BoK generation. The
first priority is to promote the provisional topology, query, and Web behavior
recorded in
`docs/notes/textus-bok-knowledge-map-web-application-spec-draft.md` into stable
design and specification contracts. Implementation follows only after the Cozy
graph-summary handoff, factual-versus-candidate relationship semantics, finite
limits, generated CML/API boundary, and Static Form packaging boundary are
fixed and executable examples are defined.

Release-only residuals from Phases 1–5 are intentionally deferred in
`docs/phase/deferred-release-work.md`. They remain outside Phase 6 until a
release-readiness phase explicitly takes ownership.
