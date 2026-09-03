# Phase 8 Checklist: Configured KnowledgeSource Startup Bootstrap

This checklist is the authoritative Phase 8 state ledger. Only one Step may be
`IN_PROGRESS` at a time.

## P8-A: Contract and Supplier Handoffs

Status: PLANNED

- [ ] Record the accepted Cozy Phase 29 metadata-finalization artifact and
      prove its KnowledgeSource/Glossary handoff against the Textus BoK reader.
- [ ] Record the accepted CNCF Phase 70 post-assembly activation artifact and
      prove its ordering, readiness, failure, cleanup, and diagnostic contract.
- [ ] Freeze `textus.bok.bootstrap.profiles` grammar, duplicate/unknown/empty
      profile behavior, deterministic order, default-off compatibility, and
      private configuration provenance.
- [ ] Freeze once-per-server-assembly execution, `complete`-only readiness,
      failed/degraded startup rejection, and safe diagnostic fields.
- [ ] Register failing-first executable specifications for every Phase 8
      acceptance group before runtime implementation.

## P8-B: Bootstrap Publication

Status: PLANNED

- [ ] Decode bootstrap profile selectors through the existing typed
      configuration and profile-registry boundary.
- [ ] Activate only after the CNCF Phase 70 component/runtime-service handoff
      and before server readiness.
- [ ] Extract/reuse one publication service shared by bootstrap and the
      protected `replaceKnowledgeSource` Operation.
- [ ] Publish configured profiles in deterministic order and require a
      `complete` result for each required profile.
- [ ] Preserve atomic later-generation replacement, catalog visibility,
      profile isolation, generation/evidence attribution, and rollback safety.
- [ ] Prove default-off, once-only startup, unchanged-input restart,
      multi-profile ordering, invalid profile, missing/invalid metadata,
      absent SIE, degraded/failed publication, and safe diagnostics.
- [ ] Preserve protected manual replacement and permanent MCP exclusion of
      mutation/administration Operations.

## P8-C: Managed Runtime Acceptance and Closure

Status: PLANNED

- [ ] Configure the SimpleModeling.org development binding and explicit
      `development` bootstrap selector without adding another resource locator.
- [ ] Generate the accepted `website.d` through the Cozy Phase 29 WIP path and
      start Textus BoK through the normal Textus/control-center lifecycle.
- [ ] Verify readiness only after bootstrap reports `complete`, then retrieve
      attributable Glossary terms through MCP with `profile=development`.
- [ ] Add one term, rebuild metadata, restart the managed instance, and prove
      the new complete generation replaces the prior in-memory state.
- [ ] Prove malformed or incompatible metadata prevents readiness and produces
      safe actionable control-center diagnostics.
- [ ] Run focused and full Textus/CNCF regression, `cozyBuildCAR`, normal CAR
      lint, managed runtime acceptance, documentation convergence, independent
      review, bounded repair/re-review when needed, and the Phase release
      commit.

Phase 8 closes only when all Steps are DONE and exact supplier, runtime,
validation, review, and commit evidence is recorded in `phase-8.md`.

