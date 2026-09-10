# BoK Resident Catalog Design

Status: Phase 10 P10-02 stable private design

## Intent

The resident catalog stores independently resolved and admitted complete BoK
generations without requiring semantic composition. The profile registry
remains the authority for selection and admission; the catalog only retains
the resulting immutable generation after complete publication.

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

Reads filter resident values by exact resolved source, dataset, and generation.
The catalog never uses another scope as fallback. Source-qualified map node and
relationship keys are ordered by `(sourceId, datasetId, localIdentity...)`.
Term and component result tie-breakers use the same source-qualified prefix,
so equal local labels remain separately addressable and deterministic.

The only attribution observation added for executable specifications is
package-private. No public resident enumeration or catalog mutation surface is
introduced.

## Boundaries

This design does not add public read, MCP, REST/Web, source-registration,
reload, backend-selection, semantic-merge, precedence, overlay, or external
two-source runtime behavior. Those concerns remain outside P10-02.
