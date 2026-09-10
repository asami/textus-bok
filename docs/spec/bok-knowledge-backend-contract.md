# BoK Knowledge Backend Contract

Status: Phase 10 P10-01 authoritative private specification

## Scope and authority

This private contract defines the BoK-owned backend boundary used after
Phase 7 profile resolution and before the existing admission/catalog-commit
sequence. It makes the in-memory backend the default path for Textus BoK and
retains the existing optional SIE provider boundary. The P10-01 private
backend completes the normalized selected source and then uses that unchanged
sequence; it does not define multi-source resident storage. Source/dataset-
qualified catalog composition is normative P10-02 work. This contract does
not change the Phase 7 public profile selection contract, public operation
signatures, or MCP readiness.

The profile registry remains the authority for selecting a logical profile and
its configured source. The private backend completes one explicitly selected
generation, after which the existing admission/catalog-commit sequence remains
the authority for admission and catalog visibility. A caller cannot use
backend choice or catalog membership to bypass authorization, project
identity, source binding, or complete-generation admission.

## Backend selection

The backend choice is private operational state. When no backend is explicitly
selected, Textus BoK MUST use its BoK-owned in-memory backend. The default path
MUST NOT require SIE or another external provider.

An explicit SIE selection MUST use the retained SIE provider boundary. If that
provider is unavailable, misconfigured, or cannot be initialized, startup or
replacement MUST return one deterministic structured `unavailable` outcome.
It MUST NOT silently select the in-memory backend, another provider, or a
foreign generation. An explicit memory selection has the same semantics as the
default path and does not initialize SIE merely to complete a generation.

Backend selection is not a public read selector. Public requests MUST NOT
select a backend, provider, resource location, credential, or runtime
configuration.

### Private configuration grammar

The private operational configuration root is `textus.bok.knowledge-backend`.
Its value MUST be an object with exactly one member, `backend`, whose value is
one of the exact strings `memory` or `sie`:

```yaml
textus:
  bok:
    knowledge-backend:
      backend: memory
```

Absence of the root and an explicit `memory` value select the BoK-owned
in-memory backend. An explicit `sie` value selects the retained SIE provider
boundary. A non-object root, a missing or non-string `backend` member, an
unknown value, an extra object member, or both direct and nested
representations of the root MUST fail as invalid private configuration.
This setting is component configuration only; it is not an action, MCP, or
public request parameter.

## Complete-generation admission

The backend receives one normalized candidate from a private profile binding
and completes that selected source. The source is read only through the
configured CNCF resource-access boundary. The existing admission/catalog-
commit sequence MUST then validate that the candidate contains a complete
generation and its required attribution:

- the normalized profile and optional project identity;
- one source identity and one dataset identity;
- one generation identity;
- attributable generation evidence; and
- the complete set of generation records required by the BoK domain contract.

Terms, component references, map nodes, relationships, assertions, vector
documents, and generation evidence form one atomic generation. A candidate
that is incomplete, degraded, malformed, rejected, or otherwise not
`complete` MUST leave the resident state unchanged. A prior complete
generation MAY remain available for rollback safety, but it MUST NOT be
reported as the new generation.

Admission is independent for each resolved profile. Phase 7's exact
single-profile selection and no-fallback isolation remain in force: a failed
selection or admission never probes, imports, or substitutes another profile.

## Source/dataset-scoped replacement (P10-02 catalog composition)

P10-01 does not deliver source/dataset-scoped replacement or multi-source
resident catalog behavior. The following requirements are normative P10-02
catalog-composition work, not P10-01 delivered behavior.

An admitted generation is replaced by the pair `(sourceId, datasetId)`. A
successful complete replacement for that pair atomically removes the previous
generation for the same pair and installs the new generation as one unit. It
MUST preserve the new generation's complete records and attribution together.

Replacement MUST NOT remove, rebuild, reorder, or otherwise mutate generations
whose source or dataset identity is outside the replacement pair. A candidate
with a conflicting source/dataset identity is not an append or implicit merge;
it is a structured conflict or unavailable result according to the calling
boundary. Repeating an unchanged generation is deterministic and MUST NOT
duplicate terms, component references, assertions, or vector documents.

The resident backend MAY retain several independently resolved generations at
once. Equal local labels from different source/dataset scopes remain distinct
records. Source-local identity and deterministic ordering are part of the
backend result, not a semantic merge or precedence rule.

## Attribution

Every admitted generation and every backend result MUST carry, or remain
reconstructible from, the following attribution:

- resolved profile;
- project identity when the profile is `project`;
- source identity;
- dataset identity;
- generation identity; and
- generation evidence.

Backend choice MUST NOT alter these identities or their meaning. Domain record
evidence remains in addition to generation-level selection attribution.

## Boundary and deferred public integration

The P10-01 backend, admission, and catalog-commit operations are private
administrative behavior. P10-02 source/dataset-scoped replacement and resident
multi-source storage are also private administrative behavior; they do not add
source registration, replacement, reload, backend-selection, or mutation
operations to MCP.

Public catalog-backed reads, REST/Web/MCP projection of resident catalog
results, and real two-source runtime acceptance remain Phase 10.1 work. This
contract establishes the private backend semantics needed by that later work;
it does not claim that those public or runtime acceptance surfaces are complete.

## Non-goals

- no change to Phase 7 selection, authorization, or no-fallback isolation;
- no removal, redesign, or semantic expansion of SIE;
- no cross-profile union during profile resolution;
- no semantic reconciliation, `sameAs`, precedence, overlay, embeddings,
  write-back, or distributed synchronization;
- no public backend or source-registration API.
