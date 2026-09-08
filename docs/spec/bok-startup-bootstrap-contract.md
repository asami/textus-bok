# BoK Startup Bootstrap Contract

Status: Phase 8 P8-A contract and supplier handoff in progress

## Scope and authority

This private specification freezes the configuration decoder and the
downstream handoff boundary for Textus BoK Phase 8, P8-A1. It does not accept
the runtime activation hook, a publication implementation, managed-server
readiness, or any Phase 8 closure evidence.

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

## Activation handoff

The decoder is unwired in P8-A1. A later P8-B implementation MAY consume its
ordered normalized keys only through the existing private registry. The CNCF
Phase 70 R9 handoff requires the consumer to use the post-assembly Server seam
after:

1. component bootstrap;
2. Subsystem context injection and runtime-service binding;
3. component-space admission and resolver rebuilding;
4. runtime-extra and default-component admission;
5. SPI resolution; and
6. `StartupImport`.

Activation then completes before managed readiness or bound-URL publication.
Textus BoK owns profile resolution, configured-source loading, complete
generation publication, and consumer-facing diagnostics. The supplier seam
does not receive Textus configuration and does not add a second publication
authority.

## Readiness and failure semantics

The eventual bootstrap operation is default-off, Server-mode-only, once per
managed server assembly, and sequential in declared profile order. It must
resolve only already registered profiles. Every required publication must
return `complete`; invalid configuration, an unavailable source, degraded or
failed publication, timeout, cancellation, or an ordinary activation failure
keeps the server unavailable and enters managed cleanup. Later callbacks must
not begin after a terminal failure, and late work must not turn a failed
assembly ready.

The protected `replaceKnowledgeSource` Operation remains available for later
no-restart administration. Bootstrap and that Operation must eventually share
the same `BokSourceReader`, `BokFederationPublisher`, atomic dataset
replacement, and local catalog replacement path. P8-A1 does not extract or
wire that service.

An unchanged input presented on restart must eventually replace the same
logical generation deterministically without duplicate terms, component
references, assertions, or vector documents. Persistent providers must receive
the same replacement semantics as in-memory SIE.

## Diagnostic boundary

Future startup diagnostics MAY expose only bounded, safe fields needed for
operator action:

- normalized profile (and project identity when selected);
- dataset, source, and generation identities;
- terminal status and failure category;
- elapsed time or deadline;
- term, component, and topology counts;
- bounded warnings; and
- safe generation evidence already intended for attribution.

Diagnostics MUST NOT expose private resource locators or paths, URLs,
credentials, provider configuration, request bodies, control-center internals,
raw `Throwable` payloads, or unbounded source content. A failure remains a
structured `Conclusion` with its canonical `appStatus`; message parsing and
foreign-profile fallback are prohibited.

## Acceptance matrix

P8-A1 registers the following executable scenarios as pending specifications.
They name future observable behavior and its Phase 8 acceptance mapping; their
runtime behavior is intentionally not implemented here.

| Scenario | Future observable expectation | Mapping |
| --- | --- | --- |
| Activation ordering and once-only | Eligible callbacks run sequentially in final admitted order exactly once per assembly. | P8-A / CNCF R3-R4 handoff |
| Configured publication and default-off | Absent configuration does nothing; explicit ordered keys publish only selected registered profiles. | P8-A / P8-B default-off and selection |
| Invalid/degraded/failed startup | Invalid configuration or any required non-`complete` result prevents readiness and reports one structured failure. | P8-A readiness/failure |
| Shared manual/bootstrap publication | Bootstrap and protected manual replacement use one atomic publication authority; mutation remains outside MCP. | P8-A publication ownership |
| Deterministic restart replacement | Repeating unchanged input does not duplicate records and later complete input replaces the prior generation atomically. | P8-A restart/idempotency |
| Safe diagnostics | Profile/status/count/evidence fields are bounded while locators, credentials, and exceptions remain absent. | P8-A diagnostic redaction |
| MCP mutation exclusion | Bootstrap adds no MCP mutation operation and `replaceKnowledgeSource` remains absent from MCP. | P8-A surface ownership |
| Managed readiness and MCP attribution | Readiness follows complete bootstrap publication and later development reads retain profile/generation attribution. | P8-C managed acceptance |

The active executable decoder specification separately proves absence/default-off,
valid selector normalization, declaration order, blank/unknown/malformed/
empty rejection, duplicate detection, direct+nested ambiguity, and the
project-identity failure inherited from `BokProfileSelection`.

## Non-goals

P8-A1 does not:

- wire `ComponentActivation` or change production startup behavior;
- load a configured source or publish a generation;
- extract or create a second publication implementation;
- change `BokProfileRegistry`, SIE, REST, Web, MCP, or managed readiness;
- make `replaceKnowledgeSource` MCP-ready or public;
- add a watcher, polling, hot reload, restart controller, or control-center API;
- modify Cozy or CNCF repositories;
- claim focused validation, review, Step completion, Phase completion, commit,
  publication, deployment, or push.
