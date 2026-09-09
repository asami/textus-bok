# Phase 9: Knowledge Map MCP Publication

status=in_progress
planned_at=2026-09-09
predecessor=[Phase 8](phase-8.md)
parallel_to=[Phase 7.5](phase-7.5.md)
strategy=[Textus BoK Development Strategy](../strategy/textus-bok-development-strategy.md)
checklist=[Phase 9 Checklist](phase-9-checklist.md)

## Stage Status

- Current status: IN PROGRESS — P9-A/P9-B accepted; P9-C managed Control
  Center proof and Phase closure remain pending.
- Owner: Textus BoK maintainers
- Priority: NEXT EXECUTABLE PHASE
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

## Completion Conditions

Phase 9 closes when MCP discovery exposes exactly the accepted ten read-only
operations, a managed MCP client can retrieve a bounded attributable Knowledge
Map, mutation/administration remains unavailable, documentation matches the
runtime surface, and all checklist evidence is accepted.
