# Phase 7.4: Representative Verification and Closure

Stage Status:
- Current status: OPEN
- Current step: P7-E Representative Verification and Closure
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
