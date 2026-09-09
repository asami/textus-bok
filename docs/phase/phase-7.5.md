# Phase 7.5: Component-Contributed Knowledge Acceptance

status=planned
planned_at=2026-09-09
predecessor=[Phase 7.4](phase-7.4.md)
depends_on=[CNCF Phase 73](../../../../dev2025/cloud-native-component-framework/docs/phase/phase-73.md), [Textus Knowledge Editor Phase 2](../../../textus-knowledge-editor/docs/phase/phase-2.md)
parallel_to=[Phase 9](phase-9.md)
strategy=[Textus BoK Development Strategy](../strategy/textus-bok-development-strategy.md)
checklist=[Phase 7.5 Checklist](phase-7.5-checklist.md)
consumes_handoff=released CNCF knowledge hub contract and released TKE textus.book contribution

## Stage Status

- Current status: PLANNED
- Owner: Textus BoK maintainers
- Update rule: Update this Phase and its checklist only from accepted profile
  fixtures, executable specifications, review evidence, or an explicit scope
  decision.
- Entry rule: Phase 7.4 remains closed, and CNCF Phase 73 and Textus Knowledge
  Editor Phase 2 must both be CLOSED with released handoffs. Phase 9 may
  proceed independently and does not satisfy either dependency.

## Goal

Consume component-contributed knowledge through the released CNCF hub without
direct provider wiring or BoK ownership of provider-domain semantics. Verify
the first representative `textus.book` contribution from TKE preserves stable
domain identity, selected-profile attribution, isolation, and evidence through
BoK's established read surfaces.

## Scope

- Freeze the generic BoK consumer boundary for producer-declared, attributable,
  complete knowledge snapshots delivered through CNCF.
- Consume `knowledgeDomainId=textus.book` independently of the current
  `providerComponent=textus-knowledge-editor` identity.
- Use TKE's accepted Book, Textual Work, Textual Edition, optional Textual
  Volume, Series, and Book-context role fixture without redefining those
  semantics in Textus BoK.
- Verify profile selection, source/generation attribution, isolation, and
  failure behavior through the established BoK read contract.
- Verify a complete contribution replaces its prior accepted generation and a
  partial, incompatible, unavailable, or foreign-domain contribution fails
  deterministically without corrupting the accepted view.
- Record end-to-end TKE -> CNCF -> Textus BoK acceptance without adding a new
  public operation merely for verification.

## Non-Goals

- Packaged multi-CAR runtime composition, launcher ownership, or cross-CAR
  Help/API acceptance; SIE Phase 7 owns that bounded runtime work.
- Knowledge Editor list/detail/edit/lifecycle, Tag filtering, or local
  Knowledge materialization; Textus Knowledge Editor Phase 1 owns that work.
- CNCF hub mechanics or TKE provider implementation; the supplier Phases own
  those contracts and their release evidence.
- Paper, generic WebResource, or unrelated Person/Organization knowledge;
  these wait for separately owned domain providers.
- Knowledge Map MCP publication; Phase 9 owns that independent priority.
- A Book editor or permanent Book-domain model owned by Textus BoK.

## Work Groups

### P7.5-A: Fixture and Contract Freeze

Freeze the generic consumer contract and the smallest accepted `textus.book`
fixture from the released TKE handoff. Reuse the Phase 7--7.4 profile contract
and do not reinterpret provider-owned Book semantics.

### P7.5-B: Representative Profile Acceptance

Verify the TKE -> CNCF -> Textus BoK path through BoK public reads and relevant
REST/Web/MCP projections. Confirm positive, replacement, negative, isolation,
provider-substitution, and evidence behavior without direct provider wiring.

### P7.5-C: Closure

Run proportionate BoK executable specifications and normal CAR structural
validation, reconcile documentation, complete independent review, and commit
the accepted Phase result.

## Completion Conditions

Phase 7.5 closes only when every checklist item is checked and the accepted
`textus.book` contribution demonstrates generic consumer, stable-domain,
read, attribution, replacement, isolation, and failure contracts without
making Textus BoK the owner of Book semantics.

## Handoff Source

- [CNCF Phase 73](../../../../dev2025/cloud-native-component-framework/docs/phase/phase-73.md)
  supplies the released domain-neutral hub contract.
- [Textus Knowledge Editor Phase 2](../../../textus-knowledge-editor/docs/phase/phase-2.md)
  supplies the released `textus.book` provider handoff.
- [Textus Knowledge Editor Phase 1](../../../textus-knowledge-editor/docs/phase/phase-1.md)
  and [SIE Phase 7](../../../textus-semantic-integration-engine/docs/phase/phase-7.md)
  remain separate tracks.
