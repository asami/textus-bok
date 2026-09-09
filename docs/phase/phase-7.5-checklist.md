# Phase 7.5 Checklist: Component-Contributed Knowledge Acceptance

status=planned
phase=[Phase 7.5](phase-7.5.md)
predecessor=[Phase 7.4 Checklist](phase-7.4-checklist.md)
depends_on=[CNCF Phase 73 Checklist](../../../../dev2025/cloud-native-component-framework/docs/phase/phase-73-checklist.md), [Textus Knowledge Editor Phase 2 Checklist](../../../textus-knowledge-editor/docs/phase/phase-2-checklist.md)
parallel_to=[Phase 9 Checklist](phase-9-checklist.md)

## Stage Status

- Current status: PLANNED
- Owner: Textus BoK maintainers
- Entry rule: CNCF Phase 73 and Textus Knowledge Editor Phase 2 are both
  CLOSED with released handoffs.
- Update rule: Check an item only from the stated executable or review evidence.

## P7.5-A: Fixture and Contract Freeze

- [ ] Freeze the generic consumer contract for producer-declared complete
      knowledge snapshots delivered through CNCF.
- [ ] Import the accepted TKE `textus.book` fixture without redefining its
      Book-domain semantics in Textus BoK.
- [ ] Bind stable `knowledgeDomainId=textus.book` independently of
      `providerComponent=textus-knowledge-editor`.
- [ ] Bind the fixture to existing profile-selection, source/generation,
      attribution, replacement, and isolation contracts.
- [ ] Define expected positive, negative, and failure observations without
      adding a verification-only public operation.

## P7.5-B: Representative Profile Acceptance

- [ ] Verify the TKE -> CNCF -> Textus BoK contribution through the BoK public
      read contract without direct provider implementation wiring.
- [ ] Verify complete-generation replacement, source/generation attribution,
      and cross-profile/cross-domain isolation.
- [ ] Verify a substituted provider can retain `knowledgeDomainId=textus.book`.
- [ ] Verify partial, incompatible, unavailable, and foreign-domain
      contributions fail without corrupting the accepted view.
- [ ] Verify relevant REST, Web, and MCP projections retain the same selected
      profile and evidence semantics.
- [ ] Record supplier failures without taking ownership of CNCF hub mechanics,
      TKE Book semantics, or SIE packaged-runtime composition.

## P7.5-C: Closure

- [ ] Run the proportionate BoK executable specifications and normal CAR
  structural validation.
- [ ] Reconcile Phase, checklist, consumer documentation, and accepted evidence.
- [ ] Complete independent review, any required bounded repair/re-review, and
  the Phase release commit.
