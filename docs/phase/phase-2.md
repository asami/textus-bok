# Phase 2: BoK Domain Model and Source Normalization

## Stage Status

- Status: `COMPLETE`
- Current step: Phase 2 closure verified; prepare operator and developer documentation.
- Closure basis: Every Phase 2 checklist item is complete and executable
  verification evidence is recorded below.

## Objective

Define the BoK-owned terminology, component-existence, source, evidence,
request, and response models, then normalize logical BoK resources without
direct filesystem or network access.

## Verification Evidence

On 2026-07-21, `sbt --batch test cozyBuildCAR` resolved CNCF
`0.5.1-SNAPSHOT` through the normal dependency route, reused 38 generated
Scala sources, passed 14 tests in 5 suites, and built
`target/textus-bok-0.1.0-SNAPSHOT.car`.

`BokDomainModelSpec` verifies the five typed operation contracts, generated
source/evidence/term/component-reference attributes and multiplicities,
exclusion of CBD-owned detail, and deny-by-default MCP policy.
`BokSourceReaderSpec` verifies metadata-only normalization through the CNCF
resource DSL, safe relative child references, canonical repository-index
validation, deterministic ordering, empty-source warnings, and duplicate
identity rejection without rendered HTML or host filesystem access.
`BokFederationPublicationSpec` verifies the
generated SIE component boundary, identical-generation retry, complete
generation replacement, stale document/assertion/evidence removal, and
fail-closed behavior when SIE is absent. `BokKnowledgeCatalogSpec` verifies
exact and provider-backed candidate matches, ambiguity, conflict,
insufficient-evidence, no-match, complete source replacement, stale domain
record removal, and retention of the last complete generation after degraded
publication. The generated SIE query contract specification also verifies
dataset/source-scoped candidate attribution and bounded 10-to-20 result
overfetch when unrelated generic knowledge precedes the BoK candidate.

## Next Boundary

Operator and developer documentation for source ingestion, provider-independent
matching, MCP use, and CBD handoff is the next boundary. MCP publication and
CBD detail resolution remain outside this phase.
