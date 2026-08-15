# Phase 7.4: Representative Verification and Closure

Stage Status:
- Current status: CLOSED
- Current step: P7-E Representative Verification and Closure closed
- Owner: Textus BoK development
- Update rule: Update this block and `phase-7.4-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Split Provenance

Split from Phase 7 with explicit user approval on 2026-08-14. Phase 7.3 is the
predecessor. This is the last child in the approved split and has no approved
successor. It exclusively owns the unfinished pre-split P7-E Step and P7-05
checklist group.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: conservative upper bound <= 6h
- estimated_at_recommended_effort: 4–6h
- recommended_minimum_effort: high
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from Phase 7

## Objective

Verify the completed Phase 7–7.3 contract in one representative SAR containing
official, development, and two distinct project-local generations, prove
cross-surface isolation and attribution, and close the selectable-profile
delivery with full tests, CAR validation, documentation, review, and a
validated Phase release commit.

## Required Inputs and Dependencies

- Phases 7, 7.1, 7.2, and 7.3 must all be CLOSED in dependency order.
- Their accepted contract, resolver, read, MCP, and Web evidence is reused
  only while the relevant trees and inputs remain unchanged.
- Knowledge Map remains outside MCP and source mutation remains private.

## Scope

1. Cover profile vocabulary, resolver isolation, filtering, attribution,
   defaulting, Web, MCP, authorization, and structured failures with focused
   executable specifications.
2. Register official, development, and two distinct project-local generations
   in one representative SAR.
3. Prove that no request observes records from a non-selected profile/project.
4. Prove equivalent resolution and attribution across REST, Static Form Web,
   and the respective MCP reads while preserving distinct operation surfaces.
5. Complete full tests, CAR build/lint, representative checks, documentation,
   final review, and the Phase 7.4 release commit.

## Step

### P7-E: Representative Verification and Closure

Focus: P7-05.

Plan focused Slices for coverage gaps, representative SAR preparation,
cross-surface runtime evidence, documentation synchronization, and closure.
Focused checks stay within their Slices. The final Phase-release gate owns the
full Textus BoK suite, `cozyBuildCAR`, normal CAR lint, representative runtime
checks, full review, and release commit.

## Boundaries and Non-goals

- Do not reopen closed child contracts without a new actionable finding and
  the owning Phase's documented correction process.
- Do not make Knowledge Map MCP-ready, expose source mutation, or add
  cross-profile merge/overlay/write-back.
- Do not start an unapproved successor after Phase 7.4 closes.
- Release-only DP items remain in `deferred-release-work.md`.

## Validation Boundary

Focused executable specifications must close any remaining coverage gaps. The
final release gate must run the full Textus BoK test suite, `cozyBuildCAR`,
normal CAR lint, and the representative selection checks, and must record the
exact inputs/results. Documentation and final review must confirm REST, Web,
MCP, generation/evidence attribution, unavailable behavior, and all remaining
boundaries.

## Completion Conditions

Phase 7.4 closes only when every P7-05 item in
`phase-7.4-checklist.md` is checked, representative and full-release evidence
is recorded here, all required documents agree, validated work is committed,
and final review finds no actionable Phase 7.4 issue. Stop after closure; no
successor is implied or started.

## Closure Evidence

Phase 7.4 closed on 2026-08-15. The representative SAR contains official,
development, project-alpha, and project-beta generations and proves positive
and foreign-selection isolation across REST terminology, REST Knowledge Map,
Static Form Web, and qualified MCP reads. It additionally proves two MCP and
two HTTP-200 Web structured failures without fallback attribution. The final
runtime marker was:

`BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 web_maps=4 mcp_terms=4 negative_rest_terms=4 negative_mcp_terms=4 negative_rest_maps=4 negative_web_maps=4 mcp_failures=2 web_failures=2`

The lifecycle marker was
`BOK_PROFILE_SELECTION_SAR_LIFECYCLE_OK profiles=4`; the server emitted no
error and port 19547 was vacant after cleanup.

Release validation used unchanged committed inputs:

- cloud-native-component-framework full test invocation
  `75617-20260815T054922Z`: 444 suites, 3269 succeeded, 0 failed,
  13 canceled, 1 ignored, and 46 pending;
- Textus BoK full test invocation `78287-20260815T055517Z`: 8 suites,
  53 succeeded, and 0 failed;
- final `cozyBuildCAR` invocation `85822-20260815T061203Z`: built
  `target/textus-bok-0.1.0-SNAPSHOT.car`;
- normal CAR lint: integrated Cozy identity and project/build/source/
  packaging/documentation checks passed. The external legacy descriptor helper
  and inherited ABI/SNAPSHOT/nominal-wrapper warnings remain recorded in
  `docs/journal/2026/08/2026-08-15-phase-7.4-hygiene-ledger.md` and are not
  Phase 7.4 runtime blockers;
- final representative SAR: exit 0 with the exact markers above.

Validated delivery commits include Textus representative preparation
`09e4dab812bbbc94ff0c6c077ee36c03c916988c`, framework SAR identity,
configuration, and failure-transport commits `1b09cfb2cba0cf4d4c745cd2594de46f22b06ae0`,
`10ffdf4accd6430987197cb0f322386a0b47eb9b`, and
`ac9adb098b6bb475a847e21d407bf8d917c772b7`, cross-surface framework commit
`d416acda2659a4999357fc2512f6621aab0daca6`, Textus representative closure
commit `585739231a2c478348d56c97320a862374226d60`, and release-gate fixture
stabilization commit `79c378c0c7ed35679571ac3a77aaeb43f967cfd7`.

The implementation full review found one untrusted Static Form query-property
spoofing path plus three local naming/executable-spec hygiene findings. The
repair filters both `error.appStatus` and its query-context alias, retains only
trusted escaped failure metadata, and closed every finding. Independent
focused re-review converged with zero actionable findings. No successor Phase
was approved or started.
