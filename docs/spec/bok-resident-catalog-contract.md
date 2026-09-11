# BoK Resident Catalog Contract

Status: Phase 10 P10-02 private specification; Phase 10.1 catalog-read boundary extension authoritative

## Scope and authority

This private contract defines the resident in-memory catalog after one or more
independently resolved and admitted BoK generations. It completes the private
backend contract for source-qualified storage and scoped replacement. Profile
resolution, authorization, complete-generation admission, and public read or
MCP contracts remain owned by their existing specifications. The explicit
catalog selector defined in
[the catalog selection contract](bok-catalog-selection-contract.md) is a later
public reader concern; this private storage contract does not claim its runtime
implementation.

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

For a future explicitly selected catalog, the public reader may receive the
independently resolved resident values for every configured member only after
all members pass their existing checks. This does not change resident storage
or replacement: each value remains scoped, complete, atomic, and separately
attributable. No member is a primary resident source.

No semantic merge, precedence, overlay, `sameAs`, cross-profile fallback,
reconciliation, deduplication, or implicit union read is defined by this
contract. A catalog read is not resident enumeration and cannot expose or
mutate source registration, replacement, or private membership.

## Deferred boundaries

Public catalog-backed reads, public enumeration, REST/Web/MCP projection,
source registration, a two-source external runtime, and any generated or CML
surface remain Phase 10.1 or later work. Existing CNCF MCP `tools/call`
remains the only potential read transport; no administrative MCP surface or
second BoK server follows from this contract. This contract changes only
private resident storage and its private replacement seam.
