# Phase 7.5: Information Profile Verification

status=planned
planned_at=2026-09-03
predecessor=[Phase 7.4](phase-7.4.md)
parallel_to=[Phase 8](phase-8.md)
strategy=[Textus BoK Development Strategy](../strategy/textus-bok-development-strategy.md)
checklist=[Phase 7.5 Checklist](phase-7.5-checklist.md)
consumes_handoff=qualified CNCF Phase 61.5.1 IC-07B closure

## Stage Status

- Current status: PLANNED
- Owner: Textus BoK maintainers
- Update rule: Update this Phase and its checklist only from accepted profile
  fixtures, executable specifications, review evidence, or an explicit scope
  decision.
- Entry rule: Phase 7.4 remains closed. This Phase owns only the deferred
  profile-verification residue and does not modify the Phase 8 bootstrap plan.

## Goal

Verify the remaining representative Information profiles at the BoK CAR
boundary: Book, Paper, WebResource, Person, Organization, and textual
Work/Edition/Series/Volume relationships must preserve selected-profile
attribution, isolation, and evidence semantics through BoK's public reads.

## Scope

- Freeze a compact representative fixture matrix for the declared profile
  families and their textual-work relationships.
- Verify profile selection, source/generation attribution, isolation, and
  failure behavior through the established BoK read contract.
- Exercise the SIE dependency only through its declared component contract.
- Record whole-profile acceptance evidence without changing profile semantics
  or adding a new public operation merely for verification.

## Non-Goals

- Packaged multi-CAR runtime composition, launcher ownership, or cross-CAR
  Help/API acceptance; SIE Phase 7 owns that bounded runtime work.
- Knowledge Editor list/detail/edit/lifecycle, Tag filtering, or local
  Knowledge materialization; Textus Knowledge Editor Phase 1 owns that work.
- Phase 8 startup bootstrap implementation or its Cozy/CNCF supplier
  dependencies.
- A new profile model, selector, source reader, or public API.

## Work Groups

### P7.5-A: Fixture and Contract Freeze

Define the smallest profile matrix that represents each deferred family and
its source/generation attribution. Reuse the Phase 7--7.4 profile contract;
do not reinterpret its selection semantics.

### P7.5-B: Representative Profile Acceptance

Verify the declared profiles through BoK public reads and the relevant
REST/Web/MCP projections. Confirm positive, negative, isolation, and evidence
behavior without relying on a broad runtime composition.

### P7.5-C: Closure

Run proportionate BoK executable specifications and normal CAR structural
validation, reconcile documentation, complete independent review, and commit
the accepted Phase result.

## Completion Conditions

Phase 7.5 closes only when every checklist item is checked and the accepted
profile matrix demonstrates the declared read, attribution, isolation, and
failure contracts without broadening the public component surface.

## Handoff Source

- [CNCF Phase 61.5.1](../../../../dev2025/cloud-native-component-framework/docs/phase/phase-61.5.1.md),
  `P61.5.1-DP-02`.
- [SIE Phase 7](../../../textus-semantic-integration-engine/docs/phase/phase-7.md)
  and [Textus Knowledge Editor Phase 1](../../../textus-knowledge-editor/docs/phase/phase-1.md)
  are separate CAR-owned tracks.
