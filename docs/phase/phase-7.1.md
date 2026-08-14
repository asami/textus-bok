# Phase 7.1: Profile Registry and Resolution

Stage Status:
- Current status: CLOSED
- Current step: P7-B Profile Registry and Resolution (closed)
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

## Phase Hygiene Ledger

The final normal CAR lint and complete reviews also exposed issues outside the
P7-B registry/resolver closure boundary. They are persisted in
`docs/journal/2026/08/2026-08-14-phase-7.1-hygiene-ledger.md`:

- `P7.1-HY-01` — the external Python CAR descriptor helper still expects
  legacy root descriptor fields even though Cozy validates the canonical
  schema-v3 component identity;
- `P7.1-HY-02` — historical probes, example fixtures, and Phase 4 examples
  still use pre-canonical operation names or repository-index v1; and
- `P7.1-HY-03` — one unchanged pre-existing multiline CML description still
  loses a joining space in generated help.

Phase 7 release-readiness items `P7-HY-02` and `P7-HY-03` also remain open;
P7-B does not absorb ABI-baseline, published-plugin, or nominal-wrapper debt.

## Completion Conditions

Phase 7.1 closes only when every P7-02 item in
`phase-7.1-checklist.md` is checked, focused and Phase-release validation
evidence is recorded here, validated work is committed, and final review finds
no actionable Phase 7.1 issue. Closure hands control to Phase 7.2 but does not
start it.

## Closure Evidence

Phase 7.1 closed on 2026-08-14.

- Step commit `4b220065f39ea04625be0b660cc41568d8da3cb7` implements the
  private profile registry/resolver, configuration binding, authorization,
  complete-generation admission, degraded retention, and executable
  specifications. Cozy support commit
  `9db5afb45489559936b7a94d70136db3590cf453` corrects canonical CAR
  component-name identity linting.
- Repair Step commit `4ebc8dfddd5d34f352a279994401d8112ce1bc2b`
  aligns repository-index v2 identity, qualified MCP names, optional component
  organization lookup, deterministic ordering, and SIE publication identity.
- Focused registry validation passed 8/8 tests in invocation
  `75200-20260814T114427Z`. Final identity/CML validation included 10/10
  source-reader tests in `24319-20260814T131120Z` and 6/6 generated-domain/MCP
  tests in `27865-20260814T132131Z`.
- The post-commit Textus BoK full suite passed 8 suites and 40/40 tests in
  invocation `30304-20260814T132824Z`. The dependency Cozy full suite passed
  97 suites and 1325/1325 tests, with 7 expected cancellations, in invocation
  `97235-20260814T121810Z`.
- `cozyBuildCAR` produced `target/textus-bok-0.1.0-SNAPSHOT.car` from the
  post-commit tree in invocation `30643-20260814T132907Z`.
- Normal CAR lint reports integrated Cozy identity
  `CAR_COMPONENT_IDENTITY_CANONICAL`. Its only FAIL is the external stale
  descriptor helper recorded as `P7.1-HY-01`; inherited release-readiness
  warnings remain in the hygiene ledgers.
- The final complete review closed all six canonical-identity findings. The
  focused CML re-review reported `PASS` with zero actionable findings.
