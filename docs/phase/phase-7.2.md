# Phase 7.2: Read Contract Integration

Stage Status:
- Current status: CLOSED
- Current step: P7-C Read Contract Integration (closed)
- Owner: Textus BoK development
- Update rule: Update this block and `phase-7.2-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Split Provenance

Split from Phase 7 with explicit user approval on 2026-08-14. Phase 7.1 is the
predecessor and Phase 7.3 is the successor. This Phase exclusively owns the
unfinished pre-split P7-C Step and P7-03 checklist group.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: conservative upper bound <= 6h
- estimated_at_recommended_effort: 4–6h
- recommended_minimum_effort: xhigh
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from Phase 7

## Objective

Apply the closed Phase 7 selector and the Phase 7.1 resolver to every Textus
BoK read contract so exact records and provider-backed SIE candidates are
filtered to one resolved profile before classification and result limits, with
consistent attribution across REST and the four existing MCP-ready reads.

## Required Inputs and Dependencies

- Phase 7.1 must be CLOSED with deterministic profile resolution and structured
  failures available to callers inside the component boundary.
- Existing operation-level MCP readiness remains authoritative.
- Existing Knowledge Map `datasetId` and `sourceId` filters remain narrowing
  filters and cannot become alternate profile selectors.

## Scope

1. Add one shared optional profile selector and resolved-selection response
   model to term, component-reference, and Knowledge Map reads.
2. Default omission to `official` and preserve explicit project identity.
3. Constrain exact catalog entries and SIE candidates before classification,
   overfetch completion, and limits are applied.
4. Make existing dataset/source filters narrow only the resolved profile and
   fail incompatible combinations structurally.
5. Project the same optional selector through the four existing MCP tools
   while keeping mutation and Knowledge Map absent from MCP.

## Step

### P7-C: Read Contract Integration

Focus: P7-03.

Plan and deliver focused Slices for the CML model, generated/public request and
response surface, component resolver integration, exact catalog filtering,
SIE candidate filtering, MCP schemas, attribution, and compatibility evidence.
Each Slice owns focused validation; the Phase release owns full test, CAR build,
and normal CAR lint evidence for the changed public contract.

## Boundaries and Non-goals

- Do not add source registration/replacement to public reader forms or MCP.
- Do not make Knowledge Map MCP-ready.
- Do not add Web selector rendering or operator configuration guidance; Phase
  7.3 owns those surfaces.
- Do not merge or overlay profiles, infer a project from ambient state, or
  silently fall back to another profile.

## Validation Boundary

Focused executable specifications must prove CML request/response shape,
omitted-selector migration, resolver use, exact and SIE filtering before
limits, incompatible-filter failure, attribution, MCP schema parity for four
tools, and continued exclusion of mutation and Knowledge Map from MCP. At Phase
release, run the full Textus BoK test suite, `cozyBuildCAR`, and normal CAR
lint with recorded evidence.

## Completion Conditions

Phase 7.2 closes only when every P7-03 item in
`phase-7.2-checklist.md` is checked, focused and Phase-release validation
evidence is recorded here, validated work is committed, and final review finds
no actionable Phase 7.2 issue. Closure hands control to Phase 7.3 but does not
start it.

## Phase Hygiene Ledger

Phase 7.2 adds the public selector wrappers required by the closed read
contract. Their accepted lint-policy follow-up is persisted in
`docs/journal/2026/08/2026-08-15-phase-7.2-hygiene-ledger.md` as
`HYG-P7.2-001`. The Phase 7 and Phase 7.1 ledgers remain authoritative for the
pre-existing nominal wrappers, release ABI/plugin warnings, and external
descriptor-helper mismatch; Phase 7.2 does not duplicate or absorb those
items.

## Closure Evidence

Phase 7.2 closed on 2026-08-15.

- Step commit `176386977200f64e35a57a88e8c80191a22d56c5` adds the shared
  selector and resolved-selection response model, resolves and authorizes the
  exact normalized tuple before catalog access, filters exact and SIE reads
  before classification and limits, constrains Knowledge Map compatibility
  filters, preserves MCP readiness boundaries, and updates the public manuals.
- The post-fix affected-consumer accumulator passed 5 suites and 37/37 tests
  in invocation `36310-20260814T185425Z`. Its included federation publication
  specification exercises the public component boundary.
- The Phase-release full suite passed 8 suites and 51/51 tests in invocation
  `42094-20260814T190545Z`.
- `cozyBuildCAR` produced
  `target/textus-bok-0.1.0-SNAPSHOT.car` in invocation
  `42515-20260814T190621Z`; the artifact SHA-256 is
  `09d47014fc5766a8f287f442007a3bf620fd449d76889ae5eb3452f117bf8618`.
- Normal CAR lint reports integrated Cozy identity
  `CAR_COMPONENT_IDENTITY_CANONICAL` for
  `org.simplemodeling.textus.Bok`. The external descriptor helper still
  reports the inherited schema-v3 false positive recorded as `P7.1-HY-01`;
  inherited ABI, SNAPSHOT-plugin, and nominal-wrapper warnings remain in their
  existing hygiene ledgers. The two Phase 7.2 selector-wrapper warnings are
  recorded as `HYG-P7.2-001`.
- The complete post-implementation review found one naming issue, which was
  repaired without behavior change. The fresh focused re-review sealed
  `P7C1-R1` as closed with no actionable findings and no requirement for a new
  full review.
