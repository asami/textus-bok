# Phase 7: BoK Profile Selection Contract

Stage Status:
- Current status: OPEN
- Current step: P7-A Selection Contract
- Owner: Textus BoK development
- Update rule: Update this block and `phase-7-checklist.md` only when a
  checklist outcome has reproducible review or executable evidence.

## Approved Split

On 2026-08-14, the user explicitly approved splitting the original open
Selectable BoK Profiles Phase at its five existing Step boundaries:

1. Phase 7 (this Phase): P7-A Selection Contract;
2. Phase 7.1: P7-B Profile Registry and Resolution;
3. Phase 7.2: P7-C Read Contract Integration;
4. Phase 7.3: P7-D Web and Operator Integration; and
5. Phase 7.4: P7-E Representative Verification and Closure.

The approved dependency order is Phase 7, 7.1, 7.2, 7.3, then 7.4. Phase 6
is this Phase's predecessor and Phase 7.1 is its successor. The original
Phase was OPEN at P7-A and had no completed Step, Slice, checklist outcome,
commit, or validation result to relocate. Phases 1–6 and all their completed
history remain unchanged.

Approval evidence is the user's explicit `$cncf-split-phase` invocation with
these same five boundaries and dependency order on 2026-08-14.

## Phase Plan Gate

Phase Plan Gate: PROCEED
- target: conservative upper bound <= 6h
- estimated_at_recommended_effort: 2–4h
- recommended_minimum_effort: high
- runtime_suitability: re-evaluate in the Phase execution task
- source: approved split from Phase 7

## Pre-split Gate Evidence

On 2026-08-14, the original five-Step Phase received
`Phase Plan Gate: SPLIT_REQUIRED`. Its conservative estimate was 17–27h at
recommended xhigh effort because its critical path crossed the public
selection contract, private resource resolution, REST/CML/SIE/MCP migration,
Static Form integration, and representative runtime/release boundaries. This
is dated pre-split evidence, not the current Phase Plan Gate.

## Objective

Let a Textus BoK reader explicitly select one of three attributable knowledge
profiles without changing component ownership or exposing source mutation:

- `official`: the formally published BoK served by `simplemodeling.org`;
- `development`: an explicitly prepared working generation from the
  `simplemodeling-org` repository;
- `project`: a project-local BoK selected with an explicit project identity.

This Phase closes the stable semantic and migration contract before any
public read shape or runtime behavior changes. Implementation of that contract
belongs to sequential Phases 7.1–7.4. The contract still governs all relevant
REST operations, the Static Form Knowledge Map, and the four existing
MCP-ready terminology/component operations without making the Knowledge Map
MCP-ready implicitly.

## Starting Point

Textus BoK already stores complete source-owned dataset generations and retains
source evidence. `getKnowledgeMap` can narrow by raw `datasetId`/`sourceId`, but
the four MCP-ready terminology/component reads have no public BoK-profile
selector. Source replacement is private and must remain private.

Phase 7 defines the semantic selection layer above those existing source and
dataset identities. It does not turn a caller-supplied URL, filesystem path,
or current working directory into a source-discovery mechanism.

## Contract Artifacts

P7-A promotes the selection boundary into these stable documents:

- `docs/design/bok-profile-selection.md` records ownership, trust boundaries,
  deterministic resolution, migration direction, and non-goals.
- `docs/spec/bok-profile-selection-contract.md` defines the closed request
  shape, resolution tuple, compatibility filters, attribution, and structured
  failure outcomes.

This Phase document remains the execution ledger. The design and specification
are the stable semantic authorities consumed by Phases 7.1 through 7.4.

## Target Selection Semantics

1. The public selector is a closed profile kind, not a resource location.
2. Omission selects `official` as the new deterministic default. This preserves
   the optional request shape but intentionally narrows the prior all-dataset
   behavior, so P7-A must define migration and compatibility evidence.
3. `development` is always explicit and cannot shadow or replace `official`.
4. `project` requires an explicit project identity; ambient process state is
   not a project selector.
5. A resolver maps the requested profile to an admitted complete
   source/dataset generation before domain retrieval begins.
6. Every result exposes the resolved profile plus source, dataset, generation,
   and evidence needed to explain what was read.
7. Phase 7 selects one profile per request. Cross-profile union, precedence,
   overlay, and conflict resolution are outside this phase.
8. Unregistered, unavailable, stale, ambiguous, or unauthorized selections
   fail in a structured form and never fall through to a different profile.

These are planning constraints. P7-A promotes the accepted model into stable
design and specification documents before the public contract is changed.

## Scope

1. Define stable `official`, `development`, and `project` profile vocabulary,
   ownership, and trust semantics.
2. Fix omitted-selector defaulting, explicit project identity, and the
   migration from the current all-selected-datasets behavior.
3. Specify resolution and attribution in terms of profile, source, dataset,
   generation, and evidence, including interaction with existing filters.
4. Specify structured unavailable, stale, ambiguous, unauthorized, and
   conflicting outcomes with no cross-profile fallback.

## Step

### P7-A: Selection Contract

Focus: P7-01.

- Freeze terminology and ownership for `official`, `development`, and
  `project` profiles.
- Specify defaulting, project identity, resolution, attribution, failure, the
  migration from omitted all-dataset reads, and compatibility with existing
  `datasetId`/`sourceId` filters.

## Boundaries

- No public read request accepts an arbitrary URL, filesystem path, resource
  root, credential, or provider configuration.
- No profile is inferred from a process current directory, Git checkout, host
  username, or another ambient value.
- Development and project content cannot claim the `official` profile or
  silently replace its selected generation.
- Profile selection is read-only. Source registration and
  `replaceKnowledgeSource` remain administrative and non-MCP-ready.
- SIE continues to own provider federation; profile resolution and exact
  BoK-selection semantics remain inside Textus BoK.
- CBD Support continues to own component detail and usage guidance.
- Cross-profile merge/overlay and write-back to any BoK source are not Phase 7
  work.
- Registry/resolver implementation belongs to Phase 7.1, public read and MCP
  integration to Phase 7.2, Web/operator integration to Phase 7.3, and
  representative runtime closure to Phase 7.4.

## Validation Boundary

Review the stable design/specification text against every P7-01 outcome and
the existing BoK domain, Knowledge Map, resource, SIE, MCP, and component
ownership contracts. Run documentation static checks and `git diff --check`.
No SBT or runtime validation is required unless the Phase execution task
explicitly admits an executable behavior change.

## Completion Conditions

Phase 7 closes only when every P7-01 item in `phase-7-checklist.md` is checked,
the stable selection and compatibility contracts are documented, validation
evidence is recorded here, and final review finds no actionable Phase 7 issue.
