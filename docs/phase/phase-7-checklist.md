# Phase 7 Checklist: BoK Profile Selection Contract

This checklist is the authoritative Phase 7 state ledger. The summary and
current step are recorded in `phase-7.md`.

The original open Phase 7 was split on 2026-08-14. This ledger retains only
P7-01. P7-02 through P7-05 moved exactly once to Phases 7.1 through 7.4.

## P7-01: Selection Contract

- [x] Stable design and specification documents define `official`,
  `development`, and `project` as closed profile kinds with distinct ownership
  and trust semantics.
- [x] The contract fixes `official` as the omitted-selector default, documents
  migration from the current all-selected-datasets behavior, requires explicit
  identity for `project`, and forbids ambient project discovery.
- [x] The contract defines how a profile resolves to source/dataset/generation,
  how resolved attribution appears in responses, and how existing
  `datasetId`/`sourceId` filters interact with the profile boundary.
- [x] Unregistered, unavailable, stale, ambiguous, unauthorized, and conflicting
  selections have structured outcomes with no fallback to another profile.

Phase 7 closes only while every P7-01 item remains checked and validation
evidence remains recorded in `phase-7.md`.
