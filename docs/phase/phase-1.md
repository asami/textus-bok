# Phase 1: CAR Baseline

## Stage Status

- Status: `COMPLETE`
- Current step: Start Phase 2 domain modeling only after the baseline is
  accepted.
- Closure basis: Every Phase 1 checklist item is complete and executable
  verification evidence is recorded below.

## Objective

Establish `textus-bok` as an independent Cozy-generated CAR with canonical
project metadata, CML, generated sources, documentation, and executable
specification layout.

## Verification Evidence

On 2026-07-21, `sbt --batch test cozyBuildCAR` used the declared Cozy runtime,
generated 17 Scala sources, passed 1 test in 1 suite, and built
`target/textus-bok-0.1.0-SNAPSHOT.car`.

The Cozy scaffold fix used by this baseline passed all 637 tests in 55 suites
(2 canceled) and the `cozy/generate-smoke` scripted test. The scripted test
scaffolded a custom CAR and verified its generated executable specification,
model metadata, ABI manifest, and CAR artifact end to end.

The development scaffold declares sbt-cozy `0.1.15-SNAPSHOT`, which is required
for the current metadata packaging contract. Publishing that plugin coordinate
is a release-track responsibility and is not represented as completed baseline
work.

## Next Boundary

Phase 2 defines BoK terminology and component-existence models. Source
normalization, SIE publication, and MCP migration are outside this phase.
