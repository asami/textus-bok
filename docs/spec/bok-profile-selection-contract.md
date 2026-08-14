# BoK Profile Selection Contract

Status: Phase 7 authoritative specification

## Scope

This specification defines the stable logical selector, deterministic
resolution result, attribution, compatibility filters, migration behavior, and
failure outcomes for Textus BoK reads. It does not define the Phase 7.1 private
registry representation or the Phase 7.2 generated CML types.

The terms `MUST`, `MUST NOT`, `SHOULD`, and `MAY` describe normative behavior.

## Profile Kinds and Selection Key

`profile` is an optional closed value with exactly these kinds:

- `official`;
- `development`; and
- `project`.

Omitting `profile` MUST normalize to `official`. An unknown value MUST fail as
`invalid-selection`; it MUST NOT become an operator-defined profile or an
`unregistered` instance of a known kind.

`projectId` is an optional logical identifier with this validity matrix:

| Normalized profile | `projectId` | Outcome |
| --- | --- | --- |
| `official` | omitted | valid |
| `development` | omitted | valid |
| `project` | present and valid | valid |
| `project` | omitted or empty | `project-identity-required` |
| `official` or `development` | present | `conflicting-selection` |

A valid `projectId` MUST be supplied explicitly by the caller and matched as a
logical registry key. It MUST NOT be interpreted as a URL, filesystem path,
resource root, credential, provider configuration, current directory, Git
checkout, hostname, or username. Public reads MUST NOT accept those values as
profile-binding input.

The normalized selection key is `(official)`, `(development)`, or
`(project, projectId)`. One request MUST contain exactly one selection key.

## Profile Meaning

### Official

`official` denotes the uniquely registered complete BoK generation published
for `simplemodeling.org`. Only the configured official binding MAY produce an
official result. Development or project content MUST NOT claim, shadow, or
replace this profile.

### Development

`development` denotes the uniquely registered complete working generation
explicitly prepared from the `simplemodeling-org` repository. It is not a
published authority, even when its content or evidence resembles the official
generation.

### Project

`project` denotes the uniquely registered complete generation for the exact
`projectId`. Resolution and retrieval MUST remain isolated by that identity.
No record from another project binding MAY enter the result.

## Private Binding and Resolution

Bindings MUST be supplied through private component or SAR configuration and
MUST reference admitted logical resources read through CNCF resource access.
Registration and source replacement MUST remain administrative and
non-MCP-ready.

For one authorized normalized selection key, the resolver MUST produce exactly
one tuple:

```text
resolvedProfile
projectId?
datasetId
sourceId
generation
evidence
```

The tuple MUST refer to one admitted complete Textus BoK generation. Terms,
component references, map nodes, relationships, and generation evidence MUST
retain the existing atomic replacement invariant. A failed or degraded
replacement MUST NOT expose a partial generation.

If an explicit binding freshness policy rejects the otherwise complete
generation, resolution MUST fail as `stale`; retaining that generation for
atomic rollback safety does not make it readable through the stale binding.

Resolution MUST NOT probe a different profile or project after any failure.
It MUST NOT use SIE to choose a profile. Provider-backed candidates MAY be
queried only after resolution and MUST be constrained to the resolved dataset
and source before classification, overfetch completion, or result limiting.

## Dataset and Source Compatibility Filters

Existing optional `datasetId` and `sourceId` inputs MUST be evaluated only
after profile resolution. They are assertions or narrowing filters within the
resolved tuple; they are not profile selectors.

| Filter state | Required outcome |
| --- | --- |
| omitted | continue with the resolved identity |
| equal to the resolved identity | continue within the resolved profile |
| different from the resolved identity | `conflicting-selection` |

A filter MUST NOT change `official` to `development` or `project`, choose a
different project, broaden the read, or cause fallback. Domain retrieval MUST
be constrained to the resolved tuple before match classification and limits.

## Successful Attribution

Every successful read response MUST identify:

- `resolvedProfile`;
- `projectId` when the resolved profile is `project`;
- `datasetId`;
- `sourceId`;
- `generation`; and
- attributable generation evidence.

Omitted selection therefore reports `resolvedProfile = official`. Record-level
evidence MUST remain present where the existing domain contract requires it;
selection attribution does not replace source evidence.

REST, the four existing MCP-ready terminology/component reads, and the Static
Form Knowledge Map MUST eventually project equivalent selection and
attribution semantics on their respective operation surfaces. This contract
does not make the Knowledge Map, source registration, or source replacement
MCP-ready.

## Structured Failure Outcomes

All selection failures MUST be structured failures and MUST return no result
from another profile.

| Failure | Meaning |
| --- | --- |
| `invalid-selection` | `profile` is not one of the three closed kinds or the selector shape is malformed. |
| `project-identity-required` | `project` was selected without a usable explicit `projectId`. |
| `unregistered` | No private binding exists for the authorized normalized key. |
| `unavailable` | A binding exists but no admitted complete generation is readable. |
| `stale` | The binding's explicit freshness policy rejects its complete generation. |
| `ambiguous` | More than one binding or eligible complete generation claims the same normalized key. |
| `unauthorized` | The caller is not permitted to select the normalized key. |
| `conflicting-selection` | `projectId`, `datasetId`, or `sourceId` contradicts the normalized/resolved profile. |

Authorization MUST be evaluated without revealing private binding locations or
the identities of other projects. A transport MAY conceal finer diagnostic
details required by security policy, but it MUST preserve failure semantics
internally and MUST NOT convert the failure into a fallback or empty success.

## Omitted-Selector Migration

Before Phase 7.2 integration, an omitted Knowledge Map dataset/source selector
can read all selected complete generations. After integration:

- omitted `profile` MUST select only `official`;
- an existing request for the official dataset/source remains valid only when
  its filters equal the resolved official identities;
- a development request MUST add `profile = development`;
- a project request MUST add `profile = project` and `projectId`;
- a legacy dataset/source filter that points outside the selected profile MUST
  fail as `conflicting-selection`; and
- no compatibility flag or fallback MAY restore implicit all-profile union.

This is an intentional compatibility migration. Public schemas and operator
guidance delivered by later Phases MUST make the new default and explicit
non-official selection visible.

## Representative Conformance Matrix

The representative conformance fixture is implementation evidence for this
contract, not an addition to the public selector vocabulary. A conforming
representative SAR MUST register exactly one official binding, one development
binding, and at least two distinct project keys. Each binding MUST have a
distinct observable dataset identity, source identity, complete generation,
generation-level evidence, term record, and graph-node identity. The prepared
implementation evidence is the profile-selection SAR example, its lifecycle
script and probe, and the resolved-profile executable specification.

| Conformance concern | Required representative behavior |
| --- | --- |
| Positive selection | Omitted selection MUST read only official; explicit development and each project key MUST read only that key's generation and retain its exact selection and record evidence. |
| Foreign term isolation | For every selected key, a cyclic probe for a term owned by another key MUST return no-match with empty results and the selected attribution. It MUST NOT union or fall back. |
| Foreign topology isolation | For every selected key, a cyclic focus for a node owned by another key MUST return no-match with empty nodes and relationships and the selected attribution. It MUST NOT expose the foreign node or topology. |
| Cross-surface reads | REST terminology and Knowledge Map reads MUST expose the resolved tuple; qualified MCP read tools MUST expose equivalent attribution for their respective reads; the Static Form Knowledge Map MUST agree with the REST map projection. |
| Surface ownership | MCP tools/list MUST contain only the four qualified terminology/component read tools for this component. Knowledge Map and source replacement MUST remain absent from MCP; source replacement MUST remain protected. |
| Resource boundary | A caller MUST NOT select a private resource root. The representative replacement requests MUST demonstrate that a request-side resource cannot choose an alternate binding. |
| Proof level | Focused executable/static evidence MUST be reported separately from live SAR evidence. Prepared artifacts MUST NOT be described as a live runtime pass; only the Phase 7.4 release gate MAY establish that result. |

The cycle is an evidence strategy: each selected key probes a different
foreign key, and the final probe closes the cycle. It does not define project
names or make fixture identities public API. A failed, unavailable, stale,
unauthorized, incomplete, or unknown selection MUST remain a structured
failure under the outcomes above.

## Isolation and Non-goals

- A response MUST contain records from one resolved profile and, for
  `project`, one exact project identity only.
- Cross-profile union, overlay, precedence, deduplication, and conflict merge
  are outside this contract.
- Profile selection is read-only and MUST NOT mutate a source or registry.
- Direct filesystem/network discovery and caller-supplied resource locations
  are forbidden.
- SIE remains the federation owner; CBD Support remains the owner of component
  detail and usage guidance.
