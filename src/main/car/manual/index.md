# Textus BoK Reference Manual

## Purpose

Textus BoK interprets Body of Knowledge metadata as typed terminology and
existence-only CAR/SAR references. It publishes provider-neutral documents to
Textus Semantic Integration Engine (SIE), then interprets generic retrieval
evidence as BoK-specific matches.

SIE owns federation and RDF/Vector provider isolation. Textus BoK owns term and
component-reference meaning. Textus CBD Support owns detailed component usage,
dependencies, operations, compatibility, manuals, and reuse guidance.

## Composition

The `Bok` component requires a `SemanticIntegrationEngine` component in the
same CNCF subsystem. Calls fail with structured `service-unavailable` behavior
when SIE is absent. Textus BoK does not connect directly to Fuseki, Chroma, an
embedding provider, the host filesystem, or the network.

## Service Contract

`BokRetrieval` provides five operations:

| Operation | Kind | Purpose | MCP policy |
|---|---|---|---|
| `replaceKnowledgeSource` | command | Replace one complete source-owned generation | never ready |
| `searchTerms` | query | Search typed terminology | ready |
| `explainTerm` | query | Resolve one exact term and reliability state | ready |
| `searchComponentReferences` | query | Search existence-only CAR/SAR references | not ready yet |
| `getComponentReference` | query | Resolve one exact CAR/SAR identity | not ready yet |

CAR or SAR configuration may disable an MCP-ready operation but may never make
`replaceKnowledgeSource` or another undeclared operation visible through MCP.

## Knowledge Source

`replaceKnowledgeSource.source` contains `sourceId`, `datasetId`, `generation`,
and a logical CNCF `resource` reference. The normalizer resolves
`metadata/cncf/knowledge-source.json` below that root. Schema
`cncf.knowledge-source.v1` recognizes:

- `glossary-terms`, containing structured term identities, titles,
  definitions, optional category, and term type;
- `component-repository-index`, using the canonical CNCF repository index for
  CAR/SAR existence.

Child references must be safe relative paths. Metadata-only operation is the
default; rendered HTML and CAR/SAR archive inspection are not required.

## Replacement Semantics

The command publishes one complete generation through SIE
`KnowledgeFederation.replaceDataset`. The typed BoK generation becomes visible
only when SIE reports `complete`. A complete later generation removes stale
terms and component references. A degraded publication leaves the previous
complete typed generation visible.

## Query Semantics

Exact identity and title matches are classified by Textus BoK. Semantic
candidates use generic SIE document IDs and scores scoped by dataset and source
identity. They remain `candidate` and are never promoted to curated facts.

Term responses use `matched`, `ambiguous`, `conflict`,
`insufficient-evidence`, or `no-match`. Component-reference queries return only
identity, kind, optional version/source/catalog/organization, and attributable
evidence. Result limiting occurs after reliability classification.

## Failures And Limits

- Invalid manifests, unsafe child references, unsupported schemas, and
  duplicate identities are structured resource failures.
- Missing SIE composition is a structured service failure.
- Unknown identities return `no-match` without generated knowledge.
- Semantic retrieval is bounded to at most 100 provider results per source.
- Detailed component capability or compatibility questions must be handed to
  Textus CBD Support.

See the [operator guide](user-guide.md) for ingestion and troubleshooting. The
repository developer guide defines projection and extension rules.
