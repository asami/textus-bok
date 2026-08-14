# Textus BoK Operator Guide

## Prerequisites

Deploy `textus-bok` and `textus-semantic-integration-engine` in the same CNCF
subsystem. In configured mode, install private profile bindings and configure a
CNCF resource provider that resolves each binding's logical source root. In
legacy mode (when no profile registry is configured), the provider resolves the
logical root in the request's `BokKnowledgeSource.resource`. Keep RDF, Vector,
embedding, credentials, and provider endpoints in the SIE deployment
configuration; they are not Textus BoK inputs.

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

Every read selects exactly one logical profile. With no `--profile`, the read
uses only `official`; it does not combine configured generations or infer a
project from the host, checkout, or request filters. Use
`--profile development` for the registered development generation. Use
`--profile project --projectId <logical-project-id>` for an exact registered
project generation. A project selector without `projectId`, or a `projectId`
combined with `official` or `development`, is a structured selection failure.
Every successful response contains `selection` attribution for the resolved
profile, optional project identity, dataset, source, generation, and generation
evidence. Keep the record-level evidence when handing an exact term or
component reference to another reader.

```sh
cncf command org.simplemodeling.textus.Bok.BokRetrieval.searchTerms \
  --query Component --category architecture --limit 10 --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.searchTerms \
  --query Component --profile development --limit 10 --format yaml

cncf command org.simplemodeling.textus.Bok.BokRetrieval.explainTerm \
  --term architecture:component --profile project \
  --projectId example-project --format yaml

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

`datasetId` and `sourceId` are compatibility assertions after profile
resolution, not profile selectors. Omit them to use the complete resolved
generation, or supply the exact resolved values to confirm it. For example,
`--profile development --datasetId simplemodeling-bok-development
--sourceId simplemodeling-development` asserts the resolved development
identity; it does not select development. A value from a development or
project generation on an omitted/official request fails as
`conflicting-selection`; it never changes the request to another profile.

For an operator-facing browser projection, open
`/web/bok/textus-bok/map` on the same runtime. Its filters select the same
public operation; the page keeps complete tables available without JavaScript
and renders the selected profile, source, generation, and evidence in its
source-generation table/no-JavaScript output. Omit the profile for official;
choose development explicitly, or choose project with its exact `projectId`.
The form has no resource, registration, replacement, or other mutation
control.

For the representative Cozy handoff verification, set
`TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT` to the generated `website.d` directory
and run `scripts/run-bok-knowledge-map-sar.sh start`. The probe replaces the
source and proves REST and Web agreement on generation, topology, evidence,
truncation, and `componentRef` handoffs.

## MCP Use

The current component publishes four read tools:

- `org.simplemodeling.textus.Bok.BokRetrieval.searchTerms`;
- `org.simplemodeling.textus.Bok.BokRetrieval.explainTerm`;
- `org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences`;
- `org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference`.

Use the runtime's MCP `tools/list` result as the authoritative names and input
schemas. `replaceKnowledgeSource` remains absent permanently. A CAR/SAR
deployment may narrow the tool list but cannot broaden it to private or
administration operations.

For a local Codex endpoint, use the repository-owned
`examples/bok-codex-sar` assembly and `scripts/run-bok-codex-sar.sh`. That SAR
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
| `invalid-selection` | Confirm `profile` is exactly `official`, `development`, or `project` and that the selector shape is valid. |
| `project-identity-required` | Add a usable explicit logical `projectId` to a `project` selection. |
| `unregistered` | Have an authorized operator confirm that the exact normalized key has a private binding; do not reveal the registry. |
| `unavailable` | Confirm that the exact binding has an admitted `complete` generation. |
| `stale` | Compare the admitted generation with configured `freshnessGeneration`; a retained older complete generation remains unreadable when it mismatches. |
| `ambiguous` | Correct duplicate normalized bindings or eligible complete generations in private configuration without disclosing their identities. |
| `unauthorized` | Check caller permission for the normalized key; do not reveal whether an unauthorized binding exists. |
| `conflicting-selection` | Ensure `projectId` is used only with `project`, and any `datasetId`/`sourceId` assertions equal the resolved binding. |

The Web form invokes the same public read operation and carries no source or
mutation control. Knowledge Map remains outside MCP, and
`replaceKnowledgeSource` remains a protected administrative operation and is
not MCP-ready.
