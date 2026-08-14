# BoK Profile Registry Design

Status: stable Phase 7.1 design

## Purpose

The private BoK profile registry binds the closed selector contract from Phase
7 to concrete logical knowledge resources without making those bindings part
of a public read request. It is the administrative bridge between CNCF-delivered
component or SAR configuration, CNCF resource access, atomic BoK generation
admission, and later public read integration.

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

The raw `BokKnowledgeSource.resource` reference exists only inside this private
binding. A public request never constructs or overrides it. The registry passes
that configured descriptor to `BokSourceReader`, so CNCF `ResourceAccess`
remains the only source-reading boundary.

Duplicate normalized keys are rejected before configuration mutation. A
dataset identity may belong to only one profile key, which prevents a
development or project binding from claiming the configured official dataset.
Changing a binding is atomic: an invalid replacement configuration leaves the
existing registry untouched.

## Registry Lifecycle

The registry follows one explicit lifecycle:

1. bootstrap, validate, and install private bindings from the resolved CNCF
   subsystem configuration;
2. load the selected configured logical source through CNCF resource access;
3. publish the normalized candidate through the existing SIE boundary;
4. admit the candidate only when publication reports `complete`; and
5. resolve an authorized normalized key to its admitted tuple.

The existing administrative replacement operation is not MCP-ready. When the
component has a registry, that operation may identify only a configured
dataset/source/generation tuple: its request-side resource field is ignored,
and the installed binding is the sole resource supplier. When no registry is
configured, the pre-Phase-7 administrative replacement behavior remains
available for compatibility. Public read selection is still deferred to Phase
7.2.

An incomplete or degraded publication does not replace admitted state. When a
configuration update retains the same dataset and source identities, the prior
complete generation remains available for rollback safety. A configured exact
generation freshness policy can nevertheless reject that retained generation
as stale.

## Resolution Boundary

Resolution accepts only:

- a logical `BokProfileSelection`;
- optional dataset/source compatibility filters; and
- an explicit `BokProfileAuthorization`.

It normalizes the selector, checks authorization before looking up the private
binding, locates the exact key, requires one admitted complete generation,
checks freshness, and finally checks compatibility filters. It never probes a
different key after failure.

`ResolvedBokProfile` contains only profile/project attribution, dataset,
source, generation, and evidence. It deliberately has no resource-reference
field. Phase 7.2 can use this tuple to constrain catalog and SIE retrieval
without exposing administrative configuration.

## Failure Representation

Each resolver failure is a `Consequence.Failure` with an Observation taxonomy
and a structured `Reason` facet containing the stable Phase 7 failure code.
The registry retains the closed outcomes `invalid-selection`,
`project-identity-required`, `unregistered`, `unavailable`, `stale`,
`ambiguous`, `unauthorized`, and `conflicting-selection`.

The private resource reference and identities of unrelated project bindings
are not included in those failures.
