# Resident Multi-BoK Catalog Proposed Specification

Status: proposal
Phase: Phase 8

## Purpose

Define the implementation direction for Phase 8 before promoting stable behavior into `docs/design` or `docs/spec`.

Phase 8 makes Textus BoK usable as a resident knowledge service that can load multiple attributable BoK generations, keep them in memory without requiring SIE, and expose the resulting catalog through the existing BoK read operations and CNCF MCP projection.

## Architectural Direction

```text
Profile Registry / Resolver
        |
        | resolves each configured source/profile
        v
Selected Generation(s)
        |
        v
Resident BoK Catalog
        |
        +-- InMemory backend   (Phase 8 default)
        |
        +-- SIE backend        (optional / future scale path)
        |
        v
BoK Read Semantics
        |
        +-- REST / Web where applicable
        +-- existing MCP-ready BoK operations
```

Phase 8 does not replace Phase 7 profile selection. Phase 7 selects an attributable generation for a profile. Phase 8 adds a catalog layer after that selection and allows multiple independently selected generations to participate in one resident read space.

## P8-01: Knowledge Backend Boundary

Introduce a BoK-owned storage/retrieval boundary so ordinary BoK read semantics do not require SIE directly.

Conceptually:

```text
BoKKnowledgeBackend
  +-- InMemoryBoKKnowledgeBackend
  +-- SieBoKKnowledgeBackend
```

Exact names are implementation details until the boundary is proven.

The in-memory implementation must support at least:

- complete generation admission;
- generation replacement without stale records;
- deterministic lookup and search;
- source/dataset/generation/evidence attribution;
- multiple simultaneously resident selected generations;
- deterministic reset/reload behavior for tests and local operation.

The SIE implementation remains available as the scale-oriented/provider-backed path. Phase 8 must not delete the existing SIE contract merely to make memory operation possible.

## P8-02: Resident Catalog Semantics

A Resident BoK Catalog is a read-oriented union of independently selected complete generations.

The catalog must preserve origin identity. Two entries with the same term name from different sources remain distinct records unless an explicit future semantic relation states otherwise.

A stable identity therefore includes attributable source-local identity, conceptually:

```text
catalog entry identity = source identity + local knowledge identity
```

Search may return multiple matching records. Phase 8 must not silently collapse them by label.

The catalog must retain enough attribution to explain:

- selected profile;
- source;
- dataset;
- generation;
- evidence;
- local term/component identity.

Phase 8 does not define `sameAs`, ontology reconciliation, semantic conflict resolution, precedence, or write-back between BoKs.

## Relationship to Phase 7

Phase 7 established request-scoped profile isolation and deliberately prohibited cross-profile union within that selection contract.

Phase 8 does not weaken that rule. Instead it introduces a higher-level composition:

```text
source/profile A -> resolve -> selected generation A --+
                                                    |
source/profile B -> resolve -> selected generation B --+--> Resident Catalog
```

Selection remains deterministic and attributable before catalog composition occurs.

A caller must not be able to use the catalog to bypass profile admission, authorization, project identity, or complete-generation checks.

## P8-03: Read and MCP Boundary

Reuse existing Textus BoK domain read semantics and MCP-ready operations wherever their response models can represent catalog results correctly.

The intended flow is:

```text
Resident BoK Catalog
        -> BoK-owned read/classification semantics
        -> generated Component Service/Operation
        -> CNCF MCP projection
```

Do not create a separate BoK-specific MCP transport or tool registry when the CNCF projection already provides the required server surface.

Administrative source registration, generation replacement, backend selection, reload, and mutation remain non-MCP-ready unless a later Phase explicitly changes that policy.

## P8-04: Representative Driver

The Phase 8 representative acceptance uses two real knowledge sources:

1. the SimpleModeling.org / `simplemodeling-org` glossary generation;
2. `bok-knowledgehub` generated knowledge.

Both must be admitted into the same Textus BoK process using the in-memory backend and observed through one resident catalog.

Acceptance must prove at least:

- each source can be loaded independently;
- both remain simultaneously resident;
- a query can return attributable results from either source;
- same-label results are not accidentally collapsed;
- existing MCP publication returns catalog-backed results;
- SIE is not required for the representative path.

## Cozy Boundary

Cozy already owns generation/finalization of BoK source artifacts, including glossary and KnowledgeSource-related outputs. Phase 8 consumes those existing outputs.

Do not add a Cozy prerequisite Phase merely to start Phase 8. If representative consumption proves that an existing Cozy output lacks required stable information, record the exact consumer evidence and hand the smallest generic correction back to Cozy.

## CNCF Boundary

CNCF already owns Component/Service/Operation runtime and MCP projection. Phase 8 uses that existing surface.

Do not add a CNCF prerequisite Phase merely to start Phase 8. If the E2E path exposes a generic runtime or MCP limitation, record reproducible evidence and create a bounded CNCF handoff rather than implementing framework behavior privately in Textus BoK.

## Backend Evolution

The intended scale path is:

```text
small / local knowledge
    -> InMemory backend

larger / provider-backed knowledge
    -> SIE backend
```

Backend choice must not change BoK domain identity, source attribution, or public read meaning. Backend-specific ranking or provider candidates may remain advisory where existing Textus BoK semantics already define that boundary.

## Deferred Work

The following are explicitly deferred unless Phase 8 implementation evidence makes one unavoidable:

- embedding search;
- RDF reasoning;
- `sameAs` inference;
- ontology reconciliation;
- distributed catalog synchronization;
- persistent cache design;
- watch-based source refresh;
- incremental remote synchronization;
- write-back to source BoKs;
- new Web UI;
- Flutter/UI work;
- new MCP transport design.

## Promotion Rule

This document is a proposal, not normative authority.

After P8-01/P8-02 implementation proves the backend and catalog boundaries, stable semantics should be promoted selectively into `docs/design` and/or `docs/spec`. Implementation discoveries and changed assumptions must be recorded in the Phase 8 journal before promotion.
