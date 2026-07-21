# Phase 2: BoK Domain Model and Source Normalization

## Stage Status

- Status: `IN_PROGRESS`
- Current step: Specify and implement matching states and source replacement.
- Closure basis: Every Phase 2 checklist item is complete and executable
  verification evidence is recorded below.

## Objective

Define the BoK-owned terminology, component-existence, source, evidence,
request, and response models, then normalize logical BoK resources without
direct filesystem or network access.

## Verification Evidence

On 2026-07-21, `sbt --batch test cozyBuildCAR` resolved CNCF
`0.5.1-SNAPSHOT` through the normal dependency route, reused 38 generated
Scala sources, passed 12 tests in 4 suites, and built
`target/textus-bok-0.1.0-SNAPSHOT.car`.

`BokDomainModelSpec` verifies the five typed operation contracts, generated
source/evidence/term/component-reference attributes and multiplicities,
exclusion of CBD-owned detail, and deny-by-default MCP policy.
`BokSourceReaderSpec` verifies metadata-only normalization through the CNCF
resource DSL, safe relative child references, canonical repository-index
validation, deterministic ordering, empty-source warnings, and duplicate
identity rejection without rendered HTML or host filesystem access. Matching
behavior remains unimplemented. `BokFederationPublicationSpec` verifies the
generated SIE component boundary, identical-generation retry, complete
generation replacement, stale document/assertion/evidence removal, and
fail-closed behavior when SIE is absent.

## Next Boundary

Exact, candidate, ambiguity, conflict, insufficient-evidence, and no-match
classification is the next boundary. BoK MCP publication and CBD detail
resolution remain outside this phase slice.
