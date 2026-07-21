# Phase 2: BoK Domain Model and Source Normalization

## Stage Status

- Status: `IN_PROGRESS`
- Current step: Implement CNCF resource DSL source reading and normalization.
- Closure basis: Every Phase 2 checklist item is complete and executable
  verification evidence is recorded below.

## Objective

Define the BoK-owned terminology, component-existence, source, evidence,
request, and response models, then normalize logical BoK resources without
direct filesystem or network access.

## Verification Evidence

On 2026-07-21, `sbt --batch test cozyBuildCAR` reused 38 generated Scala
sources, passed 5 tests in 2 suites, and built
`target/textus-bok-0.1.0-SNAPSHOT.car`.

`BokDomainModelSpec` verifies the five typed operation contracts, generated
source/evidence/term/component-reference attributes and multiplicities,
exclusion of CBD-owned detail, and deny-by-default MCP policy. Source reading
and matching behavior remain unimplemented.

## Next Boundary

Resource reading and normalization use the CNCF resource DSL. Generic SIE
dataset publication, BoK MCP publication, and CBD detail resolution remain
outside this phase slice.
