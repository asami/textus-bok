# BoK Domain Model Contract

## Purpose

`textus-bok` owns the typed vocabulary used to interpret BoK terminology and
CAR/SAR existence knowledge. SIE transports provider-neutral documents,
assertions, evidence, and provenance; it does not own these BoK meanings.

## Terminology

A `BokTerm` has a stable BoK identity, title, definition, optional category,
term type, source-owned dataset identity, and attributable `BokEvidence`.
Search and explanation results add match kind, score, and rationale without
changing the curated term.

## Component Existence

A `ComponentReference` proves only that a named CAR or SAR exists. Its stable
identity is `(kind, organization: Option[String], name)`; `None` means an
unqualified legacy identity, while `Some` names the exact organization
namespace. Version is a selector and deterministic ordering tie-breaker, not
identity. It may carry organization, catalog, source, and version metadata plus evidence. It must not
carry capabilities, dependencies, operations, runtime compatibility, manuals,
or usage guidance; those details belong to `textus-cbd-support`.

## Source And Evidence

`BokKnowledgeSource` identifies one logical resource and generation owned by a
dataset. `BokSourceReader` interprets the resource value only through the CNCF
resource DSL. `BokEvidence` attributes a result to its logical source and URI
and may record source version, observed time, and freshness.

## Requests And Responses

The component defines typed request and response models for source replacement,
term search and explanation, component-reference search and lookup, and the
factual Knowledge Map query. Query responses use explicit status,
evidence-bearing result types, and typed warnings. MCP readiness is
operation-scoped: the four terminology/component reads are ready, while source
mutation and the public Knowledge Map query remain outside the MCP catalog.

The five semantic reads use `SemanticKnowledgeRecord` as their shared
value-only output. Its direct and semantic-index projection contract is
normatively defined in [Semantic Retrieval Contract](bok-semantic-retrieval-contract.md).
Optional semantic fields are attributable metadata; they do not turn an
existence handoff into CBD detail or usage guidance.

A `BokKnowledgeMapNode` retains zero or more `ComponentReference` values only
when Cozy explicitly declares `componentRef.kind` and `componentRef.name` and
they exactly match a CAR/SAR index entry in the same selected generation; a
declared `componentRef.organization` or `componentRef.version` must also match
that entry exactly. This
is a portable existence handoff; a node ID, title, label, tag, or candidate
score never produces a component reference.

## Exclusions

This contract does not implement MCP publication or CBD detail resolution. It
exposes no Fuseki, Chroma, embedding, filesystem, or network type.

## Resource Normalization

`BokSourceReader` resolves `metadata/cncf/knowledge-source.json` from the
logical `BokKnowledgeSource.resource` root and reads every child exclusively
through `ExecutionContext.resources`. The v1 manifest recognizes structured
`glossary-terms`, CNCF `component-repository-index`, and the paired digest-bound
`component-knowledge-consumer-contract` and `semantic-index` resources. The
latter two are manifest children read through `ExecutionContext.resources`;
their hrefs must be safe relative paths, and each declared SHA-256 value is
verified before semantic metadata is admitted.

Glossary metadata becomes typed `BokTerm` values. The CNCF repository codec
validates `repository/catalog/index.json` as
`cncf.component-repository-index.v2`, and its CAR/SAR entries become
existence-only `ComponentReference` values whose evidence identifies the
canonical catalog resource. Results are sorted by `(kind,
unqualified-before-qualified, organization, name, version)` and duplicate
`(kind, optional organization, name)` identities fail. No rendered page,
CAR/SAR archive, host file, or direct network resource is opened by the
normalizer.

## Federation Publication

`replaceKnowledgeSource` maps each normalized term and component reference to
one provider-neutral SIE document and assertion. Evidence resources are
deduplicated by stable attributable identity, and deterministic SHA-256 IDs
connect every document and assertion to its evidence. BoK meaning remains in
opaque structured metadata interpreted by `textus-bok`; it does not enter the
SIE CML domain.

Publication resolves the `SemanticIntegrationEngine` component in the current
subsystem and invokes the generated `KnowledgeFederation.replaceDataset`
operation. Absence of that component is a structured service failure. There is
no direct provider or SIE runtime fallback. `project.yaml` declares both the
exact development JVM coordinate and the CAR ABI range.

## Matching And Replacement

`BokKnowledgeCatalog` owns typed BoK generations. A generation becomes visible
only after SIE reports complete provider-neutral publication; degraded
publication retains the previous complete generation. Complete replacement
removes stale terms and component references from the BoK view.

Exact matches are determined from BoK-owned identities and titles. Semantic
candidates are accepted only from generic SIE query document IDs and scores,
scoped by dataset and source identity, and remain marked `candidate`. Retrieval
overfetches bounded provider pages until domain filtering satisfies the caller's
limit or the provider result set is exhausted. Classification occurs before
result limiting, so a limit cannot hide ambiguity or conflicting definitions.
Missing definitions or evidence remain `insufficient-evidence`, and unknown
identities remain `no-match` without synthetic knowledge.
