# Phase 9 Checklist: Knowledge Map MCP Publication

status=closed
phase=[Phase 9](phase-9.md)
predecessor=[Phase 8 Checklist](phase-8-checklist.md)
parallel_to=[Phase 7.5 Checklist](phase-7.5-checklist.md)

## Stage Status

- Current status: CLOSED
- Owner: Textus BoK maintainers
- Next step: None. Phase 9 is closed; no successor Phase is started by this
  closure.
- Update rule: Check an item only from the stated executable, managed-runtime,
  review, or release evidence.

## P9-A: MCP Contract and Readiness Freeze

- [x] Inventory and verify the nine currently MCP-ready read operations.
- [x] Reconcile README, developer/operator guidance, generated Help, and
      discovery expectations that still describe only four reads.
- [x] Freeze the bounded `getKnowledgeMap` MCP input/output, profile, filter,
      limit, attribution, evidence, and failure contract.
- [x] Register failing-first discovery, schema-parity, profile, policy, and
      mutation-exclusion specifications.

## P9-B: Knowledge Map MCP Admission

- [x] Admit only `BokRetrieval.getKnowledgeMap` as the tenth MCP-ready read.
- [x] Preserve the established public operation semantics and structured
      profile-unavailable behavior.
- [x] Verify limits and filters prevent an unbounded graph export.
- [x] Verify `replaceKnowledgeSource` and all mutation/administration
      operations remain absent and non-invocable through MCP.

## P9-C: Managed MCP Acceptance and Closure

- [x] Verify MCP discovery and generated schema expose exactly the accepted
      ten read-only operations.
- [x] Verify positive, negative, profile-isolation, attribution, and bounded
      Knowledge Map reads with a real MCP client.
- [x] Verify the Control Center-managed Textus BoK instance on port 18014 can
      serve the accepted discovery and map-read sequence.
- [x] Run proportionate BoK and CNCF regression, normal CAR validation,
      documentation convergence, independent review, and the Phase release
      commit.

Profile-isolation evidence: the accepted four-profile SAR is recorded in
`phase-9.md`. It passed exact ten-read discovery, four REST/MCP bounded-map
reads, four cross-profile negative map reads, and script-owned lifecycle
cleanup; the separately managed Control Center probe remains the port-18014
development-profile proof.

Phase 9 is CLOSED. P9-A/P9-B admit only the existing bounded
`getKnowledgeMap` read as MCP's tenth read-only operation; P9-C records the
managed 18014 probe, four-profile isolation SAR, clean focused re-review, and
final serialized full-suite evidence. The release binding persists the two
Hygiene and two Development Candidate records without starting Phase 7.5 or
another successor.
