# BoK Semantic Retrieval Contract

Status: normative specification

## Public Record

All five existing semantic reads return `SemanticKnowledgeRecord` values. In
addition to their existing identity, evidence, digest, status, and optional
component-reference values, a record may carry these optional strings:
`product`, `version`, `profile`, `owner`, `license`, `logicalPath`, `chunkId`,
`publicationGeneration`, and `publicationDigest`. Absent source values remain
absent; a response must never fabricate an optional value.

Structured `cncf.semantic-index.v1` records accept a declared optional value
only when it is a nonempty public string with no `<` or `>`. A declared
`logicalPath` must be a safe relative logical path, and a declared
`publicationDigest` must match lowercase `[0-9a-f]{64}`. Malformed, empty, or
forbidden metadata rejects the semantic resource. The reader does not read a
child resource merely to acquire a projection field.

## Direct Contract Projection

`frameworkPublication` preserves product, version, publication generation,
and its publication `sha256` as `publicationDigest`; `digest` remains the
separate `sourceSha256` evidence. `publicDirective` preserves profile and
version. `skillCatalog` preserves owner and version. A matching public
contract resource contributes only its declared logical path and license, and
only when `logicalIdentity.logicalResource` matches exactly. No match leaves
both values absent.

No direct source synthesizes product, profile, owner, license, path, version,
chunk, publication, or any authority from an origin, component identifier, or
other non-declared value.

## Retrieval And Authority Boundary

This contract does not change request shapes, operation names, profile/source
selection, MCP inventory, status meanings, stale behavior, forbidden
withholding, no-match, ambiguity, or result bounds. Every result remains
value-only: no source body, physical path, credential, permission,
installation, activation, directive override, MCP grant, execution, or CBD
detail/usage/compatibility/review data is returned.

BoK supplies an exact existence handoff only. CBD Support separately selects
and owns detail and usage guidance.

## Executable Specifications

`org.simplemodeling.textus.bok.ComponentKnowledgeRetrievalSpec` proves the
digest-bound ResourceAccess-to-catalog projection of all nine optional fields.
`org.simplemodeling.textus.bok.BokSemanticRetrievalSpec` proves direct
framework/directive/skill preservation, absence behavior, stale/forbidden
withholding, and bounded semantic retrieval. The focused suite also retains
`BokCbdComponentReferenceHandoffSpec` and `BokMcpProjectionSpec` for the
cross-component handoff and public projection boundary.
