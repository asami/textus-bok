# Phase 7.2 Checklist: Read Contract Integration

This checklist is the authoritative Phase 7.2 state ledger. It was split from
the original open Phase 7 on 2026-08-14 and exclusively owns pre-split P7-03.
The summary and current step are recorded in `phase-7.2.md`.

## P7-03: Read Contract Integration

- [ ] CML defines one shared optional profile selector and resolved-selection
  response model for term, component-reference, and Knowledge Map reads.
- [ ] Exact catalog reads and provider-backed SIE candidates are constrained to
  the resolved profile before match classification and result limits apply.
- [ ] Existing Knowledge Map `datasetId`/`sourceId` filters can only narrow the
  resolved profile and cannot escape it; incompatible combinations fail
  structurally.
- [ ] MCP projection specifications expose the same optional selection fields
  for the four existing read tools while source registration/replacement stays
  absent.
- [ ] Compatibility specifications prove the intentional omitted-selector
  migration to deterministic `official` reads and that every result reports
  its resolved profile/source/dataset/generation attribution.

Phase 7.2 closes only while every P7-03 item remains checked and validation
evidence remains recorded in `phase-7.2.md`.
