# Phase 8: Resident Multi-BoK Catalog and MCP Publication

Stage Status:
- Current status: PLANNED
- Current step: P8-01 In-Memory Knowledge Backend
- Owner: Textus BoK development
- Update rule: Update this block and `phase-8-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Objective

Make Textus BoK usable as a resident knowledge hub that can load multiple
selected BoK generations at the same time, keep their attributable terminology
in memory without requiring SIE, expose one combined read catalog, and publish
that catalog through the existing BoK MCP read operations.

The first representative target is:

```text
SimpleModeling.org glossary -----------\
                                        +-> Textus BoK resident catalog -> MCP
bok-knowledgehub glossary -------------/
```

Phase 8 intentionally optimizes for practical use before large-scale semantic
federation. The default Phase 8 runtime is in-memory. SIE remains a supported
future/optional backend for larger knowledge volumes; this Phase must not remove
or invalidate the existing SIE integration contract.

## Starting Point

Phases 7 through 7.4 established explicit `official`, `development`, and
`project` profile selection, deterministic resolution to one complete
source/dataset/generation, exact attribution, REST/Web/MCP parity, and strict
cross-profile isolation.

That contract deliberately forbids cross-profile union. It proves that each
selected generation is trustworthy and attributable, but it does not yet create
one resident catalog from multiple independently selected generations.

Current source publication also assumes SIE for complete-generation federation.
For practical local use, this creates an unnecessary runtime dependency even
when the total knowledge volume can be kept in process memory.

Phase 8 adds a resident-catalog layer *after* existing profile/generation
resolution. It does not weaken Phase 7 isolation rules for resolution itself.
Each source is still resolved independently; only accepted selected generations
are admitted to the combined resident catalog.

## External Baseline and Dependencies

Phase 8 depends on existing, already-delivered framework capabilities. It does
not require a new Cozy or CNCF feature Phase as a start gate.

### CNCF baseline

Use the existing CNCF capabilities for:

- Component / Service / Operation execution;
- resource DSL based logical source reads;
- runtime assembly and CAR integration; and
- MCP projection of MCP-ready Component Operations.

If Phase 8 discovers a missing *generic runtime* capability, that issue may be
handed back to CNCF as a separate follow-up. Phase 8 must not pre-emptively add
BoK-specific behavior to CNCF.

### Cozy baseline

Use the existing Cozy BoK build/finalization contract for glossary,
KnowledgeSource, RDF/graph metadata, and generated BoK resources. Cozy Phase 38
already used `bok-knowledgehub` as the driver for the generated-knowledge
boundary.

Phase 8 owns downstream Textus BoK consumer acceptance. A Cozy change is needed
only if real Phase 8 consumption proves that the generated contract lacks
required source information or violates its documented boundary.

## Selected Architecture

```text
profile/source A
  -> existing profile resolution
  -> selected complete generation A --\
                                      \
                                       -> ResidentBoKCatalog -> BoK reads -> MCP
                                      /
profile/source B
  -> existing profile resolution
  -> selected complete generation B --/
```

The resident catalog preserves source identity. Equal terms from two sources do
not overwrite each other merely because their display names match.

A representative logical identity is therefore source-qualified, for example:

```text
simplemodeling:domain-model
knowledgehub:domain-model
```

Search may return both entries. Semantic equivalence such as `sameAs`, RDF
unification, embedding-based ranking, or automatic conflict reconciliation is
not required for Phase 8.

## Backend Direction

Introduce or formalize one BoK-owned storage boundary so application reads do
not depend directly on SIE:

```text
BoKKnowledgeStore / BoKCatalogStore
  |- InMemory  <- Phase 8 default
  `- SIE       <- retained optional/future backend
```

The exact type names are implementation decisions for P8-01, but the ownership
rule is fixed: BoK read semantics remain inside Textus BoK, while SIE is a
provider/backend and cannot be the only place where Textus BoK's catalog
semantics exist.

The in-memory backend must support complete-generation replacement semantics so
reloading one source removes stale entries from that source without deleting
entries owned by another source.

## Work Stack

### P8-01: SIE-Independent In-Memory Knowledge Backend

Goal: allow Textus BoK to start, admit, retain, replace, and read a complete BoK
generation without an installed SIE component.

Required outcomes:

- define the BoK-owned backend/store boundary used by catalog reads;
- provide an in-memory implementation as the Phase 8 default/development
  backend;
- preserve source/dataset/generation/evidence attribution;
- preserve complete-generation replacement and stale-record removal per source;
- keep the existing SIE path available behind the same BoK-owned boundary or a
  compatible provider boundary;
- prove that SIE absence no longer blocks the in-memory runtime path;
- do not silently fall back from an explicitly requested SIE backend to memory.

### P8-02: Resident Multi-BoK Catalog

Goal: combine multiple independently selected complete generations into one
resident read catalog without destroying source identity.

Required outcomes:

- admit more than one selected generation simultaneously;
- reuse existing profile resolution before catalog admission;
- keep each admitted entry attributable to its profile/source/dataset/
  generation/evidence;
- define deterministic catalog identity and ordering;
- permit equal term names from different sources without accidental overwrite;
- support source-scoped replacement/reload without rebuilding unrelated source
  generations;
- keep Phase 7 profile resolution isolation intact: catalog union happens only
  after each selected generation has been independently resolved and admitted.

### P8-03: Catalog-Backed BoK Reads and MCP Publication

Goal: make the existing public BoK terminology/component read semantics operate
against the resident catalog and publish the same semantics through existing
CNCF MCP projection.

Required outcomes:

- route the existing BoK-owned read operations through the resident catalog;
- return multi-source results where the operation semantics permit candidates
  or search results;
- preserve exact/ambiguous/conflict/insufficient-evidence/no-match semantics
  where those semantics remain applicable;
- preserve source attribution in MCP results;
- keep source mutation, registration, reload administration, and
  `replaceKnowledgeSource` non-MCP-ready;
- do not introduce a second BoK-specific MCP server implementation when the
  existing CNCF operation projection is sufficient.

### P8-04: SimpleModeling.org + bok-knowledgehub End-to-End Acceptance

Goal: prove practical use with two real BoK sources.

Representative acceptance flow:

```text
SimpleModeling.org / simplemodeling-org glossary
            -> Cozy-generated/admitted BoK source
            -> Textus BoK in-memory resident catalog

bok-knowledgehub glossary
            -> Cozy-generated/admitted BoK source
            -> Textus BoK in-memory resident catalog

both sources
            -> one catalog query
            -> existing BoK operation
            -> MCP tools/call result
```

Required evidence:

- both sources are present in the same running Textus BoK process;
- a known term from SimpleModeling.org is retrievable;
- a known term from bok-knowledgehub is retrievable;
- a query that can match both sources returns both with distinct attribution;
- replacing/reloading one source does not remove the other source's entries;
- the same representative read succeeds through the MCP surface;
- SIE is not installed or required for this acceptance path.

## Scope

1. BoK-owned in-memory storage for complete selected generations.
2. Multi-source resident catalog construction from independently resolved
   generations.
3. Catalog-backed terminology/component reads.
4. MCP publication through existing MCP-ready operations.
5. Real downstream consumption of SimpleModeling.org and `bok-knowledgehub`
   BoK content.
6. Deterministic identity, attribution, replacement, and failure semantics for
   the resident path.

## Non-Goals

- No requirement to use SIE in the representative Phase 8 runtime.
- No removal of SIE support or redesign of generic SIE federation.
- No embedding/vector search requirement.
- No semantic automatic merge of equal terms across sources.
- No `sameAs` inference or RDF graph reconciliation requirement.
- No precedence/overlay rule that allows one BoK silently to replace another.
- No write-back from Textus BoK to SimpleModeling.org, `bok-knowledgehub`, or
  another source.
- No arbitrary URL/filesystem path supplied through public read or MCP calls.
- No MCP exposure of source registration, source replacement, or reload
  administration.
- No new Flutter, Logical UI, or Progressive Static Web work as a Phase 8
  dependency.
- No pre-emptive Cozy or CNCF feature development. Downstream findings are
  handed off only when executable Phase 8 evidence proves a missing upstream
  capability.

## Compatibility Rules

1. Existing Phase 7 profile selection remains valid for single-profile reads.
2. Profile resolution and catalog composition are separate concerns.
3. A catalog member is always an explicitly admitted selected generation; the
   current working directory or other ambient state cannot add a member.
4. Existing source/dataset/generation/evidence identities remain authoritative
   provenance.
5. The in-memory backend must not change public BoK domain semantics merely
   because SIE is absent.
6. SIE-backed operation, when selected, must continue to cross the existing
   public component boundary rather than introducing a private coupling.

## Validation Boundary

Each Step should have focused executable specifications. The final Phase gate
must include:

- focused store/catalog executable specifications;
- existing BoK read and MCP regression specifications;
- `sbt --batch test cozyBuildCAR`;
- normal CAR validation/lint used by the repository;
- one representative two-source runtime scenario using SimpleModeling.org and
  `bok-knowledgehub` content;
- one MCP runtime check over the resident two-source catalog; and
- independent final review confirming that no Phase 7 profile-isolation,
  attribution, source-mutation, or component-boundary guarantee was weakened.

If a real upstream Cozy or CNCF defect blocks representative acceptance, record
that finding as an explicit blocked dependency with executable evidence rather
than expanding Phase 8 silently across repository boundaries.

## Completion Conditions

Phase 8 closes only when every item in `phase-8-checklist.md` is checked and:

1. Textus BoK runs with the in-memory backend and no SIE dependency;
2. at least two selected BoK generations coexist in one resident catalog;
3. source attribution survives combined reads;
4. existing BoK MCP reads expose the catalog successfully;
5. SimpleModeling.org and `bok-knowledgehub` complete the representative E2E
   path; and
6. full validation and independent review find no actionable Phase 8 issue.

Phase 8 must stop after closure. SIE scalability, semantic federation,
embedding search, automatic equivalence, and richer knowledge reconciliation
belong to later explicitly approved work.
