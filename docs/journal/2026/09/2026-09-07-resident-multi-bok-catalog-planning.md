# Resident Multi-BoK Catalog Planning

Date: 2026-09-07
Status: planning journal
Related phase: Phase 10

## Context

The immediate practical goal for Textus BoK is to make knowledge from multiple BoKs available through one Textus BoK MCP server.

The first representative sources are:

- the SimpleModeling.org / `simplemodeling-org` glossary;
- `bok-knowledgehub`.

The intended first operating mode is intentionally small: Textus BoK keeps the imported knowledge in process memory and publishes the resulting knowledge through its existing MCP-ready read operations. SIE remains the expected backend when the knowledge volume or provider requirements justify it, but it is not required for the first practical deployment.

## Existing Position

Phases 1 through 7.4 already established most of the surrounding machinery:

- BoK-owned terminology and component-existence models;
- complete source generation and evidence attribution;
- SIE-backed federation;
- four BoK-owned MCP reads;
- Knowledge Map projection;
- official/development/project profile selection;
- deterministic profile registry and resolution;
- cross-surface attribution and isolation.

The missing practical path is not another source format or another MCP mechanism. It is the ability to keep multiple selected BoK generations resident together without requiring SIE and to read them as one attributable catalog.

## Important Phase 7 / Phase 10 Distinction

Phase 7 deliberately prohibited cross-profile union. That decision remains correct at the profile-selection layer.

Phase 7 answers:

> Which admitted complete generation does this profile selection mean?

Phase 10 answers a different question:

> Which independently selected generations participate in this resident BoK catalog?

Therefore Phase 10 is not a reversal of Phase 7.

The composition is:

```text
Profile Registry
    |
    +-- SimpleModeling profile -> selected generation
    |
    +-- KnowledgeHub profile   -> selected generation
    |
    v
Resident BoK Catalog
    |
    v
BoK reads / MCP
```

Profile isolation continues to protect selection, admission, authorization, project identity, and attribution. Catalog composition occurs only after those decisions.

## Why SIE Becomes Optional for the First Practical Path

The current implementation routes complete source generations through SIE. This is appropriate for generic semantic federation and future scale, but it makes the smallest useful Textus BoK deployment depend on an additional component even when the working set easily fits in memory.

For the first practical deployment the desired properties are:

- simple local startup;
- deterministic loading;
- no external knowledge backend;
- straightforward debugging;
- direct E2E verification from Cozy-generated BoK artifacts to MCP results.

An in-memory backend provides those properties while preserving SIE as the later scale path.

The architectural requirement is therefore backend independence, not removal of SIE.

## Why the Catalog Must Preserve Source Identity

Multiple BoKs may define the same label, for example `Domain Model`.

Textus BoK must not treat label equality as knowledge identity. The resident catalog should retain both records with their source, dataset, generation, and evidence attribution.

Later semantic work may connect such records through explicit relationships such as `sameAs`, broader/narrower relations, RDF statements, or embedding-assisted matching. Phase 10 does not invent those relations.

This keeps the first catalog mechanically reliable and makes future knowledge integration additive rather than destructive.

## MCP Decision

No new BoK-specific MCP framework is planned.

Textus BoK already owns MCP-ready read operations, and CNCF already projects Component/Service/Operation definitions into MCP tools. Phase 10 should change the knowledge source behind those reads, not create another transport or discovery mechanism.

The target is:

```text
SimpleModeling.org -----+
                        |
                        +--> Resident BoK Catalog --> existing BoK reads --> CNCF MCP
                        |
bok-knowledgehub -------+
```

Source mutation and administrative loading remain outside MCP.

## Cozy Decision

Cozy already has the BoK source-generation/finalization work needed to produce the representative inputs, including the `bok-knowledgehub` Phase 38 driver work.

Phase 10 therefore does not wait for a new Cozy Phase.

The remaining missing evidence is downstream consumer acceptance: prove that Textus BoK can consume the generated artifacts in the intended resident-catalog path.

If this E2E test identifies a missing field or unstable contract in Cozy output, the finding should be handed back to Cozy as a bounded generic correction. Textus BoK should not duplicate Cozy generation behavior as a workaround.

## CNCF Decision

CNCF already supplies the runtime Resource DSL and MCP publication boundary required by Textus BoK.

Phase 10 therefore does not wait for a new CNCF Phase.

If implementation exposes a genuinely generic limitation in runtime lifecycle, resource handling, Component wiring, or MCP projection, Textus BoK should capture reproducible evidence and create a CNCF handoff. Framework behavior should not be privately reimplemented in Textus BoK merely to close the Phase.

## Phase 10 Work Shape

The agreed implementation order is:

```text
P10-01  SIE-independent in-memory backend
   |
   v
P10-02  selected generations -> Resident BoK Catalog
   |
   v
P10-03  catalog-backed existing reads -> MCP
   |
   v
P10-04  SimpleModeling.org + bok-knowledgehub representative E2E
```

The first meaningful runtime checkpoint is deliberately smaller than the final E2E:

1. start Textus BoK without SIE;
2. load one fixed KnowledgeSource/fixture;
3. retrieve one term through the ordinary read path;
4. search resident terms;
5. obtain the same knowledge through MCP.

Only after that path is stable should the second BoK be added and multi-source behavior exercised.

## Documentation Decision

Phase 10 remains the thin work-management ledger.

Implementation proposals and unsettled contracts belong in:

- `docs/notes/resident-multi-bok-catalog-proposed-specification.md`.

This journal records why the architecture changed and how the decision relates to the closed Phase 7 contract.

Once P10-01/P10-02 implementation demonstrates stable boundaries, only the proven semantics should be promoted into `docs/design` and `docs/spec`. This avoids freezing speculative abstractions before the first real two-BoK consumer path has run.

## Expected Practical Milestone

Phase 10 is practically successful when one running Textus BoK instance can hold both SimpleModeling.org glossary knowledge and `bok-knowledgehub` knowledge in memory and make attributable results from that combined resident catalog available through MCP without SIE.

That milestone is the first priority. Embeddings, RDF reasoning, richer semantic reconciliation, persistent storage, and UI expansion follow only after this path is useful in normal work.
