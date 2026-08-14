# Phase 7.1: Profile Registry and Resolution

Stage Status:
- Current status: OPEN
- Current step: P7-B Profile Registry and Resolution
- Owner: Textus BoK development
- Update rule: Update this block and `phase-7.1-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Split Provenance

Split from Phase 7 with explicit user approval on 2026-08-14. Phase 7 is the
predecessor and Phase 7.2 is the successor. This Phase exclusively owns the
unfinished pre-split P7-B Step and P7-02 checklist group; the original Phase 7
retains P7-A/P7-01 and all pre-split evidence.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: conservative upper bound <= 6h
- estimated_at_recommended_effort: 4–6h
- recommended_minimum_effort: high
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from Phase 7

## Objective

Implement a private, deterministic registry and resolver that maps the closed
profile contract from Phase 7 to admitted complete Textus BoK generations
without exposing resource locations or ambient project discovery through
public read requests.

## Required Inputs and Dependencies

- Phase 7 must be CLOSED with its stable profile, project identity,
  attribution, migration, and structured-failure contract accepted.
- The accepted contract authorities are
  `docs/design/bok-profile-selection.md` and
  `docs/spec/bok-profile-selection-contract.md`.
- CNCF resource access remains the only source-reading boundary.
- Existing complete-generation commit and degraded-generation retention
  behavior remains authoritative.

## Scope

1. Represent one official binding, one explicit development binding, and
   project-local bindings keyed by explicit project identity.
2. Bind profile identities to admitted logical resources through component or
   SAR configuration, never through public caller-supplied locations.
3. Resolve only complete source/dataset generations and preserve evidence.
4. Reject duplicate, missing, stale, ambiguous, unauthorized, and ambiently
   inferred bindings without falling back to another profile.
5. Retain the prior complete generation when a replacement degrades.

## Step

### P7-B: Profile Registry and Resolution

Focus: P7-02.

Plan and deliver focused Slices for the registry model, configuration binding,
resolver invariants, complete-generation admission, failure taxonomy, and
executable specifications. Each Slice owns focused validation; the Phase
release owns the full Textus BoK suite and CAR build appropriate to changed
runtime behavior.

## Boundaries and Non-goals

- Do not add the selector to public CML operations; Phase 7.2 owns that work.
- Do not change Static Form UI or operator guidance; Phase 7.3 owns them.
- Do not run the all-profile representative SAR; Phase 7.4 owns it.
- Do not accept URLs, filesystem paths, credentials, current directories, Git
  state, usernames, or provider configuration as public selection input.
- Do not let development/project content claim or replace the official binding.

## Validation Boundary

Focused executable specifications must cover deterministic resolution,
duplicate binding rejection, missing/unavailable profiles, complete-generation
admission, degraded retention, evidence, isolation, authorization, and absence
of direct filesystem/network/ambient discovery. At Phase release, run the full
Textus BoK test suite and `cozyBuildCAR`; run normal CAR lint when the delivered
CAR contract or descriptor surface changes.

## Completion Conditions

Phase 7.1 closes only when every P7-02 item in
`phase-7.1-checklist.md` is checked, focused and Phase-release validation
evidence is recorded here, validated work is committed, and final review finds
no actionable Phase 7.1 issue. Closure hands control to Phase 7.2 but does not
start it.
