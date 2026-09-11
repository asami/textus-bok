# BoK Catalog Selection Contract

Status: Phase 10.1 authoritative specification; runtime and CML realization implemented by P10-03C

## Scope

This specification defines the additive public contract for selecting a
private, resident multi-source BoK catalog. It preserves the Phase 7 direct
profile contract: `official`, `development`, and `project` remain exact,
source-isolated selections with their existing request shapes and complete
`BokResolvedSelection` response form.

P10-03C realizes this contract in the selector, private configuration decoder,
CML/generated projection, and resident runtime. P10-04 alone remains
responsible for real two-source runtime acceptance.

The terms `MUST`, `MUST NOT`, `SHOULD`, and `MAY` describe normative behavior.

## Closed Catalog Request

The sole aggregate request is the closed selector `profile = catalog` together
with one required logical `catalogId`. `catalogId` is an opaque configured key;
it is never a URL, resource reference, filesystem path, repository, provider
setting, or caller-supplied member list.

The request shape is closed:

| Request form | Required outcome |
| --- | --- |
| `profile = catalog` with a valid `catalogId` | Resolve the configured catalog only. |
| `profile = catalog` without a usable `catalogId` | `invalid-selection`. |
| `profile = catalog` with `projectId`, `datasetId`, or `sourceId` | `conflicting-selection`. |
| omitted `profile`, `official`, `development`, or `project` | Preserve the existing Phase 7 direct-profile request and response contract. |

`catalog` is not an implicit union produced by omitting a selector, naming a
dataset/source filter, or selecting any direct profile. A direct request does
not gain fallback, precedence, overlay, or aggregate behavior.

## Private Membership

Only private `textus.bok.profile-registry` configuration owns a `catalogs`
collection. A catalog entry has one logical `catalogId` and a member collection
of nonempty, unique, existing direct profile keys. A member key identifies an
already configured `official`, `development`, or exact `project` profile key;
it is not a nested catalog.

Callers cannot enumerate catalogs or members, add or remove members, submit a
resource, path, URL, or repository, or assign a priority. Membership is
inclusion-only: its declaration supplies no precedence and does not authorize
deduplication, overlay, semantic merge, or reconciliation.

## Resolution and Admission

Catalog resolution first validates the catalog request and authorizes the
requested catalog without disclosing private configuration. It then resolves
every configured member independently through the existing direct-profile
sequence:

1. authorize the exact member key;
2. locate its exact private registration;
3. require its admitted complete generation; and
4. apply its existing freshness and direct-selection compatibility rules.

Every configured member MUST pass these checks. A failure for any member,
including unauthorized, unregistered, unavailable, stale, ambiguous, or
conflicting state, fails the requested catalog with the existing structured
outcome. A catalog MUST NOT return a partial result, omit a failed member, use
a fallback generation, or probe a different profile.

The registry and catalog MUST NOT disclose a member resource reference or the
identity of unrelated bindings in success or failure handling.

## Selection Attribution

The response field remains named `selection`.

- A direct selection retains the complete existing `BokResolvedSelection`:
  `resolvedProfile`, optional direct `projectId`, `datasetId`, `sourceId`,
  `generation`, and attributable generation evidence.
- A catalog selection has `resolvedProfile = catalog`, the requested
  `catalogId`, and a deterministic complete vector of member attributions.
  Each member attribution is the complete direct resolved selection for that
  member.

A catalog selection MUST NOT fabricate a top-level `datasetId`, `sourceId`,
`generation`, or evidence tuple, because no member is a primary source. The
P10-03C CML realization makes those single-source fields conditional by
selection form; it does not invent a primary member.

Member attributions are deterministic and source-qualified. They are ordered
by their resolved `(sourceId, datasetId, generation)` identities; ties are a
configuration error rather than an opportunity to choose precedence.

## Result Semantics

Catalog records retain their source-qualified identities and evidence.
Operation-specific exact, ambiguous, conflict, insufficient-evidence, and
no-match classification sees the records from every successfully resolved
member after all-member admission. Classification does not deduplicate equal
local identities, merge terms or topology, overlay values, assign source
precedence, or perform semantic reconciliation.

Result ordering remains deterministic with the existing source-qualified key
`(sourceId, datasetId, localIdentity...)`. Equal local labels or IDs from
different sources remain distinct and attributable.

## Transport and Operating Boundaries

Existing CNCF MCP `tools/call` is the sole public transport projection for
catalog-aware read operations. This contract introduces neither a second
BoK-specific server nor an MCP surface for catalog registration, replacement,
reload, administration, or configuration. All such administration remains
non-MCP-ready.

The default resident in-memory terminology, component, and map read path does
not require SIE. Optional semantic-provider composition remains a separate
responsibility and cannot choose, add, remove, or otherwise select a catalog
member.

## Compatibility and Non-goals

- Omitted `profile` continues to mean direct `official`; it never means
  `catalog`.
- Direct `official`, `development`, and `project` preserve their Phase 7
  isolation, compatibility filters, structured failures, and successful
  selection form.
- This contract defines no public catalog enumeration, caller-owned catalog,
  source discovery, source mutation, semantic federation policy, component
  detail behavior, or deployment procedure.
- Runtime implementation, CML/generated model changes, configuration changes,
  and executable specifications are realized by P10-03C.
