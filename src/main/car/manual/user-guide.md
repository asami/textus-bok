# Textus BoK Operator Guide

## Prerequisites

Deploy `textus-bok` and `textus-semantic-integration-engine` in the same CNCF
subsystem. Configure a CNCF resource provider that resolves the logical root
used by `BokKnowledgeSource.resource`. Keep RDF, Vector, embedding, credentials,
and provider endpoints in the SIE deployment configuration; they are not Textus
BoK inputs.

Use generated Help to confirm the loaded component and operation contract:

```sh
cncf command meta.help textus-bok --format yaml
```

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

## MCP Use

The current baseline intentionally publishes no Textus BoK MCP tools. Phase 6
items P6-30 and P6-31 must complete before the four read operations appear in
`/mcp` discovery. `replaceKnowledgeSource` remains absent permanently.

After migration, use the runtime's MCP `tools/list` result as the authoritative
tool names and schemas. A CAR/SAR deployment may narrow that list. It cannot
broaden it to administration operations.

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
