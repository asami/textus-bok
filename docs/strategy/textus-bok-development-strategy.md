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
12. Phase 7.5: Component-Contributed Knowledge Acceptance
13. Phase 8: Configured KnowledgeSource Startup Bootstrap
14. Phase 9: Knowledge Map MCP Publication

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
compatibility narrowing, and MCP schema parity. Phase 7.3 closed on 2026-08-15
with the profile-aware Static Form, shareable selection state, complete
no-JavaScript attribution and failure rendering, safe progressive behavior,
CAR packaging, and operator/developer guidance. Phase 7.4 closed on 2026-08-15
with one four-profile representative SAR, cross-surface positive and negative
isolation, exact REST/Web/MCP attribution, structured MCP and HTTP-200 Web
failure status, full framework and Textus test gates, CAR validation, and
converged review. The approved Phase 7 split is now complete.

Phase 7.5 is replanned as component-contributed knowledge acceptance. It waits
for the released CNCF Phase 73 domain-neutral hub and the released Textus
Knowledge Editor Phase 2 `textus.book` provider. It then verifies the generic
consumer boundary and the TKE -> CNCF -> Textus BoK path without making Textus
BoK the owner of Book semantics. Paper, generic WebResource, and other domain
knowledge remain deferred until their owning providers are defined.

Phase 8 closed on 2026-09-09. It consumes Cozy Phase 29 project-owned site
metadata finalization and CNCF Phase 70 post-assembly component activation to
load explicitly selected registered KnowledgeSource profiles before managed
server readiness. The accepted default-off `development` path rebuilds or
selects an accepted `website.d`, writes only the marked private profile and
bootstrap configuration, restarts the Control Center-managed Textus BoK
instance, and reads attributable terms with `profile=development`. Baseline,
current-publication, complete-replacement, malformed-metadata, safe-diagnostic,
and restoration behavior passed through the managed lifecycle. Phase 8 adds no
watcher, supervisor control, inferred profile, MCP administration, or second
publication implementation.

Phase 9 is the next executable priority and is independent of CNCF Phase 73,
TKE Phase 2, and BoK Phase 7.5. Textus BoK already has nine MCP-ready
terminology, component-reference, and semantic-knowledge reads. Phase 9 admits
the existing bounded `getKnowledgeMap` public operation as the tenth read-only
MCP operation, synchronizes discovery/schema/Help and stale four-read
documentation, retains permanent MCP exclusion of `replaceKnowledgeSource`,
and verifies a bounded map read through the Control Center-managed runtime on
port 18014. It does not add provider ingestion or domain knowledge semantics.

Phase 59.7 completion record: DOC-07 adds one additive, value-only semantic
metadata projection for the five existing semantic reads. It preserves source
digest meaning, validates only declared public values through ResourceAccess,
and retains the BOK existence-only handoff to CBD Support. Focused GREEN
validation, CAR lint, review, and phase transition remain parent-owned.

Release-only residuals from Phases 1–5 are intentionally deferred in
`docs/phase/deferred-release-work.md`. They remain outside active feature
phases until a release-readiness phase explicitly takes ownership; the Phase 7
series does not implicitly admit them.
