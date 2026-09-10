# Phase 10.1 Checklist: Catalog-Backed BoK Reads and MCP Two-Source Acceptance

This checklist is the authoritative Phase 10.1 state ledger. The summary and
current step are recorded in `phase-10.1.md`.

This Phase was split from the original open Phase 10 on 2026-09-10 and
exclusively owns pre-split P10-03 and P10-04. Phase 10 retains P10-01/P10-02.

## P10-03: Catalog-Backed BoK Reads and MCP Publication

- [ ] Existing BoK-owned read operations use the resident catalog as their read
  source for the in-memory runtime path.
- [ ] Multi-source candidate/search results preserve distinct source
  attribution.
- [ ] Existing exact/ambiguous/conflict/insufficient-evidence/no-match behavior
  remains deterministic where applicable.
- [ ] Existing MCP-ready BoK operations expose the same resident-catalog read
  semantics through CNCF MCP projection.
- [ ] Source mutation, registration, replacement, and reload administration
  remain non-MCP-ready.
- [ ] No duplicate BoK-specific MCP server path is introduced when CNCF
  projection is sufficient.
- [ ] Focused REST/operation/MCP regression specifications pass.

## P10-04: SimpleModeling.org + bok-knowledgehub End-to-End Acceptance

- [ ] SimpleModeling.org or `simplemodeling-org` glossary content is admitted
  into the running in-memory catalog through the supported BoK source path.
- [ ] `bok-knowledgehub` glossary content is admitted into the same running
  in-memory catalog through the supported BoK source path.
- [ ] A known SimpleModeling.org term is retrievable with correct attribution.
- [ ] A known `bok-knowledgehub` term is retrievable with correct attribution.
- [ ] One representative query can return attributable results from both
  sources in the same response/catalog view.
- [ ] Replacing or reloading one source leaves the other source's entries
  intact.
- [ ] The representative catalog read succeeds through MCP `tools/call`.
- [ ] The representative runtime has no installed or required SIE component.
- [ ] `sbt --batch test cozyBuildCAR` passes on the accepted Phase 10.1 tree.
- [ ] Normal repository CAR validation/lint passes or any unrelated residual is
  explicitly separated as non-Phase-10.1 hygiene.
- [ ] Independent final review confirms that Phase 7 profile resolution,
  attribution, mutation boundaries, and component isolation remain intact.

Phase 10.1 closes only while every checklist item remains checked and final
validation and representative runtime evidence are recorded in `phase-10.1.md`.
