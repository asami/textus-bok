# BoK Knowledge Backend Design

Status: Phase 10 P10-01/P10-02 stable private design

## Intent

Textus BoK separates profile resolution from generation completion and resident
storage. The first practical path keeps complete selected generations in a
BoK-owned in-memory backend; the existing SIE provider remains an optional
boundary for provider-backed or larger deployments.

```text
private Profile Registry / Resolver
        |
        | exact Phase 7 profile, source, and authorization resolution
        v
Backend Completion
        |
        | source read, normalization, complete-generation admission
        v
Resident Catalog Storage
        |
        +-- InMemoryBoKKnowledgeBackend (default)
        +-- SieBoKKnowledgeBackend (explicit optional provider path)
        |
        v
private catalog consumers
```

The public BoK read and transport surfaces are downstream consumers of this
design. Their catalog-backed integration is deliberately deferred to Phase
10.1.

## Private profile resolution

The profile registry owns the closed `official`, `development`, and `project`
selection vocabulary, authorization, configured source binding, and exact
single-profile resolution. It returns one resolved tuple containing profile,
optional project identity, source, dataset, complete generation, and evidence.

Resolution occurs before backend completion. It never accepts a caller-supplied
resource location or provider setting, and it never searches another profile
after a failure. Catalog composition may combine several independently
resolved tuples only after each tuple has passed this boundary.

## Backend completion

Backend completion reads the resolved private source through CNCF resource
access, normalizes its records, verifies complete-generation invariants, and
returns an atomic candidate. It is the only layer that decides whether a
candidate is complete enough to admit. Incomplete or degraded candidates do
not partially enter resident state.

The default completion implementation is in memory and has no SIE dependency.
When the private configuration explicitly selects SIE, completion delegates to
the retained SIE provider boundary. An unavailable explicit SIE provider is a
deterministic failure; the completion layer must not replace it with memory.

## Resident catalog storage

Resident storage keeps independently admitted generations addressable by their
source/dataset scope. The detailed storage and replacement design is defined in
the [Resident Catalog Design](bok-resident-catalog.md). A successful
replacement swaps one complete generation for the same `(sourceId, datasetId)`
pair and leaves unrelated scopes intact. The stored value retains profile,
optional project, source, dataset, generation, evidence, and source-local
record identity.

The catalog preserves duplicate labels from different sources as distinct
records and applies deterministic source-qualified identity and ordering. It
does not infer semantic equivalence, precedence, overlay, or conflict merge.
The storage boundary is private administrative behavior; it is not a public
mutation or MCP operation.

## Retained SIE provider boundary

SIE remains a provider-backed implementation boundary, not the owner of BoK
profile selection or resident catalog identity. The SIE path receives the same
resolved attribution and complete-generation requirements as the in-memory
path. Backend choice therefore changes storage/provider mechanics only; it
does not change profile meaning, source attribution, generation atomicity, or
replacement scope.

No compatibility fallback is permitted from an explicitly selected SIE path to
memory. Conversely, the default in-memory path does not initialize SIE merely
because SIE is available.

## Phase boundary

This design covers the private backend and resident storage needed by P10-01
and P10-02. Public catalog-backed reads, REST/Web/MCP projection, and the real
SimpleModeling.org plus `bok-knowledgehub` two-source runtime acceptance remain
Phase 10.1 work. Phase 10 does not claim those surfaces or acceptance evidence.
