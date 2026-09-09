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

Canonical script roles are:

- `scripts/prepare-development-runtime.sh` prepares complete development runtime evidence through SBT;
- `scripts/test/run-server.sh` starts a test/development server through the CNCF launcher; and
- `scripts/run-server.sh` starts the production/operator server through the Textus launcher.

Generated Scala sources are written under `target/scala-3.3.8/src_managed/main/scala`.

`replaceKnowledgeSource` reads logical metadata through the CNCF resource DSL
and replaces the complete source generation through SIE's generated
`KnowledgeFederation` component contract. The CAR ABI dependency and exact
development `provided` coordinate are declared separately in `project.yaml`,
so SIE API classes are available while compiling but are not duplicated in the
BoK CAR.

Typed matching remains inside Textus BoK. Exact knowledge is distinct from
provider-backed candidates, and complete source generations remove stale
records only after SIE confirms publication. MCP publishes ten bounded,
read-only BoK operations, including existence-only component references and
Knowledge Map. Source replacement is permanently excluded.

The component also provides the public, read-only `getKnowledgeMap` operation
and its Static Form view at `/web/bok/textus-bok/map`. Both project one bounded
selected generation with source evidence and an existence-only CBD handoff;
Knowledge Map is MCP-ready as a bounded, read-only projection; the browser
never performs source mutation.
To verify a real Cozy source locally, build it with Cozy 0.3.0-SNAPSHOT and run
`scripts/test/run-bok-knowledge-map-sar.sh start` with
`TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT` set to its generated `website.d` root.

Phase 7.4 also prepares one representative profile-selection SAR with official,
development, and two distinct project keys. Its positive reads and cyclic
foreign-term/foreign-node probes demonstrate selected generation, evidence, and
project isolation without union or fallback; the prepared lifecycle is not
claimed live-passed until the Phase release gate runs it. See the
[representative profile-selection verification](examples/bok-profile-selection-sar/README.md).

### Managed SimpleModeling.org Development Bootstrap

Phase 8's managed development path is an explicit build → configure →
Control Center restart → MCP verify sequence. Prepare an accepted WIP snapshot
in a private temporary directory, then configure its generated `website.d`:

```sh
snapshot_root="$(mktemp -d /private/tmp/textus-bok-simplemodeling-wip.XXXXXX)"
scripts/test/prepare-simplemodeling-wip-snapshot.sh \
  --version 1.0.56 \
  --output-dir "$snapshot_root"
source_root="$snapshot_root/website.d"
scripts/configure-simplemodeling-development.sh \
  --source-root "$source_root"
```

The writer validates the `cncf.knowledge-source.v1` metadata, derives a
content-bound `sha256:` generation, and atomically writes the marked private
`.textus/config.yaml` with only the explicit `development` bootstrap binding,
in-memory SIE policy, and exact read-only file root. It refuses an unmarked
existing config and supports `--output PATH` for tests. Restart the managed
instance through Textus Control Center, then use the read-only probe with the
expected term count, present/absent term identities, and emitted generation:

```sh
scripts/test/probe-managed-bok.py \
  --source-root "$source_root" \
  --present-term-id architecture:availability \
  --absent-term-id literate-modeling:story \
  --expected-term-count 215 \
  --generation sha256:40f96e99b2308e0474584513ff7602928008d57de8e3f3bd2494a5aa83402cec
```

The probe checks loopback readiness, exact profile/source attribution, term
counts, and the absence of `replaceKnowledgeSource` from MCP without invoking
replacement. Malformed metadata must fail before configuration changes. To
roll back, restore the accepted `website.d`, rerun the writer, and restart via
Control Center. This path has no watcher, polling, hot reload, restart
controller, or Control Center API.

For final restoration, use the current SimpleModeling.org `website.d` as a
separate source root. Its accepted current values are
`literate-modeling:story` present, `architecture:availability` absent, 212
terms, and generation
`sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f`:

```sh
current_source_root="/Users/asami/src/dev2025/simplemodeling-org/website.d"
scripts/configure-simplemodeling-development.sh \
  --source-root "$current_source_root"
```

Restart `textus-bok` through the normal Textus Control Center/supervisor
lifecycle, then run the current read-only probe:

```sh
scripts/test/probe-managed-bok.py \
  --source-root "$current_source_root" \
  --present-term-id literate-modeling:story \
  --absent-term-id architecture:availability \
  --expected-term-count 212 \
  --generation sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f
```

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
- [Phase 7: BoK Profile Selection Contract](docs/phase/phase-7.md)
- [Phase 7 checklist](docs/phase/phase-7-checklist.md)
- [BoK Profile Selection design](docs/design/bok-profile-selection.md)
- [BoK Profile Selection contract](docs/spec/bok-profile-selection-contract.md)
- [Phase 7.1: Profile Registry and Resolution](docs/phase/phase-7.1.md)
- [Phase 7.1 checklist](docs/phase/phase-7.1-checklist.md)
- [Phase 7.2: Read Contract Integration](docs/phase/phase-7.2.md)
- [Phase 7.2 checklist](docs/phase/phase-7.2-checklist.md)
- [Phase 7.3: Web and Operator Integration](docs/phase/phase-7.3.md)
- [Phase 7.3 checklist](docs/phase/phase-7.3-checklist.md)
- [Phase 7.4: Representative Verification and Closure](docs/phase/phase-7.4.md)
- [Phase 7.4 checklist](docs/phase/phase-7.4-checklist.md)
- [Phase 7.5: Information Profile Verification](docs/phase/phase-7.5.md)
- [Phase 7.5 checklist](docs/phase/phase-7.5-checklist.md)
- [Phase 8: Configured KnowledgeSource Startup Bootstrap](docs/phase/phase-8.md)
- [Phase 8 checklist](docs/phase/phase-8-checklist.md)
- [Knowledge Map Web application specification draft](docs/notes/textus-bok-knowledge-map-web-application-spec-draft.md)
- [Knowledge Map Web application consideration journal](docs/journal/2026/07/textus-bok-knowledge-map-web-application-consideration-2026-07-23.md)
- [Temporary migration SAR](examples/bok-migration-sar/README.md)
- [BoK Codex SAR](examples/bok-codex-sar/README.md)
- [Developer guide](docs/developer-guide.md)
- [Packaged reference manual](src/main/car/manual/index.md)
- [Packaged operator guide](src/main/car/manual/user-guide.md)
- [Startup bootstrap contract](docs/spec/bok-startup-bootstrap-contract.md)
