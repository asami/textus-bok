# Phase 3: Operational Documentation

## Stage Status

- Status: `COMPLETE`
- Current step: Phase 3 closure verified; begin read-only BoK MCP migration.
- Closure basis: Every Phase 3 checklist item is complete and executable/static
  verification evidence is recorded below.

## Objective

Provide operator and developer documentation for source ingestion,
provider-independent matching, MCP use and policy, and the existence-only CBD
handoff before any BoK read operation is published through MCP.

## Delivered Documents

- `src/main/car/manual/index.md` is the packaged reference manual.
- `src/main/car/manual/user-guide.md` is the packaged operator guide.
- `docs/developer-guide.md` is the repository developer contract.
- `README.md` provides public navigation and an accurate current MCP state.

The documents keep SIE provider selection outside BoK, define complete
generation replacement and degraded retention, distinguish exact knowledge
from candidates, and direct component usage/compatibility questions to Textus
CBD Support.

## Verification Evidence

On 2026-07-21, the documentation was reviewed against the CML operations,
generated component selectors, implemented resource schema, generic SIE
component calls, matching specifications, and Textus CBD Support handoff
operations. `sbt --batch test cozyBuildCAR` passed 14 tests in 5 suites and
built `target/textus-bok-0.1.0-SNAPSHOT.car`. Archive inspection confirmed
`manual/index.md` and `manual/user-guide.md`. Normal CAR lint and the documented
portable `cozy lint car .` command reported no failure; the missing released ABI
baseline and SNAPSHOT sbt-cozy dependency remain release-readiness warnings.

Runtime `meta.help` execution remains unverified in this documentation slice.
The launcher selected project development mode without a generated component
runtime classpath, while disabling that mode could not resolve a compatible
`main-target` runtime. The documented selectors are verified from generated
component metadata, not claimed as executed runtime evidence.

## Next Boundary

Phase 4 may make `searchTerms`, `explainTerm`, `searchComponentReferences`, and
`getComponentReference` MCP ready. `replaceKnowledgeSource` remains private.
