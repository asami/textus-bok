# Phase 4: BoK MCP Migration

## Stage Status

- Status: `IN_PROGRESS`
- Current step: Prove mutation remains unavailable after CAR/SAR MCP policy.
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

P6-32 now proves that CAR/SAR policy cannot expose source replacement.
