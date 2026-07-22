# Phase 4: BoK MCP Migration

## Stage Status

- Status: `COMPLETE`
- Current step: Begin Phase 5 representative SAR and CBD handoff verification.
- Closure basis: Every Phase 4 checklist item is complete and migration/runtime
  evidence is recorded below.

## Objective

Publish the four BoK-owned read operations through CNCF MCP while keeping source
replacement and every future mutation private. Preserve typed evidence and the
ownership handoff to Textus CBD Support.

## Terminology Publication Evidence

On 2026-07-21, `BokPrimaryComponent.mcpReadyOperations` declared only
`BokRetrieval.searchTerms` and `BokRetrieval.explainTerm`.
`BokMcpProjectionSpec` used CNCF `McpToolCatalog` to verify that the component
projects exactly:

- `Bok.BokRetrieval.searchTerms` with `query`, optional `category`, and optional
  `limit` input;
- `Bok.BokRetrieval.explainTerm` with required `term` input.

The same specification verifies `SearchTermsResponse` and
`ExplainTermResponse` operation outputs. Existing domain specifications prove
that both responses retain typed BoK terms and attributable evidence.
`replaceKnowledgeSource`, `searchComponentReferences`, and
`getComponentReference` are absent from the projected tool catalog.

## Next Boundary

On 2026-07-21, operation-level readiness added
`BokRetrieval.searchComponentReferences` and
`BokRetrieval.getComponentReference` without making `BokRetrieval` as a whole
ready. `BokMcpProjectionSpec` verifies their `query`/`kind`/`limit` and
`name`/`version`/`kind` schemas, their existence-only response contracts, and
continued absence of `replaceKnowledgeSource`.

On 2026-07-21, `BokMcpProjectionSpec` applied CNCF component configuration
through the public MCP catalog and JSON-RPC adapter. A hostile attempt to name
`replaceKnowledgeSource` in enable-like configuration did not publish or invoke
it, while operation, service, and global disable controls only reduced the four
declared read tools. This completes P6-32.

## Migration SAR Evidence

On 2026-07-21, `scripts/check-bok-migration-sar.sh` built current SIE, BoK, and
Scraper CARs and ran a temporary SAR on CNCF `0.5.1-SNAPSHOT`. One canonical
metadata superset was loaded through the legacy SIE path and the BoK replacement
path. The probe verified:

- four old/new operation pairs with equal generated input schemas;
- sorted, deterministic, collision-free component-qualified tool identities;
- equivalent term search, term explanation, component search, and exact lookup
  semantic projections; and
- clean server ownership and shutdown, reported as `BOK_MIGRATION_SAR_OK` and
  `BOK_MIGRATION_SAR_LIFECYCLE_OK`.

The runtime integration specification also verifies the generated SIE Component
API binding, source replacement, idempotent retry, semantic query, stale
removal, and structured failure when SIE is absent. SIE is a `provided` build
dependency, preventing its Component API classes from being duplicated in the
BoK CAR.

Final validation passed 16 tests in six suites and built
`target/textus-bok-0.1.0-SNAPSHOT.car`. CAR lint reported no failure; its only
warnings were the absent released ABI baseline and the intentional development
`sbt-cozy 0.1.15-SNAPSHOT` selection. They are deferred release-readiness work
under DP-01 in `docs/phase/deferred-release-work.md`. Archive inspection found
no duplicated SIE Component API class in the BoK CAR.

Every Phase 4 checklist item is complete. The three-component representative
SAR and CBD handoff remain Phase 5 scope.
