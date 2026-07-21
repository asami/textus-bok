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

## Federation Projection

`BokFederationPublisher` maps typed records to opaque SIE metadata. Stable IDs
must remain deterministic. Every document and assertion must retain attributable
evidence. Component document metadata includes `datasetId`, `domain`, and
`recordKind` so candidate attribution survives provider retrieval.

Commit the typed catalog only after SIE returns `complete`. Never advance it on
`degraded`, `unavailable`, or failed publication. Replacement is a complete
generation, not an incremental patch.

## Matching Rules

Textus BoK owns exact, candidate, ambiguous, conflict,
insufficient-evidence, and no-match classification. SIE scores are advisory.
Key candidates by dataset, source, and document identity. Filter unrelated
generic results before applying the caller's result limit, using bounded
overfetch up to 100 results per source.

Do not convert similarity into curated knowledge. Perform reliability
classification before truncating results. Preserve evidence on every returned
term or component reference.

## MCP Policy

MCP publication is default-deny. P6-30 and P6-31 may make only the four read
queries ready. `replaceKnowledgeSource` and future mutation or administration
operations must remain non-ready at both service and operation policy levels.
CAR/SAR configuration may only disable declared tools.

## CBD Handoff

Keep `ComponentReference` existence-only. Changes that add capabilities,
dependencies, services, operations, compatibility ranges, manuals, artifacts,
or usage guidance belong in Textus CBD Support. The portable handoff identity
is `name`, `kind`, optional organization/version, and evidence URI; local source
and catalog IDs are contextual evidence only.

## Verification

Run:

```sh
sbt --batch test cozyBuildCAR
cozy lint car .
```

Executable specifications must cover normalization failures, complete and
degraded replacement, stale removal, exact/candidate distinction, every
reliability state, source-scoped attribution, bounded overfetch, CML surface,
and MCP deny-by-default behavior.
