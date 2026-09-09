# Phase 10 Checklist: Resident Multi-BoK Catalog and MCP Publication

This checklist is the authoritative Phase 10 state ledger. The summary and
current step are recorded in `phase-10.md`.

## P10-01: SIE-Independent In-Memory Knowledge Backend

- [ ] A BoK-owned backend/store boundary is defined for catalog reads.
- [ ] An in-memory implementation can admit and retain one complete generation
  without an installed SIE component.
- [ ] Source/dataset/generation/evidence attribution is preserved in memory.
- [ ] Complete-generation replacement removes stale records only for the
  replaced source/generation ownership boundary.
- [ ] Existing SIE integration remains available behind the retained public
  component/provider boundary.
- [ ] Explicit SIE selection fails deterministically when SIE is unavailable;
  it does not silently fall back to memory.
- [ ] Focused executable specifications prove startup, admission, replacement,
  read behavior, and SIE-absent operation.

## P10-02: Resident Multi-BoK Catalog

- [ ] More than one independently resolved selected generation can coexist in
  one resident catalog.
- [ ] Existing profile resolution happens before catalog admission.
- [ ] Every catalog entry preserves profile/source/dataset/generation/evidence
  attribution.
- [ ] Catalog identity and result ordering are deterministic.
- [ ] Equal term names from different sources do not overwrite one another.
- [ ] Source-scoped replacement/reload leaves unrelated source generations
  intact.
- [ ] Focused executable specifications prove multi-source admission,
  attribution, duplicate-name coexistence, isolation, and replacement.

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
- [ ] `sbt --batch test cozyBuildCAR` passes on the accepted Phase 10 tree.
- [ ] Normal repository CAR validation/lint passes or any unrelated residual is
  explicitly separated as non-Phase-10 hygiene.
- [ ] Independent final review confirms that Phase 7 profile resolution,
  attribution, mutation boundaries, and component isolation remain intact.

Phase 10 closes only while every checklist item remains checked and the final
validation and representative runtime evidence are recorded in `phase-10.md`.
