# Phase 10: Resident Multi-BoK In-Memory Catalog

Stage Status:
- Current status: CLOSED
- Current step: None. Phase 10 is closed; Phase 10.1 remains planned and is
  not started by this closure.
- Owner: Textus BoK development
- Update rule: Update this block and `phase-10-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

P10-02 was accepted on 2026-09-10 in local Step commit
`d891c74e3c08390aff3f34606934d41e18824c51` after focused
`BokFederationPublicationSpec` and `BokKnowledgeCatalogSpec` validation (28
passing tests) and independent lightweight review. Its sole authoring-gate
blocker was repaired within the frozen P10-02 boundary and the final M0
inspection accepted that correction. P10-02 establishes private resident
multi-BoK catalog composition; it does not claim any Phase 10.1 public read,
MCP, or two-source runtime surface.

P10-01 was accepted on 2026-09-10 in local Step commit
`96bc3bf060bcdd96add143d98fee324e5015385d` after focused
`BokFederationPublicationSpec` validation (18 passing tests) and an
independent lightweight review/re-review. It establishes the default
SIE-independent completion path and retained explicit SIE failure boundary;
it does not claim P10-02 catalog composition or any Phase 10.1 public surface.

## Release Closure

Phase 10 closed on 2026-09-10 after the accepted P10-01 and P10-02 Step
commits, the Phase full review, the bounded documentation repair for
`CPB-P10-FULL-001`, and a clean independent focused re-review. The final
closure binds the serialized full `sbt --batch test` receipt, normal CAR lint
receipt, complete review-disposition ledger, and this Phase/checklist state to
one distinct local release commit. The release continues to preserve Phase 7
profile isolation, source attribution, deterministic ordering, source-scoped
replacement, and the explicit SIE provider boundary.

`HYG-P10-001` records the already-corrected split-ownership wording as
nonblocking documentation hygiene in the canonical Phase 10 hygiene ledger.
The successor Phase 10.1 plan, checklist, README entry, and strategy
projection are adopted as a complete planned/not-started management set; no
Phase 10.1 public read, MCP, or two-source acceptance work is included here.

## Approved Split

On 2026-09-10, the user explicitly approved splitting the original open
Resident Multi-BoK Catalog and MCP Publication Phase into two delivery units:

1. Phase 10 (this Phase): P10-01 SIE-Independent In-Memory Knowledge Backend
   and P10-02 Resident Multi-BoK Catalog; and
2. Phase 10.1: P10-03 Catalog-Backed BoK Reads and MCP Publication and P10-04
   SimpleModeling.org + `bok-knowledgehub` End-to-End Acceptance.

The dependency order is Phase 10, then Phase 10.1. The original Phase was
PLANNED with every P10-01 through P10-04 checklist item unchecked, so no
completed Step, Slice, commit, validation result, approval, or historical date
was moved. P10-03 and P10-04 moved exactly once to Phase 10.1.

Approval evidence is the user's explicit `$cncf-split-phase Phase 10`
invocation on 2026-09-10 after confirming the decimal-suffixed child identity.
The split adds one Phase document, checklist, release review, validation, and
commit. That overhead is justified because the original calibrated expected
duration was 10.5h, above the 8h planning ceiling; this is not a
reasoning-cost-isolation split.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: calibrated expected duration centered on 6h; allowed ceiling 8h
- estimate_calibration: no comparable completed Phase duration record; use the
  original four-group evidence-based estimate without a global multiplier
- planning_demand: bounded-settled
- recommended_parent_profile: Terra / high
- profile_cost_role: lower-cost execution; no cost-isolation split is claimed
- expensive_reasoning_kernel: none
- frozen_profile_transition_handoff: none
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: 5.5h expected, 5-7h uncertainty; 30
  minutes below the 6h target and within the 8h ceiling
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none; this Phase is not below 4h
- rebalance_attempts_for_every_sub_5h_child: none; this Phase is not below 5h
- adjacent_merge_structural_rejection_evidence: merging P10-01/P10-02 with
  Phase 10.1 recreates the 10.5h original Phase, above the 8h ceiling
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: one additional Phase closure adds planning and release
  overhead but restores independently closable work below the ceiling
- agent_reasoning_mode_policy: default standard; consider pro only at an
  eligible agent launch when the active interface supports it and frozen
  quality-first evidence justifies it
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from the original Phase 10

## Pre-split Gate Evidence

On 2026-09-10, the original four-group Phase 10 received
`Phase Plan Gate: SPLIT_REQUIRED` with `split_reasons: [time-bound]`. Its
calibrated expected duration was 10.5h, with a 15h conservative upper bound,
against a 6h target and 8h ceiling. This is dated pre-split evidence, not the
current Phase Plan Gate.

## Objective

Make Textus BoK able to retain more than one independently selected complete
BoK generation in a BoK-owned in-memory resident catalog without requiring SIE.
The catalog preserves attribution, deterministic identity and ordering, and
source-scoped complete-generation replacement. Public catalog-backed read/MCP
semantics and the real two-source runtime acceptance belong to Phase 10.1.

## Starting Point

Phases 7 through 7.4 established explicit `official`, `development`, and
`project` profile selection, deterministic resolution to one complete
source/dataset/generation, exact attribution, REST/Web/MCP parity, and strict
cross-profile isolation.

That contract deliberately forbids cross-profile union during profile
resolution. Phase 10 composes only independently resolved and accepted selected
generations after that resolution; it does not weaken Phase 7 isolation.

The current source-publication path assumes SIE for complete-generation
federation. This Phase establishes the BoK-owned in-memory path for practical
local operation while retaining the existing SIE component boundary.

## External Baseline and Dependencies

Phase 10 uses the existing CNCF Component/Service/Operation runtime, resource
DSL source reads, CAR integration, and existing Cozy BoK
generation/finalization contract. It does not require a new Cozy or CNCF
feature Phase as a start gate. A proven generic upstream defect remains a
separate handoff rather than a local substitute.

## Selected Architecture

```text
profile/source A -> existing profile resolution -> selected generation A --\
                                                                          \
                                                                           -> ResidentBoKCatalog
                                                                          /
profile/source B -> existing profile resolution -> selected generation B --/
```

Equal terms from two sources remain separate source-qualified records. A
catalog identity includes attributable source-local identity, for example
`simplemodeling:domain-model` and `knowledgehub:domain-model`. Semantic
equivalence, `sameAs`, RDF reconciliation, embeddings, precedence, and overlay
rules remain out of scope.

## Work Stack

### P10-01: SIE-Independent In-Memory Knowledge Backend

Define a BoK-owned backend/store boundary used by catalog reads and provide its
in-memory implementation as the default/development path. It must admit,
retain, replace, and read one complete generation without SIE; preserve
source/dataset/generation/evidence attribution; retain the SIE path behind its
existing compatible provider/component boundary; and fail deterministically
when explicitly selected SIE is unavailable rather than silently falling back
to memory.

### P10-02: Resident Multi-BoK Catalog

Admit multiple independently resolved selected generations to one resident
catalog. Keep profile/source/dataset/generation/evidence attribution,
deterministic catalog identity and ordering, duplicate-name coexistence, and
source-scoped replacement/reload without rebuilding unrelated generations.

## Scope

1. BoK-owned in-memory storage for complete selected generations.
2. Multi-source resident catalog construction after independent profile
   resolution and admission.
3. Deterministic source-qualified identity, attribution, replacement, and
   failure semantics for that internal resident path.

## Non-Goals

- Catalog-backed public terminology/component reads and MCP projection; these
  belong to Phase 10.1.
- The SimpleModeling.org plus `bok-knowledgehub` running two-source/MCP E2E;
  this belongs to Phase 10.1.
- Removal or redesign of SIE, semantic federation, embeddings, `sameAs`, RDF
  reconciliation, precedence/overlay, write-back, source registration, reload
  administration, or a new MCP transport.
- Pre-emptive Cozy or CNCF feature development.

## Compatibility Rules

1. Existing Phase 7 profile selection remains valid for single-profile reads.
2. Profile resolution and catalog composition remain separate concerns.
3. A catalog member is always an explicitly admitted selected generation;
   ambient working-directory or process state cannot add one.
4. Existing source/dataset/generation/evidence identities remain authoritative
   provenance.
5. SIE-backed operation, when selected, continues to use its existing public
   component boundary.

## Validation Boundary

Each Step uses focused executable specifications for backend startup,
admission, replacement, source attribution, duplicate-name coexistence,
isolation, and deterministic ordering. The Phase release includes appropriate
full Textus BoK validation, `sbt --batch test cozyBuildCAR`, normal CAR
validation/lint, and independent review that Phase 7 isolation, attribution,
and the SIE component boundary remain intact. No real-source MCP runtime
acceptance is required here; Phase 10.1 owns it.

## Completion Conditions

Phase 10 closes only when every item in `phase-10-checklist.md` is checked and:

1. Textus BoK runs with the in-memory backend and no required SIE dependency;
2. at least two independently selected generations coexist in one resident
   catalog;
3. attribution, identity, ordering, and source-scoped replacement remain
   deterministic; and
4. full validation and independent review find no actionable Phase 10 issue.

Phase 10 stops after closure. Phase 10.1 is a separately invoked successor;
it is not started by this Phase.
