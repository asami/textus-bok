# Phase 4 Checklist: BoK MCP Migration

- [x] `searchTerms` is MCP ready with typed query/category/limit input and an
  evidence-bearing `SearchTermsResponse` contract.
- [x] `explainTerm` is MCP ready with typed term input and an evidence-bearing
  `ExplainTermResponse` contract.
- [x] `searchComponentReferences` and `getComponentReference` are MCP ready
  with existence-only results.
- [x] `replaceKnowledgeSource` and every mutation/administration operation are
  proven unavailable through MCP after CAR/SAR policy application.
- [ ] A migration SAR proves old/new operation parity and CBD handoff.

Phase 4 closes only when every item is checked and verification evidence is
recorded in `phase-4.md`.
