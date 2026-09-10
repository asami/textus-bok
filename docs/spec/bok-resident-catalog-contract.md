# BoK Resident Catalog Contract

Status: Phase 10 P10-02 authoritative private specification

## Scope and authority

This private contract defines the resident in-memory catalog after one or more
independently resolved and admitted BoK generations. It completes the private
backend contract for source-qualified storage and scoped replacement. Profile
resolution, authorization, complete-generation admission, and public read or
MCP contracts remain owned by their existing specifications.

The catalog is an administrative storage boundary. It is not a source
registration, reload, mutation, public enumeration, REST/Web, or MCP API.

## Resident scope and attribution

Each resident generation is stored under the deterministic scope
`(sourceId, datasetId)`. The stored value is one immutable normalized
generation together with its optional resolved-profile attribution:

- resolved profile;
- optional project identity;
- source identity;
- dataset identity;
- generation identity; and
- generation evidence.

The attributed replacement path MUST validate that all source, dataset, and
generation identities in the supplied `ResolvedBokProfile` exactly match the
normalized source. A mismatch MUST return rejection and MUST leave every
resident scope unchanged. The compatibility-only no-attribution commit seam
uses the same scoped map but is not an admission or public read path.

## Completion and replacement

Only a publication whose normalized state is `complete` may replace resident
state. Replacement atomically swaps the complete value for its exact
`(sourceId, datasetId)` scope. A replacement MUST preserve every unrelated
scope, including scopes with the same dataset or equal local term labels.
Incomplete, rejected, and mismatched-attribution candidates MUST NOT mutate
resident state.

## Exact selection and deterministic identity

Catalog-backed selection retains Phase 7's exact source, dataset, and
generation match. It MUST NOT fall back to another profile, source, dataset,
or generation. Terms, components, topology nodes, and topology relationships
retain source-qualified ordering and identity so equal local labels and IDs
remain distinct. Ordering uses `(sourceId, datasetId, localIdentity...)`.

No semantic merge, precedence, overlay, `sameAs`, cross-profile fallback,
reconciliation, or union read is defined by this contract.

## Deferred boundaries

Public catalog-backed reads, public enumeration, REST/Web/MCP projection,
source registration, a two-source external runtime, and any generated or CML
surface remain Phase 10.1 or later work. This contract changes only private
resident storage and its private replacement seam.
