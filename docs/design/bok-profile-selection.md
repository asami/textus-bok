# BoK Profile Selection Design

Status: stable Phase 7 design

## Purpose

Textus BoK selects exactly one attributable knowledge profile for each read.
The selector expresses the caller's knowledge intent; it is not a source URL,
filesystem location, provider choice, or instruction to discover a source from
ambient process state.

This design separates the stable selection semantics from the later registry,
public-operation, Web, and representative-runtime implementation in Phases
7.1 through 7.4.

## Profile Vocabulary

| Profile | Selection key | Content authority | Trust boundary |
| --- | --- | --- | --- |
| `official` | The profile kind alone | The published BoK generation served by `simplemodeling.org` | Only the uniquely registered published generation may be represented as official. |
| `development` | The profile kind alone | An explicitly prepared working generation from the `simplemodeling-org` repository | Development content is review input and never shadows, replaces, or claims official content. |
| `project` | The profile kind plus an explicit `projectId` | The owner of the registered project-local BoK generation | One project identity cannot read another project's generation, and no project is inferred from a checkout or process context. |

The three names are closed profile kinds. A new kind changes the public
selection contract and requires a later reviewed specification change; it is
not an operator-defined registry label.

`projectId` is an opaque logical identity registered by component or SAR
configuration. It is not a path, URL, Git remote, directory name discovered
from the current process, host username, credential, or provider setting.

## Ownership

| Owner | Responsibility |
| --- | --- |
| Source publisher or project owner | Owns the content and evidence of a generation. |
| Textus BoK | Owns profile vocabulary, private bindings, deterministic resolution, attribution, read isolation, and selection failures. |
| CNCF | Owns resource access, component/SAR configuration delivery, operation dispatch, and authorization context. |
| SIE | Owns provider-neutral federation and candidates; it does not choose a BoK profile or reinterpret profile identity. |
| Textus CBD Support | Owns component detail and usage guidance after an existence-only BoK handoff. |

Profile registration and source replacement are administrative behavior. They
remain private and non-MCP-ready. Public readers receive a logical selector
only and cannot supply binding locations, credentials, or provider
configuration.

## Selection and Resolution Model

A logical request contains:

- an optional `profile` whose omission means `official`; and
- `projectId` exactly when the selected profile is `project`.

The private resolver maps the normalized selection key to one
`ResolvedBokProfile` containing:

- the resolved profile kind and optional project identity;
- one source identity;
- one dataset identity;
- one complete generation identity; and
- attributable evidence for that selected generation.

Resolution is deterministic and follows this semantic order:

1. normalize and validate the closed selection shape;
2. authorize the normalized profile key without accepting a caller-supplied
   resource location;
3. locate exactly one private binding for the key;
4. require exactly one admitted complete and sufficiently fresh generation;
5. compare any legacy `datasetId` or `sourceId` filters with the resolved
   identities; and
6. constrain domain retrieval to that resolved tuple before classification,
   limiting, or presentation.

The resolver never searches other profiles after a failure. A missing,
unavailable, stale, ambiguous, unauthorized, or conflicting selection remains
a failure of the requested key.

## Complete-Generation and Evidence Invariants

A binding identifies an admitted logical resource through private component or
SAR configuration. CNCF resource access remains the only source-reading
boundary. A binding becomes readable only through a complete generation
already accepted by Textus BoK's atomic catalog rules.

Profile selection does not weaken generation atomicity. Terms, component
references, map nodes, relationships, and their evidence switch together.
Failed or degraded replacement retains the preceding complete generation but
does not make that generation current when an explicit freshness rule marks it
stale.

Every successful read exposes the resolved profile, optional project identity,
source, dataset, generation, and evidence. Domain records retain their existing
source evidence in addition to this selection attribution. Development and
project results therefore remain visibly distinct from official results even
when their terms happen to be textually identical.

## Existing Dataset and Source Filters

`datasetId` and `sourceId` are compatibility filters, not alternate profile
selectors. Profile resolution happens first.

- A supplied filter that equals the resolved identity may narrow or confirm
  the read.
- A supplied filter that differs from the resolved identity is a structured
  conflicting selection.
- A filter never causes the resolver to choose another profile or project.
- A request cannot omit `profile`, name a development/project dataset, and
  thereby escape the `official` default.

This preserves explicit filtering while removing the earlier ability of an
omitted profile request to read the union of every selected dataset.

## Migration Direction

Before the profile contract is integrated, an omitted Knowledge Map
`datasetId`/`sourceId` selector reads all selected complete generations. The
new public contract intentionally changes omission to one deterministic
`official` read.

Clients that require development or project knowledge must add an explicit
profile selection; project clients must also add `projectId`. Existing
dataset/source filters may remain only when they agree with the selected
profile. There is no compatibility mode that silently restores union reads,
infers a profile from a dataset, or falls back from an unavailable profile.

Phase 7 defines this migration contract. Phase 7.2 owns its public CML, REST,
SIE-candidate, and MCP-ready read integration, and Phase 7.3 owns the Web and
operator presentation.

## Failure Boundary

Selection failures are structured domain outcomes, not empty success results
or implicit fallback:

- invalid or incomplete selection shape;
- unregistered profile key;
- registered profile with no readable complete generation;
- generation rejected by its explicit freshness policy;
- more than one eligible binding or generation for the same key;
- caller not authorized for the selected key; or
- `projectId`, `datasetId`, or `sourceId` conflicting with the normalized
  selection.

Failure responses must not expose private resource locations, credentials, or
other project bindings. Transport layers may reduce diagnostic detail
according to authorization policy without changing the domain outcome.

## Non-goals

- No cross-profile union, precedence, overlay, or conflict merge.
- No write-back or public source registration/replacement.
- No profile discovery from current directory, Git state, environment,
  username, hostname, or request-supplied resource location.
- No change to SIE provider ownership or CBD Support detail ownership.
- No implicit MCP readiness for the Knowledge Map or administrative behavior.
