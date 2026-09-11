# BoK Profile Registry Design

Status: stable Phase 7.1 direct-profile design; Phase 10.1 catalog extension

## Purpose

The private BoK profile registry binds the closed selector contract from Phase
7 to concrete logical knowledge resources without making those bindings part
of a public read request. It is the administrative bridge between CNCF-delivered
component or SAR configuration, CNCF resource access, backend completion,
atomic BoK generation admission, and later resident catalog integration.

The registry resolves a profile; it does not own backend choice or resident
storage. Those responsibilities belong to the private BoK knowledge backend
contract. P10-03C realizes public catalog-backed reads and their existing
REST/Web/MCP projection; real two-source runtime acceptance remains P10-04 work.

This registry is not a source-discovery mechanism. Its inputs are explicit
configuration and an explicit authorization set; it has no reason to inspect a
working directory, Git checkout, environment variable, host identity, user
name, provider setting, filesystem path, or network location supplied by a
reader.

## Configuration Model

The subsystem configuration key `textus.bok.profile-registry` contains one
private object with a `profiles` collection. Component initialization decodes
and validates this object before an action call exists. Each binding declares:

- the closed `profile` kind;
- `projectId` exactly for a project binding;
- one `BokKnowledgeSource` logical source descriptor;
- one generation-level `BokEvidence`; and
- an optional `freshnessGeneration` requirement.

The private object may also carry a `catalogs` collection. A catalog is one
logical `catalogId` with nonempty, unique existing direct profile keys as
members. It has no public representation, enumeration, member override,
resource field, or priority. Members are inclusion-only and cannot name a
catalog, so the configuration cannot create nested or recursive composition.

The raw `BokKnowledgeSource.resource` reference exists only inside this private
binding. A public request never constructs or overrides it. The registry passes
that configured descriptor to `BokSourceReader`, so CNCF `ResourceAccess`
remains the only source-reading boundary.

Duplicate normalized keys, empty catalogs, duplicate catalog identities,
duplicate catalog members, and unknown member keys are rejected before
configuration mutation. A
dataset identity may belong to only one profile key, which prevents a
development or project binding from claiming the configured official dataset.
Changing a binding is atomic: an invalid replacement configuration leaves the
existing registry untouched.

## Registry Lifecycle

The registry follows one explicit lifecycle:

1. bootstrap, validate, and install private bindings from the resolved CNCF
   subsystem configuration;
2. load the selected configured logical source through CNCF resource access;
3. complete the normalized candidate through the selected BoK backend, using
   in-memory storage by default and the retained SIE provider boundary only
   when SIE is explicitly selected;
4. admit the candidate only when backend completion reports `complete`; and
5. resolve an authorized normalized key to its admitted tuple.

When an explicitly selected SIE provider is unavailable or cannot be
initialized, completion returns one deterministic structured `unavailable`
outcome. It MUST NOT silently select the in-memory backend, another provider,
or a foreign profile. The default and explicit memory paths do not require SIE.

The existing administrative replacement operation is not MCP-ready. When the
component has a registry, that operation may identify only a configured
dataset/source/generation tuple: its request-side resource field is ignored,
and the installed binding is the sole resource supplier. When no registry is
configured, the pre-Phase-7 administrative replacement behavior remains
available for compatibility. P10-03C public read selection uses the configured
direct-or-catalog resolver.

An incomplete or degraded publication does not replace admitted state. When a
configuration update retains the same dataset and source identities, the prior
complete generation remains available for rollback safety. A configured exact
generation freshness policy can nevertheless reject that retained generation
as stale.

## Resolution Boundary

Direct resolution accepts only:

- a logical `BokProfileSelection`;
- optional dataset/source compatibility filters; and
- an explicit `BokProfileAuthorization`.

It normalizes the selector, checks authorization before looking up the private
binding, locates the exact key, requires one admitted complete generation,
checks freshness, and finally checks compatibility filters. It never probes a
different key after failure.

Catalog resolution accepts only the closed `profile = catalog` request and a
logical `catalogId`; direct `projectId`, `datasetId`, and `sourceId` conflict
with it. The registry authorizes the catalog and independently resolves every
private member through the direct sequence. Each member retains its own
authorization, registration, complete-generation admission, and freshness
gate. Any one failure fails the catalog with its existing structured outcome;
the registry cannot return a partial catalog, choose an alternate member, or
fall back to another profile.

`ResolvedBokProfile` contains only profile/project attribution, dataset,
source, generation, and evidence. It deliberately has no resource-reference
field. Phase 7.2 can use this tuple to constrain backend completion and the
retained optional SIE provider path without exposing administrative
configuration.

Catalog selection attribution has `resolvedProfile = catalog`, its requested
`catalogId`, and deterministic complete direct member attributions ordered by
source-qualified `(sourceId, datasetId, generation)`. It deliberately has no
top-level source/dataset/generation/evidence tuple and does not establish a
primary member or precedence. P10-03C realizes the CML and runtime selector.

## Failure Representation

Each resolver failure is a `Consequence.Failure` with an Observation taxonomy
and a structured `Reason` facet containing the stable Phase 7 failure code.
That `Reason` facet remains the registry's internal classification; the same
code is explicitly materialized as `Conclusion.status.appStatus`, the canonical
structured transport projection for REST, Web, and MCP consumers. This does
not parse a message, promote `Reason` in framework-wide behavior, or create a
fallback or foreign result. The registry retains the closed outcomes `invalid-selection`,
`project-identity-required`, `unregistered`, `unavailable`, `stale`,
`ambiguous`, `unauthorized`, and `conflicting-selection`.

MCP retains its legacy operation-failure result (`isError` and one text block)
and projects the code only as `structuredContent.error.appStatus`. The Static
Form Knowledge Map retains its intentional HTTP `200` operation-result page
and renders that code only as the escaped `error.appStatus` property. Neither
surface recovers a code from the Reason or message, and neither may attribute a
failure to a fallback profile.

The private resource reference, catalog membership, and identities of
unrelated project bindings are not included in those failures. Existing CNCF
MCP `tools/call` remains the sole read transport; registry/replacement/catalog
administration remains non-MCP-ready and no second BoK server is introduced.
