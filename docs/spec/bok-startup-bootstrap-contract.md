# BoK Startup Bootstrap Contract

Status: Phase 8 P8-B bootstrap-publication implementation contract

## Scope and authority

This private specification defines the P8-B consumer implementation of the
already accepted P8-A configuration and supplier handoff. It wires configured
publication through the CNCF post-assembly activation seam. It does not claim
the managed `website.d` runtime acceptance, bound-URL observation, or MCP term
retrieval reserved to P8-C.

The contract is subordinate to the existing private
`BokProfileSelection`/`BokProfileRegistry` boundary and the structured
`BokProfileResolutionFailure` taxonomy. The authoritative supplier inputs are:

| Supplier | Accepted identity | Handoff used by P8-A |
| --- | --- | --- |
| Cozy Phase 29 | release `238a078`, `docs/phase/phase-29.md` | Project-owned generated `website.d`/`doxsite.d` metadata can be finalized as a `cncf.knowledge-source.v1` handoff. Cozy does not activate or publish it in Textus BoK. |
| CNCF Phase 70 | release `a7ce3822`, `docs/phase/phase-70.md`, `docs/spec/component-activation-lifecycle.md` R9 | A consumer-neutral, opted-in post-assembly Server activation seam runs after component/runtime-service assembly and before readiness. R9 leaves profile selection, source loading, publication, and consumer diagnostics to Textus BoK. |

The supplier identities are recorded for this handoff; their source trees are
not modified by this Slice.

## Private configuration grammar

The decoder reads the private root `textus.bok.bootstrap`. The equivalent
configuration path is `textus.bok.bootstrap.profiles`:

```yaml
textus:
  bok:
    bootstrap:
      profiles:
        - development
```

The root value MUST be an object with exactly one member, `profiles`. That
member MUST be a non-empty ordered list. Each list item MUST be one exact
string:

```text
official
development
project:<projectId>
```

`official` and `development` are normalized through the existing
`BokProfileSelection` API. `project:<projectId>` is split only at the fixed
`project:` prefix and its project identifier is then normalized by that same
API. The existing project identifier grammar and failure semantics therefore
remain authoritative. Selector spelling is case-sensitive; whitespace is not
trimmed or accepted as an alternative spelling. A declaration's order is
retained in the resulting `Vector[BokProfileKey]`.

Absence of the entire `textus.bok.bootstrap` root means disabled bootstrap and
returns no configuration. It MUST NOT implicitly select `official`. A present
empty list is invalid, not disabled. A missing `profiles` member, a non-object
root, a non-list value, a non-string item, an extra root member, a blank item,
or an unknown/malformed selector fails with the existing structured
`invalid-selection` vocabulary. An unusable `project:<projectId>` is passed to
the existing selector normalizer and retains its established
`project-identity-required` outcome.

The decoder accepts either the normal nested path or the direct resolved
configuration key, but a value present at both representations fails as
`ambiguous`. Duplicate normalized keys, including repeated project keys, also
fail as `ambiguous`; no duplicate is silently removed. No failure may cause a
fallback to another profile.

The decoder accepts logical profile identities only. It MUST NOT accept or
derive a filesystem path, URL, resource locator, provider, credential,
hostname, username, current directory, request body, or control-center value.

## Activation and replacement authority

`BokPrimaryComponent` opts in to CNCF `ComponentActivation`. Its callback uses
only `ComponentActivationContext`, is inert outside `RunMode.Server`, and
decodes this private configuration from the context. Absence remains a
successful no-op. For configured Server startup, the callback obtains its
ordinary component-bound `ExecutionContext` from the already admitted
component runtime (`logic.executionContext()`), rather than constructing an
`Action`/`ActionCall` or performing a factory or implementation lookup.

The CNCF Phase 70 R9 handoff invokes the callback only after:

1. component bootstrap;
2. Subsystem context injection and runtime-service binding;
3. component-space admission and resolver rebuilding;
4. runtime-extra and default-component admission;
5. SPI resolution; and
6. `StartupImport`.

Activation then completes before managed readiness or bound-URL publication.
Textus BoK owns profile resolution, configured-source loading, complete
generation publication, and consumer-facing diagnostics. CNCF owns activation
ordering, once-only outcome retention, readiness gating, and cleanup; BoK adds
no global or per-process once-state.

Both configured startup and protected `replaceKnowledgeSource` enter one
package-private replacement service. The manual caller first resolves its
request to the private configured key as before; bootstrap receives the
already normalized configured key and loads it directly from that registry.
Both then use the same source reader, `BokFederationPublisher` SIE dataset
replacement, registry admission, and catalog replacement. The catalog commits
only after admission succeeds. The publisher's ActionCall overload remains a
compatibility path, while activation uses its component/context overload and
never manufactures an artificial Action.

## Readiness and failure semantics

Bootstrap is default-off, Server-mode-only, and sequential in declaration
order. It resolves only already registered profiles. The BoK coordinator checks
deadline and cancellation before and after each synchronous replacement and
stops at the first terminal result. Every required result must be normalized
`complete` and admitted; a configuration error, missing registry binding or
SIE, unavailable source, non-complete publication, admission rejection,
deadline, cancellation, or unexpected callback failure returns one sanitized
structured BoK startup failure. CNCF then keeps the server unavailable and
enters ordinary cleanup. Later profiles do not begin, and late work cannot turn
a failed assembly ready.

The protected `replaceKnowledgeSource` Operation remains available for
no-restart administration with its unchanged response shape. It remains absent
from `mcpReadyOperations`; bootstrap adds no MCP mutation surface.

An unchanged input presented on restart must eventually replace the same
logical generation deterministically without duplicate terms, component
references, assertions, or vector documents. Persistent providers must receive
the same replacement semantics as in-memory SIE.

## Diagnostic boundary

`BokStartupBootstrapDiagnostic` retains immutable bounded startup state on the
component. It may expose only safe fields needed for operator action:

- normalized profile (and project identity when selected);
- dataset, source, and generation identities;
- terminal status and failure category;
- elapsed time or deadline;
- term, component, and topology counts;
- bounded warnings; and
- safe generation evidence already intended for attribution.

Diagnostics MUST NOT retain or expose the private source resource locator or
path, provider/runtime configuration, credentials, request body,
control-center internals, raw `Conclusion`/`Throwable` payload, or unbounded
source content. Registered attribution evidence is retained only in its
existing public profile form. Message parsing and foreign-profile fallback are
prohibited.

## Acceptance matrix

P8-B implements and specifies the following observable scenarios. The managed
runtime/readiness attribution scenario remains pending only for P8-C.

| Scenario | Observable expectation | Mapping |
| --- | --- | --- |
| Activation ordering and once-only | CNCF invokes opted-in components sequentially in final admitted order once per assembly; the BoK callback is inert outside Server mode. | CNCF R3-R4 / P8-B |
| Configured publication and default-off | Absent configuration does nothing; explicit ordered keys publish only selected registered profiles. | P8-B |
| Invalid/degraded/failed startup | Invalid configuration or any required non-`complete` result stops later profiles and returns one structured failure. | P8-B |
| Shared manual/bootstrap publication | Bootstrap and protected manual replacement use one atomic publication authority; mutation remains outside MCP. | P8-B |
| Deterministic restart replacement | Repeating unchanged input replaces the same logical generation without duplicate records. | P8-B |
| Safe diagnostics | Profile/status/count/evidence fields are bounded while private locators, credentials, and raw failures remain absent. | P8-B |
| MCP mutation exclusion | Bootstrap adds no MCP mutation operation and `replaceKnowledgeSource` remains absent from MCP. | P8-B |
| Managed readiness and MCP attribution | Readiness follows complete bootstrap publication and later development reads retain profile/generation attribution. | P8-C pending managed acceptance |

The active executable decoder specification separately proves absence/default-off,
valid selector normalization, declaration order, blank/unknown/malformed/
empty rejection, duplicate detection, direct+nested ambiguity, and the
project-identity failure inherited from `BokProfileSelection`.

## Non-goals

P8-B does not:

- add a watcher, polling, hot reload, restart controller, control-center API,
  real `website.d` acceptance, or bound-URL observation;
- make `replaceKnowledgeSource` MCP-ready or public, or change its response;
- change public API, schema, or configuration keys;
- modify Cozy or CNCF repositories;
- claim focused validation, review, Step completion, Phase completion, commit,
  publication, deployment, or push.
