# BoK Profile Registry Contract

Status: Phase 7.1 direct-profile specification; Phase 10.1 catalog extension authoritative

## Scope

This specification defines the private binding, resource-loading, generation
admission, deterministic resolution, authorization, freshness, and failure
behavior implemented in Phase 7.1. The public CML selector and read-operation
integration remain Phase 7.2 work.

The terms `MUST`, `MUST NOT`, `SHOULD`, and `MAY` describe normative behavior.

## Private Configuration

The resolved CNCF subsystem configuration key
`textus.bok.profile-registry` MUST contain a private object with a `profiles`
collection whose entries have this internal shape:

| Field | Requirement |
| --- | --- |
| `profile` | One of `official`, `development`, or `project`; omission normalizes to `official`. |
| `projectId` | Present and a valid logical identifier only for `project`. |
| `source` | One complete `BokKnowledgeSource` descriptor supplied by private component/SAR configuration. |
| `evidence` | Generation-level `BokEvidence` whose `sourceId` equals the configured source identity. |
| `freshnessGeneration` | Optional exact generation required for readable resolution. |

The same private object MAY contain a `catalogs` collection. Each catalog has
one opaque logical `catalogId` and a nonempty collection of unique existing
direct profile keys. A member key can name only a configured `official`,
`development`, or exact `project` key; it cannot name a catalog. Catalog
membership is inclusion-only: it supplies no priority, result precedence,
resource location, or public enumeration capability.

The registry MUST reject an empty catalog, duplicate member, unknown member,
or duplicate catalog identity before installing configuration. It MUST validate
catalog changes atomically with profile changes: a failed catalog validation
preserves the prior installed profiles, catalogs, and admissions. Neither a
public request nor a configured member may provide a resource reference, URL,
filesystem path, repository, credential, or caller-selected member list.

The registry MUST reject duplicate normalized keys as `ambiguous` before
installing the configuration. It MUST reject use of one dataset identity by
different profile keys as `conflicting-selection`. Consequently development
and project bindings MUST NOT claim or replace the configured official dataset.

Configuration mutation MUST be atomic. A failed validation MUST preserve the
previous installed bindings and admissions.

When the configuration key is present, component initialization MUST create
and own the registry before action calls execute. Malformed configuration MUST
fail component initialization through the Structured Failures vocabulary.

## Logical Resource Access

Only the installed binding MAY supply `BokKnowledgeSource.resource` to a load.
The registry MUST load that source through `BokSourceReader` and its CNCF
`ResourceAccess` boundary. It MUST NOT accept a resource reference, URL,
filesystem path, credential, or provider setting from a read selection.

The registry and resolver MUST NOT perform direct filesystem or network access
and MUST NOT infer a project from the current directory, Git state,
environment, host, user, or provider context.

For a configured component, the existing non-MCP administrative replacement
operation MUST match the requested dataset/source/generation identity to one
installed binding and MUST load only that binding's resource. It MUST NOT read
the request-side resource field. A complete publication from the selected
backend MUST be admitted to the exact matched profile key; a non-complete
publication MUST leave both the admitted profile and the readable catalog
unchanged. An unavailable explicit SIE selection MUST fail as `unavailable`
and MUST NOT fall back to the BoK-owned memory backend or another provider.
The legacy administrative replacement path MAY remain when no profile registry
is configured. This compatibility path MUST NOT become a public read selector.

## Complete-Generation Admission

An admission MUST match the source descriptor currently installed for the
exact profile key. A mismatched source MUST fail as `conflicting-selection`.

Only a publication whose normalized state is `complete` MAY replace the
admitted generation. Any other publication state MUST leave the previous
admission untouched and report that no replacement occurred.

When configuration changes but retains the same dataset and source identities,
the registry MUST retain the preceding complete admission until a replacement
is admitted. If the new binding requires an exact generation and the retained
generation differs, resolution MUST fail as `stale`; retention MUST NOT bypass
freshness.

## Deterministic Resolution

For one direct request the resolver MUST perform these checks in order:

1. normalize and validate the closed selection shape;
2. authorize the exact normalized key;
3. locate the exact installed binding;
4. require its admitted complete generation;
5. apply the binding freshness policy; and
6. compare optional dataset/source filters with the admitted identities.

The authorization input MUST be explicit. Authorization failure MUST occur
before binding lookup so an unauthorized caller cannot distinguish registered
from unregistered private project keys.

The resolver MUST NOT try another profile or project after any failure. An
omitted profile MUST resolve only `official`; it MUST NOT fall back to an
available development or project binding.

For `profile = catalog`, the resolver validates the required logical
`catalogId`, rejects `projectId`, `datasetId`, and `sourceId` as
`conflicting-selection`, authorizes the requested catalog without revealing
its configuration, and resolves every configured member through the complete
direct sequence above. Authorization, registration, complete-generation
admission, and freshness are independently required for every member. A
single member failure fails the catalog with the existing structured outcome;
the resolver MUST NOT return a partial catalog, fallback profile, or foreign
generation.

## Successful Result

Successful resolution MUST return exactly:

```text
resolvedProfile
projectId?
datasetId
sourceId
generation
evidence
```

The result MUST NOT contain the configured resource reference. Project results
MUST contain the exact explicit project identity, and non-project results MUST
not contain a project identity.

A catalog result retains the response field `selection` with
`resolvedProfile = catalog`, its requested `catalogId`, and deterministic
complete direct member attributions. It has no top-level dataset, source,
generation, or evidence tuple. Member attributions are source-qualified and
ordered by `(sourceId, datasetId, generation)`; this is not a precedence rule.
The future CML model must make single-source fields conditional rather than
fabricating a primary member. This specification does not change CML or claim
that the runtime selector exists.

## Structured Failures

Every failure MUST be a structured `Consequence.Failure`. Its Observation MUST
carry an appropriate taxonomy and a `Reason` facet containing one stable code:

- `invalid-selection` (including an absent or malformed required `catalogId`);
- `project-identity-required`;
- `unregistered`;
- `unavailable`;
- `stale`;
- `ambiguous`;
- `unauthorized`; or
- `conflicting-selection` (including a catalog request that supplies
  `projectId`, `datasetId`, or `sourceId`).

A failure MUST return no result from another profile or a partial catalog and
MUST NOT expose the configured resource reference, catalog membership, or
unrelated project bindings.
