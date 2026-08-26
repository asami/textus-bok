# BoK Semantic Retrieval Design

## Scope

DOC-07 projects attributable semantic metadata through the five existing BoK
semantic reads: search, discovery, manifest, resource, and section. The
projection is additive to `SemanticKnowledgeRecord`; it neither changes a
request nor introduces an operation, selector, status, or source-selection
rule.

## Bounded Data Flow

`BokSourceReader` reads only the declared
`component-knowledge-consumer-contract` and `semantic-index` source kinds
through `ExecutionContext.resources`. It verifies their declared digests,
decodes value-only data, and builds `BokSemanticRecord`.
`BokKnowledgeCatalog` selects the already admitted generation and projects each
record into the generated public `SemanticKnowledgeRecord`. MCP-ready semantic
reads, Web, and other component surfaces consume that one typed catalog
projection; MCP readiness adds no authorization or mutation capability.

The semantic index carries optional public `product`, `version`, `profile`,
`owner`, `license`, `logicalPath`, `chunkId`, `publicationGeneration`, and
`publicationDigest` values. They are present only when declared. The reader
accepts nonempty public strings without angle-bracket markup, requires a safe
relative `logicalPath`, and requires a lowercase SHA-256
`publicationDigest`.

## Direct Consumer Sources

Framework-publication evidence supplies `product`, `version`,
`publicationGeneration`, and `publicationDigest` from its publication
`sha256`. Its existing semantic `digest` remains the generated-from
`sourceSha256`. Public Directive evidence supplies `profile` and `version`;
Skill Catalog evidence supplies `owner` and `version`. A Directive or Skill
adds `logicalPath` and `license` only when exactly one contract resource has
the same `logicalIdentity.logicalResource` value. No field is inferred from a
component ID, origin, source, or resource body.

## Boundaries

The reader performs no directory scan, child-file read for metadata,
installation, activation, execution, directive override, or authorization
action. It exposes no body, physical path, credential, permission, or grant.
Stale, forbidden, no-match, ambiguity, bounds, and source-digest behavior are
unchanged.

BoK continues to hand off only exact CAR/SAR existence. Textus CBD Support,
not BoK, independently selects and owns component detail, usage,
compatibility, and review guidance.
