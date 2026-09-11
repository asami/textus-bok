# BoK Resident Catalog Design

Status: Phase 10 P10-02 stable private design; Phase 10.1 catalog-read boundary extension

## Intent

The resident catalog stores independently resolved and admitted complete BoK
generations without requiring semantic composition. The profile registry
remains the authority for selection and admission; the catalog only retains
the resulting immutable generation after complete publication. The P10-03C
catalog reader composes only already-resolved resident members; it does not
alter private storage or replacement responsibility.

```text
ResolvedBokProfile + NormalizedBokSource
                 |
                 | complete attributed commit
                 v
Resident scope: (sourceId, datasetId)
                 |
                 +-- immutable normalized generation
                 +-- retained resolved-profile attribution
```

## Storage and replacement

The private map key is a deterministic `(sourceId, datasetId)` value. A
successful commit updates exactly one key under the catalog's synchronization
boundary. The map value keeps the complete normalized source and, for an
admitted replacement, the exact `ResolvedBokProfile` returned by the registry.
An incomplete publication, rejected admission, or attribution mismatch
returns without changing the map.

The old two-argument commit remains only as a compatibility seam for existing
direct fixtures. It writes the same scoped storage with no profile attribution;
production replacement always uses the attributed commit from the shared
replacement service.

## Selection and ordering

Direct reads filter resident values by exact resolved source, dataset, and
generation. The catalog never uses another scope as fallback. The public
`profile = catalog` reader receives every independently resolved configured
member only after every member passes authorization, registration,
complete-generation admission, and freshness. One failed member fails the
catalog; it has no partial result or fallback.

Source-qualified map node and relationship keys are ordered by `(sourceId,
datasetId, localIdentity...)`. Term and component result tie-breakers use the
same source-qualified prefix, so equal local labels remain separately
addressable and deterministic. A catalog reader does not deduplicate, merge,
overlay, reconcile, or select a primary member; its `selection` attribution
has a catalog ID and complete member attributions rather than a
fabricated top-level source tuple.

The only attribution observation added for executable specifications is
package-private. No public resident enumeration or catalog mutation surface is
introduced.

## Boundaries

This design does not add source-registration, reload, backend-selection,
semantic-merge, precedence, overlay, or external two-source runtime behavior.
Existing CNCF MCP `tools/call` remains the sole read transport; administration
remains non-MCP-ready and no second BoK server is introduced. P10-03C realizes
the catalog-read CML/runtime boundary without claiming P10-04 two-source
acceptance.
