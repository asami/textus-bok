# Textus BoK

Textus BoK is the BoK-domain CAR for terminology and component-existence
knowledge. It is independent from the generic Textus Semantic Integration
Engine and uses that component only through its public component contract.

Component:
- artifact: `textus-bok`
- package: `org.simplemodeling.textus.bok`
- version: `0.1.0-SNAPSHOT`

## Development

Component identity, Scala and Cozy runtime versions, and CAR library
dependencies are maintained in `project.yaml`. The sbt build-plugin coordinate
remains in `project/plugins.sbt` because sbt must resolve it before reading the
project model. Generate and verify the CAR with:

```sh
sbt --batch test cozyBuildCAR
```

Generated Scala sources are written under `target/scala-3.3.8/src_managed/main/scala`.

`replaceKnowledgeSource` reads logical metadata through the CNCF resource DSL
and replaces the complete source generation through SIE's generated
`KnowledgeFederation` component contract. The CAR ABI dependency and exact
development `provided` coordinate are declared separately in `project.yaml`,
so SIE API classes are available while compiling but are not duplicated in the
BoK CAR.

Typed matching remains inside Textus BoK. Exact knowledge is distinct from
provider-backed candidates, and complete source generations remove stale
records only after SIE confirms publication. MCP publishes the four typed BoK
read operations, including existence-only component references. Source
replacement is permanently excluded.

The component also provides the public, read-only `getKnowledgeMap` operation
and its Static Form view at `/web/bok/textus-bok/map`. Both project one bounded
selected generation with source evidence and an existence-only CBD handoff;
neither operation is MCP-ready and the browser never performs source mutation.
To verify a real Cozy source locally, build it with Cozy 0.3.0-SNAPSHOT and run
`scripts/run-bok-knowledge-map-sar.sh start` with
`TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT` set to its generated `website.d` root.

## Documentation

- [Development strategy](docs/strategy/textus-bok-development-strategy.md)
- [Phase 1](docs/phase/phase-1.md)
- [Phase 1 checklist](docs/phase/phase-1-checklist.md)
- [BoK domain model contract](docs/spec/bok-domain-model.md)
- [Phase 2](docs/phase/phase-2.md)
- [Phase 2 checklist](docs/phase/phase-2-checklist.md)
- [Phase 3](docs/phase/phase-3.md)
- [Phase 3 checklist](docs/phase/phase-3-checklist.md)
- [Phase 4](docs/phase/phase-4.md)
- [Phase 4 checklist](docs/phase/phase-4-checklist.md)
- [Phase 5](docs/phase/phase-5.md)
- [Phase 5 checklist](docs/phase/phase-5-checklist.md)
- [Deferred release work](docs/phase/deferred-release-work.md)
- [Phase 6](docs/phase/phase-6.md)
- [Phase 6 checklist](docs/phase/phase-6-checklist.md)
- [Knowledge Map Web application specification draft](docs/notes/textus-bok-knowledge-map-web-application-spec-draft.md)
- [Knowledge Map Web application consideration journal](docs/journal/2026/07/textus-bok-knowledge-map-web-application-consideration-2026-07-23.md)
- [Temporary migration SAR](examples/bok-migration-sar/README.md)
- [BoK Codex SAR](examples/bok-codex-sar/README.md)
- [Developer guide](docs/developer-guide.md)
- [Packaged reference manual](src/main/car/manual/index.md)
- [Packaged operator guide](src/main/car/manual/user-guide.md)
