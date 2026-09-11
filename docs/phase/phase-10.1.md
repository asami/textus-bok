# Phase 10.1: Catalog-Backed BoK Reads and MCP Two-Source Acceptance

Stage Status:
- Current status: CLOSED (effected by this distinct Phase release commit)
- Current step: Complete
- Owner: Textus BoK development
- Update rule: Update this block and `phase-10.1-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Split Provenance

Split from Phase 10 with explicit user approval on 2026-09-10. Phase 10 is the
predecessor; no later successor is currently scheduled. This Phase exclusively
owns the unfinished pre-split P10-03 and P10-04 work and their checklist groups.
Phase 10 retains P10-01/P10-02 and all pre-split history; no completed record
was moved because the source Phase was still PLANNED.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: calibrated expected duration centered on 6h; allowed ceiling 8h
- estimate_calibration: no comparable completed Phase duration record; use the
  original four-group evidence-based estimate without a global multiplier
- planning_demand: protected-decision
- recommended_parent_profile: Terra / xhigh
- profile_cost_role: lower-cost execution; no cost-isolation split is claimed
- expensive_reasoning_kernel: none
- frozen_profile_transition_handoff: none; consume ordinary accepted Phase 10
  catalog behavior and closure evidence
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 5h expected, 4.5-6.5h uncertainty; 60
  minutes below the 6h target and within the 8h ceiling
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none; this Phase is not below 4h
- rebalance_attempts_for_every_sub_5h_child: none; this Phase is not below 5h
- adjacent_merge_structural_rejection_evidence: merging with Phase 10
  recreates the 10.5h original Phase, above the 8h ceiling
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: one additional Phase closure adds planning and release
  overhead but restores independently closable work below the ceiling
- agent_reasoning_mode_policy: default standard; consider pro only at an
  eligible agent launch when the active interface supports it and frozen
  quality-first evidence justifies it
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from Phase 10

## Objective

Make existing public BoK terminology/component reads and their existing CNCF
MCP projection operate against the accepted resident catalog, preserving
distinct source attribution and deterministic read semantics. Then prove the
practical two-source path with SimpleModeling.org and `bok-knowledgehub` in one
in-memory Textus BoK process where BoK selects `memory` and does not select or
require an SIE knowledge backend. The acceptance SAR includes SIE, BoK, and
Scraper as sibling CARs: SIE is non-MCP, and Scraper closes SIE's required
dependency.

## Required Inputs and Dependencies

- Phase 10 must be CLOSED with its BoK-owned in-memory backend, resident
  catalog, source-qualified identity, deterministic ordering, attribution, and
  source-scoped replacement behavior accepted.
- Existing Phase 7 profile resolution remains the independent selection and
  admission boundary. Catalog composition cannot bypass it.
- Existing CNCF Component/Service/Operation MCP projection remains the only
  MCP transport/projection boundary.
- Existing Cozy-generated BoK source artifacts remain the representative input;
  a proven generic upstream defect is a separate bounded handoff.

## Work Stack

### P10-03: Catalog-Backed BoK Reads and MCP Publication

Route existing BoK-owned read operations through the resident catalog for the
in-memory runtime path. Preserve exact, ambiguous, conflict,
insufficient-evidence, and no-match behavior where applicable; return
attributable multi-source candidates/search results where permitted; and keep
source registration, replacement, reload, and mutation non-MCP-ready. Reuse
the existing CNCF MCP projection rather than creating a second BoK-specific
MCP server.

### P10-04: SimpleModeling.org + bok-knowledgehub End-to-End Acceptance

Admit SimpleModeling.org / `simplemodeling-org` glossary knowledge and
`bok-knowledgehub` generated knowledge into the same in-memory catalog. Prove
that known terms from both sources are retrievable with distinct attribution,
a query can return both, replacement of one leaves the other intact, and the
representative read succeeds through MCP `tools/call` while BoK selects
`memory` and neither selects nor requires an SIE knowledge backend. The
acceptance SAR contains SIE, BoK, and Scraper as sibling CARs, with SIE
non-MCP and Scraper closing SIE's required dependency.

## Scope

1. Catalog-backed terminology/component read semantics and their public
   request/response compatibility decision.
2. Existing MCP-ready operations projecting those same semantics through CNCF.
3. Two-real-source in-memory runtime and MCP representative acceptance.
4. Regression evidence for source attribution, deterministic failure behavior,
   and the continuing non-MCP mutation boundary.

## Non-Goals

- Reimplementing the Phase 10 backend/catalog boundary or weakening its source
  identity, ordering, replacement, or Phase 7 isolation guarantees.
- A new BoK-specific MCP server, MCP administration, source mutation,
  registration, replacement, reload, arbitrary source locations, or write-back.
- SIE removal, semantic federation, embeddings, RDF/sameAs reconciliation,
  precedence/overlay, new Web UI, or pre-emptive Cozy/CNCF feature work.

## Validation Boundary

Each Step uses focused REST/operation/MCP executable regressions. The Phase
release includes the representative running two-source scenario, MCP
`tools/call`, appropriate full Textus BoK validation, `sbt --batch test
cozyBuildCAR`, normal CAR validation/lint, and an independent final review that
Phase 7 isolation, attribution, source-mutation exclusion, and component
boundaries remain intact.

## Completion Conditions

Phase 10.1 closes only when every item in `phase-10.1-checklist.md` is checked
and:

1. existing public reads use the resident catalog with attributable multi-source
   results where their semantics permit them;
2. existing MCP-ready operations expose exactly those read semantics without a
   duplicate transport or administrative mutation surface;
3. SimpleModeling.org and `bok-knowledgehub` complete the representative
   two-source in-memory/MCP path with BoK selecting `memory` and not selecting
   or requiring an SIE knowledge backend; the acceptance SAR includes SIE, BoK,
   and Scraper as sibling CARs, with SIE non-MCP and Scraper closing SIE's
   required dependency; and
4. full validation and independent review find no actionable Phase 10.1 issue.

Phase 10.1 stops after closure. It does not start a later Phase automatically.

## Closure Evidence

- P10-03/P10-04 implementation and two-source acceptance are committed in
  `71d76a25013a8cf9325dd24858fd8622deeb5f05`.
- The serial Phase validation `sbt --batch test cozyBuildCAR` passed 91 tests
  and CAR construction.  Its immutable receipt is
  `1358a8b956004c2d5e4c7df23e9a78a68156b72a1e448230ccb8af94dabed813`.
- The two-source sibling-CAR SAR passed with `components=3`, `sources=2`, and
  `backend=memory`; it verified the exact ten BoK MCP reads, two catalog
  members, protected REST-only replacement, and owned runtime cleanup.
- Normal CAR lint has no `FAIL`.  Its residual ABI/SNAPSHOT/CML/manual-link
  warnings are retained as the separate Phase 10.1 Hygiene records.
- The independent focused re-review `P10.1-FOCUSED-REREVIEW-003` closed the
  final direct-versus-catalog manual-attribution finding with no new blocker.
