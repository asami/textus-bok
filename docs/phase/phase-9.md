# Phase 9: Knowledge Map MCP Publication

status=closed
planned_at=2026-09-09
predecessor=[Phase 8](phase-8.md)
parallel_to=[Phase 7.5](phase-7.5.md)
strategy=[Textus BoK Development Strategy](../strategy/textus-bok-development-strategy.md)
checklist=[Phase 9 Checklist](phase-9-checklist.md)

## Stage Status

- Current status: CLOSED
- Owner: Textus BoK maintainers
- Next step: None. Phase 9 is closed; no successor Phase is started by this
  closure.
- Entry rule: Phases 6, 7.4, and 8 remain CLOSED. CNCF Phase 73, TKE Phase 2,
  and BoK Phase 7.5 are not dependencies.
- Update rule: Update this Phase and its checklist only from accepted MCP
  contract, executable, managed-runtime, review, or release evidence.

## Goal

Publish the existing bounded, read-only `getKnowledgeMap` operation through
CNCF MCP so an MCP consumer can retrieve attributable BoK topology while the
existing profile contract and deny-by-default administration boundary remain
unchanged.

## Starting Point

Textus BoK already marks nine terminology, component-reference, and semantic
knowledge reads as MCP-ready. Phase 6 established the bounded public
`getKnowledgeMap` operation but intentionally kept it outside MCP. Phase 8
verified managed Control Center MCP terminology retrieval on port 18014. Some
public documentation still describes only the original four MCP reads and must
be reconciled with the implementation before adding the tenth read.

## Scope

- Freeze the bounded MCP input/output projection for `getKnowledgeMap` using
  its existing public operation semantics.
- Preserve explicit profile selection, filters, limits, generation/source
  attribution, evidence, and structured unavailable/failure behavior.
- Admit only `BokRetrieval.getKnowledgeMap` as the new MCP-ready operation.
- Keep discovery, generated schemas, Help, manuals, and the actual MCP-ready
  operation set synchronized.
- Verify default-deny policy and permanent MCP exclusion of
  `replaceKnowledgeSource` and every mutation/administration operation.
- Perform representative local and Control Center-managed MCP acceptance,
  including discovery and a bounded map read on the configured port 18014.

## Non-Goals

- CNCF Phase 73 knowledge contribution or TKE `textus.book` consumption.
- A new Knowledge Map domain model, graph query language, mutation API, or Web
  redesign.
- Making `replaceKnowledgeSource` or any administration operation MCP-ready.
- Unbounded graph export, streaming, subscriptions, watchers, or hot reload.
- Repeating the already accepted four-profile selection implementation.

## Work Groups

### P9-A: MCP Contract and Readiness Freeze

Freeze the existing bounded operation's MCP projection, reconcile the nine-read
starting surface, and register failing-first discovery, schema, policy, and
profile-attribution specifications.

### P9-B: Knowledge Map MCP Admission

Mark only `BokRetrieval.getKnowledgeMap` MCP-ready and implement any minimal
projection work required for parity with the existing public read.

### P9-C: Managed MCP Acceptance and Closure

Verify discovery, bounded reads, profiles, attribution, structured failures,
mutation exclusion, Control Center operation on port 18014, documentation,
review, validation, and release closure.

#### Managed Acceptance Record

On 2026-09-09, the ordinary local Textus Control Center recognized
`textus-bok` as the available `DEV:dev2026` source and started the managed
instance. The selected instance became ready on loopback port `18014` with
`/rest/v1/admin/system/status` reporting `UP`.

The committed `scripts/test/probe-managed-bok.py` probe
(`23a4286423d5954a05c03e7452d7fd2d2f287ff11b1e1b4644e0769f32b72342`)
then passed against that actual managed runtime and current
SimpleModeling.org source generation:

```text
MANAGED_BOK_PROBE_OK profile=development \
generation=sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f \
term_count=212 present=1 absent=0 maps=2 mutation_tools=0
```

The probe establishes exact ten-read discovery, profile attribution, one
positive terminology read, one absent-term read, bounded matching and
no-match Knowledge Map reads, and permanent mutation exclusion. It performs
no replacement or other administration operation.

The separate four-profile representative SAR then completed on 2026-09-09
against its isolated loopback runtime on port `19547`:

```text
BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 web_maps=4 \
mcp_terms=4 mcp_maps=4 negative_rest_terms=4 negative_mcp_terms=4 \
negative_rest_maps=4 negative_mcp_maps=4 negative_web_maps=4 \
mcp_failures=2 web_failures=2
BOK_PROFILE_SELECTION_SAR_LIFECYCLE_OK profiles=4
```

This complementary acceptance proves REST/MCP bounded-map parity and
foreign-profile topology non-leakage for `official`, `development`, and both
project identities. The SAR owned and removed its temporary runtime; it did
not replace a source or perform an administration operation. Together with
the Control Center proof above, it supplies the complete P9-C profile and
managed-runtime acceptance boundary.

## Review and Release Closure

The single Phase 9 full review covered the complete
`90c279c7fdd7d994c700a22c805d9b0cb7a1a239..c899cbe7f2ec4a9b99678e23331f979633084643`
accumulator. Its typed disposition is
`a0a7e95f8e525512f5941a556fbfb59183cd318f07761d3f99388172f33cb168`.
It identified only `CB-P9-001`: four-profile MCP isolation evidence had been
executed but not yet recorded. The existing SAR was rerun successfully and the
bounded documentation/evidence repair passed independent focused re-review in
typed disposition
`877b7a9b6ab13724bdcfb8c44f893c9b9fa0408696cc0342f8978efc48adbb24`.
No Current Boundary Blocker remains.

Final full-suite validation ran through the serialized SBT gate in invocation
`42048-20260909T053222Z`: 14 suites and 79 tests succeeded, with 0 failed or
aborted tests, SBT exit 0, wrapper exit 0, and the shared lock released. The
normal CAR validation and closure-ledger checks are bound to this final
documentation tree before the distinct local Phase release commit.

The Phase 9 release binding persists the accepted Hygiene and Development
Candidate records separately. They do not expand the bounded MCP publication
scope: manual-link packaging and ABI-baseline work remain Hygiene, while CML
schema modeling and the sbt-cozy lifecycle remain future development work.
The concurrent Phase 7.5 planning, private `.textus/config.yaml`, and
`.gitignore` remain outside the Phase 9 commit.

## Completion Conditions

Phase 9 closes when MCP discovery exposes exactly the accepted ten read-only
operations, a managed MCP client can retrieve a bounded attributable Knowledge
Map, mutation/administration remains unavailable, documentation matches the
runtime surface, and all checklist evidence is accepted.
