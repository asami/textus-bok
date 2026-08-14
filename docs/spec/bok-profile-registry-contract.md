# BoK Profile Registry Contract

Status: Phase 7.1 authoritative specification

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
the request-side resource field. A complete SIE publication MUST be admitted
to the exact matched profile key; a non-complete publication MUST leave both
the admitted profile and the readable catalog unchanged. The legacy
administrative replacement path MAY remain when no profile registry is
configured. This compatibility path MUST NOT become a public read selector.

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

For one request the resolver MUST perform these checks in order:

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

## Structured Failures

Every failure MUST be a structured `Consequence.Failure`. Its Observation MUST
carry an appropriate taxonomy and a `Reason` facet containing one stable code:

- `invalid-selection`;
- `project-identity-required`;
- `unregistered`;
- `unavailable`;
- `stale`;
- `ambiguous`;
- `unauthorized`; or
- `conflicting-selection`.

A failure MUST return no result from another profile and MUST NOT expose the
configured resource reference or unrelated project bindings.
