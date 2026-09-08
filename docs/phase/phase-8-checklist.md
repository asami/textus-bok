# Phase 8 Checklist: Configured KnowledgeSource Startup Bootstrap

This checklist is the authoritative Phase 8 state ledger. Only one Step may be
`IN_PROGRESS` at a time.

## P8-A: Contract and Supplier Handoffs

Status: DONE

- [x] Record the accepted Cozy Phase 29 metadata-finalization artifact and
      prove its KnowledgeSource/Glossary handoff against the Textus BoK reader.
- [x] Record the accepted CNCF Phase 70 post-assembly activation artifact and
      prove its ordering, readiness, failure, cleanup, and diagnostic contract.
- [x] Freeze `textus.bok.bootstrap.profiles` grammar, duplicate/unknown/empty
      profile behavior, deterministic order, default-off compatibility, and
      private configuration provenance.
- [x] Freeze once-per-server-assembly execution, `complete`-only readiness,
      failed/degraded startup rejection, and safe diagnostic fields.
- [x] Register failing-first executable specifications for every Phase 8
      acceptance group before runtime implementation.

P8-A records Cozy Phase 29 release
`238a078869b2d5ceffe0bf3677355b7e9bff3c83` and CNCF Phase 70 release
`a7ce382245e20d52b517b323200805927e8045c0`. The reader and focused P8-A
proofs, initial review, blocker repair, and clean focused re-review are bound
in `phase-8.md`. P8-A acceptance covers the private configuration/contract and
supplier handoff only. Production startup publication begins in P8-B; actual
SimpleModeling.org managed-runtime acceptance remains in P8-C.

## P8-B: Bootstrap Publication

Status: DONE

- [x] Decode bootstrap profile selectors through the existing typed
      configuration and profile-registry boundary.
- [x] Activate only after the CNCF Phase 70 component/runtime-service handoff
      and before server readiness.
- [x] Extract/reuse one publication service shared by bootstrap and the
      protected `replaceKnowledgeSource` Operation.
- [x] Publish configured profiles in deterministic order and require a
      `complete` result for each required profile.
- [x] Preserve atomic later-generation replacement, catalog visibility,
      profile isolation, generation/evidence attribution, and rollback safety.
- [x] Prove default-off, once-only startup, unchanged-input restart,
      multi-profile ordering, invalid profile, missing/invalid metadata,
      absent SIE, degraded/failed publication, and safe diagnostics.
- [x] Preserve protected manual replacement and permanent MCP exclusion of
      mutation/administration Operations.

P8-B acceptance is commit
`4893adc2c1aea752b6352f70f856c3b2c27b284e`; its final focused proof reported
25 succeeded, 0 failed, and 1 then-pending P8-C managed scenario with
`lock=released`.

## P8-C: Managed Runtime Acceptance and Closure

Status: DONE

- [x] Configure the SimpleModeling.org development binding and explicit
      `development` bootstrap selector without adding another resource locator.
- [x] Generate the accepted `website.d` through the Cozy Phase 29 WIP path and
      start Textus BoK through the normal Textus/control-center lifecycle.
- [x] Verify readiness only after bootstrap reports `complete`, then retrieve
      attributable Glossary terms through MCP with `profile=development`.
- [x] Compare accepted baseline and current metadata, restart the managed
      instance, and prove
      the new complete generation replaces the prior in-memory state.
- [x] Prove malformed or incompatible metadata prevents readiness and produces
      safe actionable control-center diagnostics.
- [x] Run focused and full Textus/CNCF regression, `cozyBuildCAR`, normal CAR
      lint, managed runtime acceptance, documentation convergence, independent
      review, bounded repair/re-review when needed, and the Phase release
      commit.

P8-C acceptance is commit
`21362f11ec32e9808a42a710888b7d194abb15f5`. The managed baseline/current
transition, malformed-metadata rejection, restoration, full-review PASS, final
release validation, and distinct Phase release boundary are recorded in
`phase-8.md`.

Acceptance clarification (2026-09-09): the earlier draft's instruction to edit
the live producer by adding one term was superseded by the accepted read-only
comparison of the generated 1.0.56 baseline and the clean current publication.
Their different term identities, counts, and generations prove the same
restart/replacement behavior without mutating SimpleModeling.org.

Phase 8 is CLOSED. All Steps are DONE; no successor Phase is started by this
ledger transition.
