# Phase 10.1 Checklist: Catalog-Backed BoK Reads and MCP Two-Source Acceptance

This checklist is the authoritative Phase 10.1 state ledger. The summary and
current step are recorded in `phase-10.1.md`.

This Phase was split from the original open Phase 10 on 2026-09-10 and
exclusively owns pre-split P10-03 and P10-04. Phase 10 retains P10-01/P10-02.

## P10-03: Catalog-Backed BoK Reads and MCP Publication

- [x] Existing BoK-owned read operations use the resident catalog as their read
  source for the in-memory runtime path.
- [x] Multi-source candidate/search results preserve distinct source
  attribution.
- [x] Existing exact/ambiguous/conflict/insufficient-evidence/no-match behavior
  remains deterministic where applicable.
- [x] Existing MCP-ready BoK operations expose the same resident-catalog read
  semantics through CNCF MCP projection.
- [x] Source mutation, registration, replacement, and reload administration
  remain non-MCP-ready.
- [x] No duplicate BoK-specific MCP server path is introduced when CNCF
  projection is sufficient.
- [x] Focused REST/operation/MCP regression specifications pass.

## P10-04: SimpleModeling.org + bok-knowledgehub End-to-End Acceptance

- [x] SimpleModeling.org or `simplemodeling-org` glossary content is admitted
  into the running in-memory catalog through the supported BoK source path.
- [x] `bok-knowledgehub` glossary content is admitted into the same running
  in-memory catalog through the supported BoK source path.
- [x] A known SimpleModeling.org term is retrievable with correct attribution.
- [x] A known `bok-knowledgehub` term is retrievable with correct attribution.
- [x] One representative query can return attributable results from both
  sources in the same response/catalog view.
- [x] Replacing or reloading one source leaves the other source's entries
  intact.
- [x] The representative catalog read succeeds through MCP `tools/call`.
- [x] The representative runtime has BoK selecting `memory` and neither
  selecting nor requiring an SIE knowledge backend; its acceptance SAR includes
  SIE, BoK, and Scraper as sibling CARs, with SIE non-MCP and Scraper closing
  SIE's required dependency.
- [x] `sbt --batch test cozyBuildCAR` passes on the accepted Phase 10.1 tree.
- [x] Normal repository CAR validation/lint passes or any unrelated residual is
  explicitly separated as non-Phase-10.1 hygiene.
- [x] Independent final review confirms that Phase 7 profile resolution,
  attribution, mutation boundaries, and component isolation remain intact.

Phase 10.1 closes only while every checklist item remains checked and final
validation and representative runtime evidence are recorded in `phase-10.1.md`.
