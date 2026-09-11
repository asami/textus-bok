# Textus BoK Operator Guide

## Prerequisites

Deploy `textus-bok` for its resident in-memory terminology, component, and map
read path. That default path does not require SIE. When an operator explicitly
selects the optional `sie` knowledge backend, deploy a compatible SIE provider
in the same CNCF subsystem and keep RDF, Vector, embedding, credentials, and
provider endpoints in that SIE deployment configuration. In configured mode,
install private profile bindings and configure a CNCF resource provider that
resolves each binding's logical source root. In legacy mode (when no profile
registry is configured), the provider resolves the logical root in the
request's `BokKnowledgeSource.resource`.

Use generated Help to confirm the loaded component and operation contract:

```sh
cncf command meta.help textus-bok --format yaml
```

## Configure A Private Profile Registry

The private subsystem configuration key `textus.bok.profile-registry` binds a
profile to one source identity, logical resource, and generation-level
evidence. Configure all three profile kinds as distinct bindings;
`projectId` belongs only to a project binding:

```yaml
textus:
  bok:
    profile-registry:
      profiles:
        - profile: official
          source:
            sourceId: simplemodeling-official
            datasetId: simplemodeling-bok-official
            generation: 2026-07-21T12:00:00Z
            resource: urn:textus:bok:simplemodeling:official
          evidence:
            uri: https://evidence.example/simplemodeling/official
            sourceId: simplemodeling-official
        - profile: development
          source:
            sourceId: simplemodeling-development
            datasetId: simplemodeling-bok-development
            generation: 2026-08-01T09:00:00Z
            resource: urn:textus:bok:simplemodeling:development
          evidence:
            uri: https://evidence.example/simplemodeling/development
            sourceId: simplemodeling-development
          freshnessGeneration: 2026-08-01T09:00:00Z
        - profile: project
          projectId: example-project
          source:
            sourceId: example-project
            datasetId: example-project-bok
            generation: 2026-08-02T15:30:00Z
            resource: urn:textus:bok:projects:example-project
          evidence:
            uri: https://evidence.example/projects/example-project
            sourceId: example-project
```

Each `sourceId`, `datasetId`, `generation`, logical `resource`, and evidence
value identifies its own binding, and each evidence `sourceId` matches the
binding's source identity. These are logical CNCF resources; do not put a
filesystem path, network location, credential, or provider setting in a
public request. `freshnessGeneration` is optional. When omitted, any admitted
complete generation for that binding is acceptable. When present, resolution
requires that exact generation; a mismatch is `stale` even when an older
complete generation is retained for atomic rollback safety.

Bindings are validated during component bootstrap. Duplicate or conflicting
bindings, malformed source descriptors, and mismatched evidence are rejected
before replacement starts.

## Managed SimpleModeling.org Development Bootstrap

The managed development profile is loaded during server assembly only when the
project-local `.textus/config.yaml` explicitly selects `development`. Prepare
the generated source from an accepted SimpleModeling.org WIP snapshot, without
running the source checkout in place:

```sh
snapshot_root="$(mktemp -d /private/tmp/textus-bok-simplemodeling-wip.XXXXXX)"
scripts/test/prepare-simplemodeling-wip-snapshot.sh \
  --version 1.0.56 \
  --output-dir "$snapshot_root"
source_root="$snapshot_root/website.d"
```

Configure the resulting `website.d` root with the repository-owned writer:

```sh
scripts/configure-simplemodeling-development.sh \
  --source-root "$source_root"
```

The writer validates `metadata/cncf/knowledge-source.json`, every declared
resource, and the glossary terms. It derives a content-bound `sha256:`
generation and atomically writes a marked private config with one
`simplemodeling-development` binding, an explicit `development` bootstrap
selector, in-memory RDF/Vector SIE providers, and the exact read-only file
root. It refuses to overwrite an unmarked existing config; `--output PATH` is
available for isolated tests.

After configuration, restart the managed instance through the normal Textus
Control Center/supervisor lifecycle. Do not call `replaceKnowledgeSource` for
this startup path. Verify readiness and attribution through the bounded,
read-only MCP probe:

```sh
scripts/test/probe-managed-bok.py \
  --source-root "$source_root" \
  --present-term-id architecture:availability \
  --absent-term-id literate-modeling:story \
  --expected-term-count 215 \
  --generation sha256:40f96e99b2308e0474584513ff7602928008d57de8e3f3bd2494a5aa83402cec
```

The probe requires a local loopback endpoint, checks the expected present and
absent term identities and counts, verifies the exact `development` profile,
dataset, source, generation, and evidence attribution, and confirms that no
mutation operation is MCP-ready. If metadata is malformed, unsafe, or
incomplete, configuration fails before the prior marked config changes; do not
restart that candidate. Restore the last accepted `website.d`, regenerate the
marked config, and restart through Control Center to roll back. Updates are
always build-then-configure-then-restart: there is no watcher, polling,
hot-reload, or Control Center mutation API.

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

## Bounded Two-Source Documentation Catalog Acceptance

The P10 two-source acceptance uses only the current generated roots below;
they are explicit inputs, not locations inferred from the current directory,
request, host state, or source discovery:

```sh
simplemodeling_source_root="/Users/asami/src/dev2025/simplemodeling-org/website.d"
knowledgehub_source_root="/Users/asami/src/Project2026/bok-knowledgehub/website.d"
scripts/configure-bok-two-source-acceptance.sh \
  --simplemodeling-source-root "$simplemodeling_source_root" \
  --knowledgehub-source-root "$knowledgehub_source_root"
```

The writer validates both KnowledgeSource manifests and every declared regular,
non-symlink resource; it derives both generations and atomically writes only a
marked private configuration. Its normal target is the exact project
`.textus/config.yaml`; it may migrate that exact default from the established
`textus-bok-managed-profile-config: v1` marker. For isolated checks,
`--output` accepts only a file strictly below this repository's real `target/`
tree. Dot-segment paths, symbolic-link ancestors, and every other unmarked
configuration are refused before a directory is created or a file is written.
The writer configures `documentation` with `development` and
`project:knowledgehub`, preserves that bootstrap order, and explicitly selects
the BoK-owned `memory` backend. It writes no `textus.sie` policy. The resident
memory path does not select an SIE backend. This SAR assembles SIE,
BoK, and Scraper as sibling CARs in that order. SIE supplies BoK's typed
optional federation API and starts with its in-memory settings; Scraper joins
only as SIE's required sibling dependency. Neither is embedded in the BoK CAR
or selected as the BoK knowledge backend, and Scraper is not a catalog source,
catalog member, MCP capability, or external source access path. Explicit BoK
`sie` selection remains the optional provider path and fails closed when
unavailable.

For bounded runtime acceptance, the repository-owned SAR takes the same two
explicit roots. It creates runtime output only under this repository's
`target/`, packages SIE then BoK then Scraper as exactly three sibling CARs
plus local user security, launches one loopback runtime with
`--textus.config.file` and `--no-default-components`, and removes only that
captured runtime and its owned directory. The SIE CAR is an API provider
sibling, present only for that typed API and dependency closure. The endpoint
can also publish framework-owned common tools, but the complete BoK MCP
projection is its ten established read tools. SIE remains a sibling CAR/API
provider but is non-MCP-published through `cncf.mcp.enabled: "false"`, without
disabling its typed component API or provider role. Scraper joins only to
satisfy SIE's required Scrapers SPI. Neither is
a BoK-embedded implementation or BoK backend selection, and Scraper is not a
catalog source, catalog member, MCP capability, or external source access
path:

```sh
scripts/test/check-bok-two-source-acceptance-sar.sh \
  --simplemodeling-source-root "$simplemodeling_source_root" \
  --knowledgehub-source-root "$knowledgehub_source_root"
```

Its loopback MCP probe requires the exact qualified BoK set
`org.simplemodeling.textus.Bok.BokRetrieval.{searchTerms, explainTerm,
searchComponentReferences, getComponentReference, getKnowledgeMap,
searchSemanticKnowledge, getSemanticManifest, getSemanticResource,
getSemanticSection, discoverSemanticKnowledge}` exactly once, confirms that
no SIE-qualified or Scraper-qualified MCP tool or `replaceKnowledgeSource` is
published, and reports both the BoK projection and actual endpoint tool counts.
`replaceKnowledgeSource` remains REST-only. Framework-owned common endpoint
tools are outside the BoK projection.
It reads SimpleModeling Story
(`literate-modeling:story`) and KnowledgeHub Embedding (`technology:embedding`)
through `profile=catalog` / `catalogId=documentation`, and confirms their exact
source-qualified member attribution. It proves both sources appear in one
Knowledge Map response. The only mutation is one existing protected REST
replacement of KnowledgeHub, after which the probe proves the SimpleModeling
generation remains resident.

## Prepare Metadata

At the logical source root, provide
`metadata/cncf/knowledge-source.json`:

```json
{
  "schemaVersion": "cncf.knowledge-source.v1",
  "resources": [
    {
      "kind": "glossary-terms",
      "href": "metadata/glossary/terms.json"
    },
    {
      "kind": "component-repository-index",
      "href": "repository/catalog/index.json"
    }
  ]
}
```

A minimal glossary resource is:

```json
{
  "terms": [
    {
      "id": "architecture:component",
      "title": "Component",
      "definition_text": "A reusable unit with an explicit contract.",
      "category": "architecture",
      "term_type": "concept"
    }
  ]
}
```

The repository index must satisfy
`cncf.component-repository-index.v2`. Its CAR/SAR entries become existence-only
references. Textus BoK does not read their archives or manuals.

## Replace A Generation

SAR/component configuration prepares the private bindings; it does not load
or admit their content. Invoke the existing protected administrative
`org.simplemodeling.textus.Bok.BokRetrieval.replaceKnowledgeSource` operation
separately for each configured binding identity and generation. Use CNCF record
path notation for the generated nested datatype:

```sh
cncf command org.simplemodeling.textus.Bok.BokRetrieval.replaceKnowledgeSource \
  --source.sourceId simplemodeling-official \
  --source.datasetId simplemodeling-bok-official \
  --source.generation 2026-07-21T12:00:00Z \
  --source.resource urn:textus:bok:simplemodeling:official \
  --format yaml
```

Repeat the command with `simplemodeling-development` /
`simplemodeling-bok-development` / `2026-08-01T09:00:00Z` and with
`example-project` / `example-project-bok` / `2026-08-02T15:30:00Z` for the
development and `example-project` bindings. In configured mode the identity
selects the private binding resource; the request-side logical `resource`
field is ignored and is not an alternate source. Never use a request-supplied
filesystem or network location.

The input is equivalent to:

```yaml
source:
  sourceId: simplemodeling-official
  datasetId: simplemodeling-bok-official
  generation: 2026-07-21T12:00:00Z
  resource: urn:textus:bok:simplemodeling:official
```

Treat `status: complete` as the commit point. Record `sourceId`, `datasetId`,
`generation`, `termCount`, `componentCount`, and warnings. A degraded or failed
publication is not a successful typed-generation switch.

With a private profile registry configured, configured mode matches the request
`datasetId`, `sourceId`, and `generation` to one installed binding and loads
only that binding's `source.resource`. The request-side `resource` may remain
in the generated compatibility request shape, but it is ignored and is not an
alternate source. When no registry is configured, legacy mode reads the
request-side `source.resource` as before.

Only a `complete` publication is admitted. A degraded or failed replacement
keeps the previous complete generation active; a later complete generation
replaces it atomically.

To replace content, publish a complete later generation with the same
`datasetId`. Omitted records are intentionally removed. Do not send partial
delta generations.

## Read Knowledge

Use `searchTerms` for discovery and `explainTerm` for an exact identity or
title. Accept `matched` exact knowledge directly only with its evidence. Treat
`candidate` as inferred retrieval evidence requiring explicit selection.
Resolve `ambiguous`, `conflict`, and `insufficient-evidence` instead of choosing
silently. `no-match` means that Textus BoK has no grounded answer.

Every read uses exactly one closed selection form. Direct selection preserves
the Phase 7 contract: omit `--profile` (or use `--profile official`) for the
official binding, use `--profile development` for the registered development
binding, or use `--profile project --projectId <logical-project-id>` for one
exact registered project binding. A direct request never combines configured
generations, infers a project from host, checkout, or request filters, or gains
catalog fallback, precedence, or aggregate behavior. A project selector without
`projectId`, and `projectId` or `catalogId` combined with `official` or
`development`, are structured selection failures; `catalogId` also conflicts
with a `project` selector.

The sole aggregate selection is `--profile catalog --catalogId
<logical-catalog-id>`. `catalogId` is an opaque configured key, not a URL,
resource reference, filesystem path, repository, provider setting, or
caller-supplied member list. A catalog request without a usable `catalogId` is
`invalid-selection`; one with `projectId`, `datasetId`, or `sourceId` is
`conflicting-selection`. It resolves every configured direct member through
the existing authorization, registration, admission, freshness, and direct
compatibility checks. Any member failure fails the whole catalog: no partial
catalog, fallback profile, or foreign generation is returned.

A direct success contains `selection` attribution for its resolved profile,
optional project identity, dataset, source, generation, and generation
evidence. A catalog success has `selection.resolvedProfile = catalog`, its
requested `catalogId`, and deterministic source-qualified member attributions;
it has no top-level dataset, source, generation, or evidence tuple. Keep the
record-level evidence when handing an exact term or component reference to
another reader.

```sh
cncf command org.simplemodeling.textus.Bok.BokRetrieval.searchTerms \
  --query Component --category architecture --limit 10 --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.searchTerms \
  --query Component --profile development --limit 10 --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.explainTerm \
  --term architecture:component --profile project \
  --projectId example-project --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.explainTerm \
  --term architecture:component --profile catalog \
  --catalogId documentation --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.explainTerm \
  --term architecture:component --format yaml
```

## Observe Profile Freshness

For every successful read, record the `selection.resolvedProfile`, optional
`selection.projectId`, `selection.datasetId`, `selection.sourceId`,
`selection.generation`, and `selection.evidence` fields:

```yaml
selection:
  resolvedProfile: development
  datasetId: simplemodeling-bok-development
  sourceId: simplemodeling-development
  generation: 2026-08-01T09:00:00Z
  evidence:
    uri: https://evidence.example/simplemodeling/development
    sourceId: simplemodeling-development
```

For a project result, the same record also contains the exact `projectId`.
Compare the observed generation with the configured `freshnessGeneration`: an
omitted policy accepts any admitted complete generation, while an exact
mismatch is `stale`. Confirm that the separate replacement result was
`status: complete`; degraded or failed replacement retains the prior complete
generation but does not make a stale binding readable. The Knowledge Map
source-generation table, including its complete no-JavaScript output, provides
the same observation point for browser reads.

Use `searchComponentReferences` to discover that a CAR or SAR exists and
`getComponentReference` to resolve an exact name, optional version, kind, and
organization. The canonical identity is `kind` plus optional organization plus
name; version is a selector and ordering tie-breaker, not identity. When CAR
and SAR share a name, pass the returned `kind` explicitly. When the same
artifact is published by more than one organization, pass `organization` or
the lookup remains `ambiguous` without a reference.

```sh
cncf command org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference \
  --name textus-account --kind car --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference \
  --name textus-account --kind car \
  --organization org.simplemodeling.textus --format yaml
```

## Read A Knowledge Map

After a complete replacement of a Cozy source that declares
`metadata/rdf/graph.json`, query the bounded factual map through the normal
component boundary:

```sh
cncf command org.simplemodeling.textus.Bok.BokRetrieval.getKnowledgeMap \
  --profile official \
  --nodeLimit 128 --relationshipLimit 256 --format yaml
```

The map uses the same public operation for all profile kinds:

| Read | Selection | Meaning |
|---|---|---|
| official | omit `profile` or use `--profile official` | the official binding |
| development | `--profile development` | the registered development binding |
| project | `--profile project --projectId example-project` | the exact project binding |
| catalog | `--profile catalog --catalogId documentation` | the configured complete multi-source catalog |

`datasetId` and `sourceId` are compatibility assertions after profile
resolution, not profile selectors. Omit them to use the complete resolved
generation, or supply the exact resolved values to confirm it. For example,
`--profile development --datasetId simplemodeling-bok-development
--sourceId simplemodeling-development` asserts the resolved development
identity; it does not select development. A value from a development or
project generation on an omitted/official request fails as
`conflicting-selection`; it never changes the request to another profile.
Catalog selection instead requires `catalogId` and rejects `projectId`,
`datasetId`, and `sourceId` as `conflicting-selection`; it has no synthetic
single-source tuple to assert.

For an operator-facing browser projection, open
`/web/bok/textus-bok/map` on the same runtime. Its filters select the same
public operation; the page keeps complete tables available without JavaScript
and renders the selected profile, source, generation, and evidence in its
source-generation table/no-JavaScript output. Omit the profile for official;
choose development explicitly, or choose project with its exact `projectId`.
Choose catalog with its exact `catalogId`; do not combine it with a project or
source assertion. The form has no resource, registration, replacement, or
other mutation control.

For the representative Cozy handoff verification, set
`TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT` to the generated `website.d` directory
and run `scripts/test/run-bok-knowledge-map-sar.sh start`. The probe replaces the
source and proves REST and Web agreement on generation, topology, evidence,
truncation, and `componentRef` handoffs.

## Profile Selection Representative SAR

The prepared profile-selection representative SAR is a one-shot operator
procedure for the private four-binding model. Its historical fixture packages
the existing Textus BoK, Semantic Integration Engine, and Scraper SNAPSHOT
CARs; that fixture composition is not a requirement of the resident memory
path or the bounded two-source catalog acceptance. It also needs a compatible
CNCF runtime, Python 3.10 or newer, and `zip`, `curl`, and `lsof`. Use
`PYTHON_BIN` to choose the Python 3.10+ interpreter when the default `python3`
is not suitable. From the Textus BoK repository, run:

```sh
scripts/test/check-bok-profile-selection-sar.sh
```

The logical bindings are official (omitted selector), development,
project-alpha, and project-beta. Their prepared identities are deliberately
distinct:

| Binding | Dataset | Source | Generation | Registry evidence URI |
|---|---|---|---|---|
| official | `profile-official-dataset` | `profile-official-source` | `2026-08-15T00:00:00Z` | `https://evidence.example/textus-bok/profile-official` |
| development | `profile-development-dataset` | `profile-development-source` | `2026-08-15T01:00:00Z` | `https://evidence.example/textus-bok/profile-development` |
| project (`project-alpha`) | `profile-project-alpha-dataset` | `profile-project-alpha-source` | `2026-08-15T02:00:00Z` | `https://evidence.example/textus-bok/profile-project-alpha` |
| project (`project-beta`) | `profile-project-beta-dataset` | `profile-project-beta-source` | `2026-08-15T03:00:00Z` | `https://evidence.example/textus-bok/profile-project-beta` |

The lifecycle checks positive REST `searchTerms` and Knowledge Map reads,
Static Form Web map reads, and qualified MCP terminology/read-tool
publication. It also cycles foreign-term and foreign-focus queries across the
four bindings: each must retain the selected attribution while returning no
match with empty results or topology. The checks prove that a selected read
does not union generations, cross project boundaries, or fall back. REST and
Web map projections must agree. For incomplete and unknown project selectors,
the two MCP calls retain their legacy error text and expose the exact
`structuredContent.error.appStatus`; the two Static Form Web checks intentionally
return HTTP 200 result pages with escaped `error.appStatus` and no fallback
selection attribution. `tools/list` includes Knowledge Map as a bounded,
read-only projection, and `replaceKnowledgeSource` remains protected and non-MCP; callers do not provide
private resource roots.

On a live release-gate run, the probe reports these stable markers:

```text
BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 web_maps=4 mcp_terms=4 mcp_maps=4 negative_rest_terms=4 negative_mcp_terms=4 negative_rest_maps=4 negative_mcp_maps=4 negative_web_maps=4 mcp_failures=2 web_failures=2
BOK_PROFILE_SELECTION_SAR_LIFECYCLE_OK profiles=4
```

The lifecycle accepts
loopback HTTP only, rejects an occupied port, owns a temporary runtime
directory, stops only its process/listener, and removes only that directory.
Repository, CAR, fixture, CNCF, port, URL, timeout, and runtime-development
overrides are documented in the
[profile-selection SAR README](../../../../examples/bok-profile-selection-sar/README.md).

Phase 7.4's release gate has live-passed this representative lifecycle and
probe. Full-test and CAR build/lint evidence is recorded separately in the
Phase ledger; final re-review, release commit, and phase closure remain
separate gates until they complete.

## MCP Use

The current component publishes ten bounded, read-only MCP read tools:

- `org.simplemodeling.textus.Bok.BokRetrieval.searchTerms`;
- `org.simplemodeling.textus.Bok.BokRetrieval.explainTerm`;
- `org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences`;
- `org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference`;
- `org.simplemodeling.textus.Bok.BokRetrieval.getKnowledgeMap`;
- `org.simplemodeling.textus.Bok.BokRetrieval.searchSemanticKnowledge`;
- `org.simplemodeling.textus.Bok.BokRetrieval.getSemanticManifest`;
- `org.simplemodeling.textus.Bok.BokRetrieval.getSemanticResource`;
- `org.simplemodeling.textus.Bok.BokRetrieval.getSemanticSection`; and
- `org.simplemodeling.textus.Bok.BokRetrieval.discoverSemanticKnowledge`.

Use the runtime's MCP `tools/list` result as the authoritative names and input
schemas. `replaceKnowledgeSource` remains absent permanently. A CAR/SAR
deployment may narrow the tool list but cannot broaden it to private or
administration operations.

For a local Codex endpoint, use the repository-owned
`examples/bok-codex-sar` assembly and `scripts/test/run-bok-codex-sar.sh`. That SAR
disables the transitional SIE BoK tools, loads metadata through Textus BoK, and
keeps SIE's provider-neutral component API available in the same subsystem.

Use `cncf.mcp.enabled=false` to disable component publication, or list declared
service/operation identities in `cncf.mcp.disabled-services` and
`cncf.mcp.disabled-operations`. Configuration cannot make an undeclared
operation MCP ready.

## CBD Handoff

A component reference proves existence, not suitability. Pass `name`, `kind`,
optional `organization` and `version`, plus the evidence URI to Textus CBD
Support. Use `CbdRetrieval.getComponent` for exact detail or
`CbdRetrieval.searchComponents` for further discovery. Do not treat BoK
`sourceId` or `catalogId` as portable provider identities, and do not infer
runtime compatibility from the BoK result.

## Troubleshooting

| Symptom | Check |
|---|---|
| SIE service unavailable | Both CARs are main/available components in the same subsystem |
| Manifest not found | The CNCF resource provider resolves the logical root and canonical manifest path |
| Unsafe reference failure | Every manifest `href` is relative and remains below the source root |
| Duplicate identity failure | Term IDs and component `kind/optional-organization/name` identities are unique within the source |
| Degraded replacement | Inspect SIE provider status; the prior complete BoK generation remains active |
| Candidate missing | Check SIE readiness and source evidence; exact and candidate matching never synthesize data |
| Component lacks usage detail | Hand the exact reference to Textus CBD Support |
| Map is `no-match` | Replace a complete generation that declares the Cozy graph summary, then confirm the selected dataset/source filters |

For public profile selection failures, diagnose the stable code and the
corresponding request or private-binding check. Never retry another profile or
expose private resource references or project registry contents in diagnostics:

| Failure code | Check |
|---|---|
| `invalid-selection` | Confirm `profile` is exactly `official`, `development`, `project`, or `catalog`; `catalog` requires one usable explicit `catalogId`. |
| `project-identity-required` | Add a usable explicit logical `projectId` to a `project` selection. |
| `unregistered` | Have an authorized operator confirm that the exact normalized key has a private binding; do not reveal the registry. |
| `unavailable` | Confirm that the exact binding has an admitted `complete` generation. |
| `stale` | Compare the admitted generation with configured `freshnessGeneration`; a retained older complete generation remains unreadable when it mismatches. |
| `ambiguous` | Correct duplicate normalized bindings or eligible complete generations in private configuration without disclosing their identities. |
| `unauthorized` | Check caller permission for the normalized key; do not reveal whether an unauthorized binding exists. |
| `conflicting-selection` | Ensure `projectId` is used only with `project`, `catalogId` only with `catalog`, direct `datasetId`/`sourceId` assertions equal the resolved binding, and catalog requests supply no `projectId`, `datasetId`, or `sourceId`. |

The Web form invokes the same public read operation and carries no source or
mutation control. Knowledge Map is MCP-ready as a bounded, read-only read, and
`replaceKnowledgeSource` remains a protected administrative operation and is
not MCP-ready.
