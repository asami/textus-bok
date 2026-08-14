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
evidence. For example, an official binding can be delivered by component or
SAR configuration:

```yaml
textus:
  bok:
    profile-registry:
      profiles:
        - profile: official
          source:
            sourceId: simplemodeling
            datasetId: simplemodeling-bok
            generation: 2026-07-21T12:00:00Z
            resource: urn:textus:bok:simplemodeling
          evidence:
            uri: https://evidence.example/simplemodeling
            sourceId: simplemodeling
```

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
`cncf.component-repository-index.v1`. Its CAR/SAR entries become existence-only
references. Textus BoK does not read their archives or manuals.

## Replace A Generation

Invoke `Bok.BokRetrieval.replaceKnowledgeSource` through the CNCF component
command path. Use CNCF record path notation for the generated nested datatype:

```sh
cncf command Bok.BokRetrieval.replaceKnowledgeSource \
  --source.sourceId simplemodeling \
  --source.datasetId simplemodeling-bok \
  --source.generation 2026-07-21T12:00:00Z \
  --source.resource urn:textus:bok:simplemodeling \
  --format yaml
```

The input is equivalent to:

```yaml
source:
  sourceId: simplemodeling
  datasetId: simplemodeling-bok
  generation: 2026-07-21T12:00:00Z
  resource: urn:textus:bok:simplemodeling
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

```sh
cncf command Bok.BokRetrieval.searchTerms \
  --query Component --category architecture --limit 10 --format yaml

cncf command Bok.BokRetrieval.explainTerm \
  --term architecture:component --format yaml
```

Use `searchComponentReferences` to discover that a CAR or SAR exists and
`getComponentReference` to resolve an exact name, optional version, and kind.
When CAR and SAR share a name, pass the returned `kind` explicitly.

```sh
cncf command Bok.BokRetrieval.getComponentReference \
  --name textus-account --kind car --format yaml
```

## Read A Knowledge Map

After a complete replacement of a Cozy source that declares
`metadata/rdf/graph.json`, query the bounded factual map through the normal
component boundary:

```sh
cncf command Bok.BokRetrieval.getKnowledgeMap \
  --datasetId knowledgehub --sourceId knowledgehub \
  --nodeLimit 128 --relationshipLimit 256 --format yaml
```

For an operator-facing browser projection, open
`/web/bok/textus-bok/map` on the same runtime. Its filters select the same
public operation; the page keeps complete tables available without JavaScript
and renders CBD handoffs as existence-only identities. It has no replace or
other mutation control.

For the representative Cozy handoff verification, set
`TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT` to the generated `website.d` directory
and run `scripts/run-bok-knowledge-map-sar.sh start`. The probe replaces the
source and proves REST and Web agreement on generation, topology, evidence,
truncation, and `componentRef` handoffs.

## MCP Use

The current component publishes four read tools:

- `Bok.BokRetrieval.searchTerms`;
- `Bok.BokRetrieval.explainTerm`;
- `Bok.BokRetrieval.searchComponentReferences`;
- `Bok.BokRetrieval.getComponentReference`.

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
| Duplicate identity failure | Term IDs and component `kind:name` identities are unique within the source |
| Degraded replacement | Inspect SIE provider status; the prior complete BoK generation remains active |
| Candidate missing | Check SIE readiness and source evidence; exact and candidate matching never synthesize data |
| Component lacks usage detail | Hand the exact reference to Textus CBD Support |
| Map is `no-match` | Replace a complete generation that declares the Cozy graph summary, then confirm the selected dataset/source filters |
