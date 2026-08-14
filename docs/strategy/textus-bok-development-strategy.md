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
- Treat the published `simplemodeling.org` BoK, the working
  `simplemodeling-org` BoK, and project-local BoKs as explicitly selected,
  attributable profiles. Never infer a profile from the current directory or
  let development/project content silently override official knowledge.

## Phase Overview

1. Phase 1: CAR Baseline
2. Phase 2: BoK Domain and Federation Baseline
3. Phase 3: Operational Documentation
4. Phase 4: BoK MCP Migration
5. Phase 5: SAR and CBD Handoff Verification
6. Phase 6: Knowledge Map Web Application
7. Phase 7: BoK Profile Selection Contract
8. Phase 7.1: Profile Registry and Resolution
9. Phase 7.2: Read Contract Integration
10. Phase 7.3: Web and Operator Integration
11. Phase 7.4: Representative Verification and Closure

## Current Priority

Phase 5 is closed. It verified the representative SIE, BoK, CBD Support, and
Scraper SAR, the four BoK-owned MCP reads, source ownership, and the
existence-to-detail CBD handoff without crossing component boundaries.

Phase 6 is closed. It delivers a versioned Cozy graph-summary handoff, atomic
selected-generation topology, the bounded public `getKnowledgeMap` query
(outside MCP), and the component-owned Static Form map with an accessible
fallback and progressive local SVG interaction. P6-E verified a live
KnowledgeHub source with 95 nodes, 150 relationships, and one explicit
component handoff; REST and Web agreed on the selected generation, topology,
evidence, truncation, and handoff. The clean full suite (24 tests), CAR build,
normal CAR lint, configuration check, and final review passed.

The original open Phase 7 was split with explicit user approval on 2026-08-14
into the sequential Phase 7, 7.1, 7.2, 7.3, and 7.4 delivery units. Phase 7
closed on 2026-08-14 with the stable selection and migration contract for the
published official `simplemodeling.org` BoK, the explicitly selected
development generation from `simplemodeling-org`, and an explicitly identified
project-local BoK. Phase 7.1 closed on 2026-08-14 with the private registry,
configuration-bound resolution, complete-generation admission, deterministic
failure behavior, and canonical component-identity integration. Phase 7.2
closed on 2026-08-15 with exact profile-scoped public reads, resolved-selection
attribution, pre-classification catalog and SIE filtering, Knowledge Map
compatibility narrowing, and MCP schema parity. Phase 7.3 is the next priority
and owns Web/operator integration, but it has not started. Phase 7.4 remains
the later representative runtime closure. Each child starts only after its
predecessor closes.

Release-only residuals from Phases 1–5 are intentionally deferred in
`docs/phase/deferred-release-work.md`. They remain outside active feature
phases until a release-readiness phase explicitly takes ownership; the Phase 7
series does not implicitly admit them.
