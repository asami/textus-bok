# Phase 4 Checklist: BoK MCP Migration

- [x] `searchTerms` is MCP ready with typed query/category/limit input and an
  evidence-bearing `SearchTermsResponse` contract.
- [x] `explainTerm` is MCP ready with typed term input and an evidence-bearing
  `ExplainTermResponse` contract.
- [x] `searchComponentReferences` and `getComponentReference` are MCP ready
  with existence-only results.
- [x] `replaceKnowledgeSource` and every mutation/administration operation are
  proven unavailable through MCP after CAR/SAR policy application.
- [x] A migration SAR proves old/new operation parity with deterministic,
  collision-free tool identities.
- [x] Released ABI baseline and non-SNAPSHOT build-plugin verification are
  deferred as a (Future Development Candidate: DP-01) to
  `docs/phase/deferred-release-work.md`.

CBD handoff remains Phase 5 scope and is not claimed by this migration parity
ledger.

Phase 4 closes only when every item is checked and verification evidence is
recorded in `phase-4.md`.
