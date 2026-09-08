# Phase 8: Configured KnowledgeSource Startup Bootstrap

Stage Status:
- Current status: IN_PROGRESS
- Accepted step: P8-A Contract and Supplier Handoffs
- Next step: P8-B Bootstrap Publication (PLANNED, not started)
- Owner: Textus BoK development
- Update rule: Update this block and `phase-8-checklist.md` only from accepted
  executable, runtime, review, or dependency-handoff evidence.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: conservative upper bound <= 6h
- estimated_at_recommended_effort: 4–6h after supplier handoffs
- recommended_minimum_effort: high
- runtime_suitability: re-evaluate in the Phase execution task
- source: SimpleModeling.org local Glossary operation follow-up

## Objective

Allow an explicitly configured Textus BoK profile to publish its complete
KnowledgeSource generation during managed server startup. A control-center
restart must be sufficient to reload an updated SimpleModeling.org Glossary
from the profile registry without an external administrative HTTP call.

The intended private configuration shape is:

```yaml
textus.bok.bootstrap:
  profiles:
    - development
```

Profile source identity, dataset identity, generation, resource location, and
evidence continue to come exclusively from the existing private profile
registry. Bootstrap configuration selects registered logical profiles; it
never accepts another filesystem path, URL, provider, credential, or request
body.

## Required Inputs and Dependencies

- Cozy Phase 29 must supply and validate the project-owned-site
  `cncf.knowledge-source.v1` handoff consumed by the selected profile.
- CNCF Phase 70 must supply a post-assembly component activation boundary that
  runs after component/runtime-service binding and before server readiness.
- Closed Textus BoK Phases 7--7.4 supply profile registration, exact selection,
  complete-generation publication, attribution, REST/Web/MCP isolation, and
  representative control-center-compatible runtime behavior.
- `replaceKnowledgeSource` remains the single semantic publication authority;
  bootstrap reuses its internal normalization/federation/catalog commit path.

## P8-A Acceptance Record

P8-A1 records the frozen supplier identities consumed by the contract boundary:

- Cozy Phase 29 release `238a078` (`cozy/docs/phase/phase-29.md`) supplies the
  project-owned generated-site `cncf.knowledge-source.v1` metadata-finalization
  handoff. It does not activate or publish a BoK generation here.
- CNCF Phase 70 release `a7ce3822` (`cloud-native-component-framework/docs/phase/phase-70.md`)
  and `docs/spec/component-activation-lifecycle.md` R9 supply the
  consumer-neutral post-assembly Server activation seam. R9 leaves profile
  selection, source loading, publication, and consumer diagnostics to Textus
  BoK.

The private bootstrap grammar and the pending acceptance-spec registration are
recorded in `docs/spec/bok-startup-bootstrap-contract.md` and
`src/test/scala/org/simplemodeling/textus/bok/BokStartupBootstrapSpec.scala`.
The Cozy handoff and current reader agree on schema
`cncf.knowledge-source.v1` and the `glossary-terms` resource at
`metadata/glossary/terms.json`; the reader proof ran
`BokSourceReaderSpec` in serialized SBT invocation
`64903-20260908T070229Z` (attempt
`03fb113cfa4765f5cfd8238eac552bebc088132a2367ab20bfdb8687b1904b9f`):
10 tests succeeded, 0 failed, with `lock=released`.

CNCF Phase 70 R3, R4, R7, and R9 establish the required post-assembly
placement, deterministic once-only order, readiness/failure/cleanup/safe
diagnostic behavior, and the Textus BoK ownership boundary. The P8-A focused
implementation proof ran in serialized SBT invocation
`50359-20260908T064736Z` (attempt
`e1f71a8846dc888bd5027c1ffec605b817ddf7675282a9a23a0b7e09bf05307c`):
5 tests succeeded, 0 failed, 8 pending, with `lock=released`.

The initial Step review is recorded by Review Disposition Bundle
`f189f3a89e5bf8a288d44dbcdfe25394c30260e02cc4e19eb93cba862112de17`.
Its blocker `CPB-P8-001` was repaired and closed by the focused re-review
bundle
`ec0331e81be14e7eac8b12333e539a7f3dd0109368ff474c824ead285c798380`,
which reports no remaining blocker, Hygiene item, or Development Candidate.

P8-A is accepted as the configuration/contract and supplier-handoff Step. It
does not claim production startup activation or publication. Acceptance using
an actual SimpleModeling.org managed `website.d`, control-center restart, and
MCP term retrieval remains reserved to P8-C.

## Scope and invariants

- Bootstrap is disabled by default and activates only explicitly listed,
  already registered profiles.
- Resolve profile identities through `BokProfileRegistry`; never infer from the
  current directory, host, control-center record, request, or generated site.
- Execute once per managed server assembly after SIE and all participating
  components are available and before readiness is reported.
- Publish selected profiles in deterministic configuration order and require
  every result to be `complete`. A failed or degraded required publication
  fails startup and keeps the server unavailable.
- Reuse the same `BokSourceReader`, `BokFederationPublisher`, atomic dataset
  replacement, and local catalog replacement used by the protected
  administrative Operation.
- Keep the protected `replaceKnowledgeSource` Operation available for later
  no-restart administration, but do not expose it through MCP.
- Preserve exact generation/evidence attribution. Restarting unchanged input
  must be deterministic and must not duplicate terms, component references,
  assertions, or vector documents.
- Support in-memory SIE as the primary SimpleModeling.org acceptance mode.
  Persistent providers must receive the same idempotent replacement semantics,
  not an append-on-start shortcut.
- Emit bounded startup diagnostics containing profile, dataset, source,
  generation, status, term/component/topology counts, warnings, and safe
  evidence. Never expose the private resource locator or credentials.
- A profile update remains an explicit build-then-restart operation. This Phase
  adds no file watcher, polling, hot reload, or control-center mutation API.

## Non-goals

- Generating or correcting SmartDox/Cozy metadata.
- Adding an arbitrary startup-Operation list to Textus BoK configuration.
- Making `replaceKnowledgeSource` MCP-ready or public.
- Automatically bootstrapping every registered official, development, or
  project profile.
- Watching `website.d`, restarting the application, or controlling
  textus-control-center.
- Adding another publication implementation beside
  `replaceKnowledgeSource`.

## Steps

### P8-A: Contract and Supplier Handoffs

Freeze configuration grammar, default-off behavior, activation order,
readiness/failure semantics, diagnostics, and exact Cozy Phase 29/CNCF Phase 70
handoff identities. Register failing-first specifications without changing
runtime behavior.

### P8-B: Bootstrap Publication

Consume the CNCF activation hook, resolve configured profiles, and publish each
generation through the shared replacement service. Preserve protected manual
replacement, profile isolation, complete-generation atomicity, and existing
read semantics.

### P8-C: Managed Runtime Acceptance and Closure

Use SimpleModeling.org `website.d` produced through the Cozy Phase 29 contract
and start Textus BoK through the normal Textus/control-center development
runtime. Prove automatic Glossary availability through `profile=development`,
updated content after restart, failed startup for invalid metadata, full
regression, CAR validation, documentation, independent review, and release
closure.

## Validation boundary

- Configuration decoder and registry/bootstrap selection specifications.
- Component activation ordering, once-only behavior, disabled compatibility,
  complete/degraded/failed publication, deterministic restart, and safe
  diagnostics specifications.
- Existing profile registry, source reader, federation publication, catalog,
  REST, Web, MCP, and representative SAR regression.
- CNCF Phase 70 focused and full supplier evidence.
- Cozy Phase 29 SimpleModeling.org WIP metadata evidence.
- One managed runtime acceptance showing a newly generated term is unavailable
  before restart and attributable through `profile=development` after restart.
- Full Textus BoK tests, `cozyBuildCAR`, normal CAR lint, independent review,
  repair/re-review when required, and a Phase release commit.

## Completion conditions

Phase 8 closes only when every item in `phase-8-checklist.md` is checked, the
exact Cozy Phase 29 and CNCF Phase 70 supplier identities are recorded, managed
restart acceptance passes, all existing manual/profile/read contracts regress
cleanly, documentation and review converge, and the validated work is
committed.

## References

- `docs/phase/phase-8-checklist.md`
- `docs/phase/phase-7.4.md`
- `docs/design/bok-profile-registry.md`
- `docs/spec/bok-profile-registry-contract.md`
- `docs/spec/bok-startup-bootstrap-contract.md`
- `src/main/scala/org/simplemodeling/textus/bok/impl/ComponentFactory.scala`
- `src/main/scala/org/simplemodeling/textus/bok/runtime/BokProfileRegistry.scala`
- `src/main/scala/org/simplemodeling/textus/bok/runtime/BokFederationPublisher.scala`
- `src/main/scala/org/simplemodeling/textus/bok/runtime/BokStartupBootstrap.scala`
- `src/test/scala/org/simplemodeling/textus/bok/BokStartupBootstrapSpec.scala`
- `cozy:docs/phase/phase-29.md`
- `cloud-native-component-framework:docs/phase/phase-70.md`
