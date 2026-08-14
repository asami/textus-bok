# Textus BoK Developer Guide

## Ownership Boundary

Keep BoK source interpretation, typed terms, existence-only component
references, match classification, and BoK response construction in this CAR.
Use SIE only through generated component APIs:

- `KnowledgeFederation.replaceDataset` for complete dataset publication;
- `SemanticRetrieval.query` for provider-neutral candidate evidence.

Do not import SIE runtime or provider SPI packages. Do not add Fuseki, Chroma,
embedding, retry, endpoint, credential, filesystem, or network types to CML.

## Generated And Handwritten Code

`src/main/cozy/textus-bok.cml` is the public component contract. Cozy-generated
Scala remains under `target/scala-3.3.8/src_managed/main/scala` and is never
edited. `ComponentFactory` replaces generated ActionCalls with handwritten
programs. Runtime classes normalize sources, project SIE documents, retrieve
generic candidates, and classify BoK results.

## Source Adapter Rules

All content reads must start from `BokKnowledgeSource.resource` and use
`ExecutionContext.resources`. Resolve child paths with the CNCF resource DSL.
New resource kinds require:

1. an explicit schema and safe-relative-reference rule;
2. deterministic normalization into existing or deliberately revised CML
   types;
3. duplicate-identity and empty-source behavior;
4. metadata-only executable specifications;
5. no direct filesystem, URL client, environment, or system-property access.

Rendered HTML is not a BoK default. Generic optional HTML indexing remains an
SIE administration capability.

The Knowledge Map admits only Cozy's versioned `rdf-graph-summary` resource.
Graph edges remain factual source topology, every node and edge retains source
evidence, and a component handoff can originate only from an explicit
same-generation `componentRef` validated against the CAR/SAR index. Optional
organization and version are exact constraints; never infer an identity from a
label, identifier, tag, edge, or rendered page.

## Federation Projection

`BokFederationPublisher` maps typed records to opaque SIE metadata. Stable IDs
must remain deterministic and use `(kind, optional organization, name)` as the
component identity. Qualified component document/assertion IDs and assertion
subjects include the organization; an unqualified (`None`) component retains
the legacy ID and subject bytes exactly. Every document and assertion must
retain attributable evidence. Component document metadata includes `datasetId`,
`domain`, and `recordKind` so candidate attribution survives provider retrieval.

Commit the typed catalog only after SIE returns `complete`. Never advance it on
`degraded`, `unavailable`, or failed publication. Replacement is a complete
generation, not an incremental patch.

## Matching Rules

Textus BoK owns exact, candidate, ambiguous, conflict,
insufficient-evidence, and no-match classification. SIE scores are advisory.
Key candidates by dataset, source, and document identity. Filter unrelated
generic results before applying the caller's result limit, using bounded
overfetch up to 100 results per source.

Component lookup identity is `(kind, organization: Option[String], name)`.
Supply organization for an exact namespace match; omitting it spans
organizations and must return no reference when the result is ambiguous.
Version narrows or orders a match but never changes the stable identity.

Do not convert similarity into curated knowledge. Perform reliability
classification before truncating results. Preserve evidence on every returned
term or component reference.

## MCP Policy

MCP publication is default-deny. The component currently marks only
the four BoK read operations ready at operation level: terminology search and
explanation plus existence-only component-reference search and lookup.
`replaceKnowledgeSource` and future mutation or administration operations must
remain non-ready at both service and operation policy levels. CAR/SAR
configuration may only disable declared tools through `cncf.mcp.enabled`,
`cncf.mcp.disabled-services`, and `cncf.mcp.disabled-operations`; configuration
must never be treated as an operation readiness declaration.

## CBD Handoff

Keep `ComponentReference` existence-only. Changes that add capabilities,
dependencies, services, operations, compatibility ranges, manuals, artifacts,
or usage guidance belong in Textus CBD Support. The portable handoff identity
is `name`, `kind`, optional organization/version, and evidence URI; local source
and catalog IDs are contextual evidence only.

For an admitted Cozy graph node, accept a CBD handoff only from explicit
`componentRef.kind` and `componentRef.name` after exact same-generation
CAR/SAR-index validation; if declared, `componentRef.organization` and
`componentRef.version` must match the same index entry exactly. Do not recover a handoff from a node ID, label,
title, tag, relationship, or SIE candidate. Web pages may display that
existence identity but must not invoke CBD Support or render CBD-owned detail.

## Knowledge Map Web Surface

The `textus-bok` Static Form page at `/web/bok/textus-bok/map` selects the
public `bok.bok-retrieval.get-knowledge-map` operation. Keep its query bounded
and read-only. The page's operation result is the sole data source for both
the no-JavaScript tables and local progressive SVG enhancement. Runtime JSON
uses generated snake_case property names, so authored tables and JavaScript
must accept those names while request parameters remain the generated CML
selectors. Use `textContent`, not HTML sinks, and permit evidence navigation
only for explicit HTTP(S) URLs.

## Verification

Run:

```sh
sbt --batch test cozyBuildCAR
cozy lint car .
TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT=/path/to/website.d \
  scripts/run-bok-knowledge-map-sar.sh start
```

Executable specifications must cover normalization failures, complete and
degraded replacement, stale removal, exact/candidate distinction, every
reliability state, source-scoped attribution, bounded overfetch, CML surface,
and MCP deny-by-default behavior.
