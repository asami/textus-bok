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

## Documentation

- [Development strategy](docs/strategy/textus-bok-development-strategy.md)
- [Phase 1](docs/phase/phase-1.md)
- [Phase 1 checklist](docs/phase/phase-1-checklist.md)
- [BoK domain model contract](docs/spec/bok-domain-model.md)
- [Phase 2](docs/phase/phase-2.md)
- [Phase 2 checklist](docs/phase/phase-2-checklist.md)
