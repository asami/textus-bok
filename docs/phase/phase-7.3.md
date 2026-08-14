# Phase 7.3: Web and Operator Integration

Stage Status:
- Current status: CLOSED
- Current step: P7-D Web and Operator Integration (closed)
- Owner: Textus BoK development
- Update rule: Update this block and `phase-7.3-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Split Provenance

Split from Phase 7 with explicit user approval on 2026-08-14. Phase 7.2 is the
predecessor and Phase 7.4 is the successor. This Phase exclusively owns the
unfinished pre-split P7-D Step and P7-04 checklist group.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: conservative upper bound <= 6h
- estimated_at_recommended_effort: 3–5h
- recommended_minimum_effort: high
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from Phase 7

## Objective

Expose the Phase 7.2 read-selection contract through the component-owned
Static Form Knowledge Map and operator/developer guidance while preserving
shareable state, complete no-JavaScript behavior, accessibility, safe
rendering, and resource/MCP boundaries.

## Required Inputs and Dependencies

- Phase 7.2 must be CLOSED with the shared public selector, structured failure,
  and resolved-attribution response contract available.
- The existing Static Form operation remains `getKnowledgeMap`, which remains
  outside MCP.

## Scope

1. Add `official`, `development`, and `project` choices to the Static Form.
2. Request project identity only when `project` is selected.
3. Preserve selection in shareable request state and identify resolved profile
   and evidence in complete no-JavaScript output.
4. Preserve progressive enhancement, keyboard access, narrow-screen behavior,
   and safe text/URI rendering for success and structured failures.
5. Document SAR preparation, freshness observation, and failure diagnosis for
   all three profile kinds.

## Step

### P7-D: Web and Operator Integration

Focus: P7-04.

Plan and deliver focused Slices for Static Form HTML, shareable parameter
state, progressive behavior, rendered attribution/failures, accessibility,
safe rendering, packaging, and operator/developer documentation. Each Slice
owns focused validation; the Phase release owns the full suite and CAR/Web
packaging evidence.

## Boundaries and Non-goals

- Do not introduce a Web-only selection contract or bypass the public operation.
- Do not expose raw resource locations, credentials, registration, or source
  replacement through the form.
- Do not make Knowledge Map MCP-ready.
- Do not run or close the all-profile representative SAR; Phase 7.4 owns that
  evidence.

## Validation Boundary

Focused executable specifications and static asset checks must cover the three
choices, conditional project identity, shareable request state, resolved
attribution, structured failures, complete no-JavaScript rendering,
progressive enhancement, keyboard access, narrow screens, safe text/URI
rendering, and packaging. At Phase release, run the full Textus BoK test suite,
`cozyBuildCAR`, and normal CAR lint with recorded evidence.

## Completion Conditions

Phase 7.3 closes only when every P7-04 item in
`phase-7.3-checklist.md` is checked, focused and Phase-release validation
evidence is recorded here, validated work is committed, and final review finds
no actionable Phase 7.3 issue. Closure hands control to Phase 7.4 but does not
start it.

## Closure Evidence

Phase 7.3 closed on 2026-08-15. P7-D was committed as
`6ced99478b35db65ccb8b2a0977b723f8376ec52`, adding the profile-aware Static
Form, shareable selection state, complete no-JavaScript attribution and
failure rendering, safe progressive behavior, CAR packaging, and
operator/developer guidance.

- Focused Web validation passed 5/5 tests in invocation
  `78441-20260814T202417Z`.
- Accumulated focused validation passed 14/14 tests in invocation
  `82683-20260814T202434Z`.
- The accumulated full review passed with no actionable findings.
- The Phase-release full suite passed 8 suites and 53/53 tests in invocation
  `39705-20260814T204856Z`.
- `cozyBuildCAR` passed in invocation `39040-20260814T204747Z`; the resulting
  CAR SHA-256 was
  `240b9a9e1a2df348e1704ea6455955a157817969d004e91008686ef7e1eccd88`.
- Normal CAR lint accepted the canonical schema-v3 identity, structure,
  packaging, CML descriptions, manuals, component factory, and internal DSL.
  The outer legacy-descriptor helper finding is the already-recorded
  `P7.1-HY-01`, not a Phase 7.3 blocker; inherited ABI, SNAPSHOT, and nominal
  wrapper warnings remain outside this Phase. Semantic documentation review
  found no actionable issue.

Phase 7.4 is the next delivery unit and remains unstarted.
