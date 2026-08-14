# Textus BoK Developer Guide

## Ownership Boundary

Keep BoK source interpretation, typed terms, existence-only component
references, match classification, and BoK response construction in this CAR.
Use SIE only through generated component APIs:

- `KnowledgeFederation.replaceDataset` for complete dataset publication;
- `SemanticRetrieval.query` for provider-neutral candidate evidence.

Do not import SIE runtime or provider SPI packages. Do not add Fuseki, Chroma,
embedding, retry, endpoint, credential, filesystem, or network types to CML.

## Profile Preparation And Resolution

Private component/SAR authors must keep the three profile keys distinct:
`official`, `development`, and `project`. `projectId` is present only for
`project`, and it is an explicit logical identity rather than a path, URL,
host, checkout, or provider setting. Each binding has its own `sourceId`,
`datasetId`, `generation`, logical CNCF `resource`, and generation evidence;
the evidence `sourceId` must match the binding source identity. Public reads
must never supply these private resource locations or credentials.

Registry configuration prepares a binding but does not load or admit its
content. The protected administrative replacement operation must be invoked
for each binding identity and generation. Only `complete` publication changes
the readable catalog; degraded or failed replacement retains the prior
complete generation. The packaged operator guide is the operator-facing
companion for this preparation and observation procedure.

Freshness is an optional exact-generation invariant. Without
`freshnessGeneration`, any admitted complete generation is readable. With it,
the observed generation must match exactly; retaining an older complete
generation for atomic rollback does not bypass a `stale` result. Successful
attribution (`resolvedProfile`, optional `projectId`, `datasetId`, `sourceId`,
`generation`, and `evidence`) is the diagnostic source of truth, including the
Knowledge Map source-generation table and no-JavaScript output.

Maintainers must preserve the deterministic responsibility order: normalize
and validate the closed selector, authorize the exact key, locate its exact
binding, require an admitted complete generation, apply freshness, then apply
optional dataset/source compatibility filters. Never probe or fall back to
another profile, infer a project, or expose private bindings when one check
fails. Keep the stable failure codes and their meanings intact:
`invalid-selection`, `project-identity-required`, `unregistered`,
`unavailable`, `stale`, `ambiguous`, `unauthorized`, and
`conflicting-selection`.

The Web selectors invoke the same public Knowledge Map operation and remain
read-only. Resource locations, source registration/replacement, provider
credentials, and MCP readiness stay outside the Web surface. Knowledge Map
and `replaceKnowledgeSource` remain outside the four MCP-ready reads.

## Generated And Handwritten Code

`src/main/cozy/textus-bok.cml` is the public component contract. Cozy-generated
Scala remains under `target/scala-3.3.8/src_managed/main/scala` and is never
edited. `ComponentFactory` replaces generated ActionCalls with handwritten
programs. Runtime classes normalize sources, project SIE documents, retrieve
generic candidates, and classify BoK results.

## Source Adapter Rules

All content reads must start from `BokKnowledgeSource.resource` and use
`ExecutionContext.resources`. Resolve child paths with the CNCF resource DSL.
New resource kinds require:

1. an explicit schema and safe-relative-reference rule;
2. deterministic normalization into existing or deliberately revised CML
   types;
3. duplicate-identity and empty-source behavior;
4. metadata-only executable specifications;
5. no direct filesystem, URL client, environment, or system-property access.

Rendered HTML is not a BoK default. Generic optional HTML indexing remains an
SIE administration capability.

The Knowledge Map admits only Cozy's versioned `rdf-graph-summary` resource.
Graph edges remain factual source topology, every node and edge retains source
evidence, and a component handoff can originate only from an explicit
same-generation `componentRef` validated against the CAR/SAR index. Optional
organization and version are exact constraints; never infer an identity from a
label, identifier, tag, edge, or rendered page.

## Federation Projection

`BokFederationPublisher` maps typed records to opaque SIE metadata. Stable IDs
must remain deterministic and use `(kind, optional organization, name)` as the
component identity. Qualified component document/assertion IDs and assertion
subjects include the organization; an unqualified (`None`) component retains
the legacy ID and subject bytes exactly. Every document and assertion must
retain attributable evidence. Component document metadata includes `datasetId`,
`domain`, and `recordKind` so candidate attribution survives provider retrieval.

Commit the typed catalog only after SIE returns `complete`. Never advance it on
`degraded`, `unavailable`, or failed publication. Replacement is a complete
generation, not an incremental patch.

## Matching Rules

Textus BoK owns exact, candidate, ambiguous, conflict,
insufficient-evidence, and no-match classification. SIE scores are advisory.
Key candidates by dataset, source, and document identity. Filter unrelated
generic results before applying the caller's result limit, using bounded
overfetch up to 100 results per source.

Component lookup identity is `(kind, organization: Option[String], name)`.
Supply organization for an exact namespace match; omitting it spans
organizations and must return no reference when the result is ambiguous.
Version narrows or orders a match but never changes the stable identity.

Do not convert similarity into curated knowledge. Perform reliability
classification before truncating results. Preserve evidence on every returned
term or component reference.

## MCP Policy

MCP publication is default-deny. The component currently marks only
the four BoK read operations ready at operation level: terminology search and
explanation plus existence-only component-reference search and lookup.
`replaceKnowledgeSource` and future mutation or administration operations must
remain non-ready at both service and operation policy levels. CAR/SAR
configuration may only disable declared tools through `cncf.mcp.enabled`,
`cncf.mcp.disabled-services`, and `cncf.mcp.disabled-operations`; configuration
must never be treated as an operation readiness declaration.

## CBD Handoff

Keep `ComponentReference` existence-only. Changes that add capabilities,
dependencies, services, operations, compatibility ranges, manuals, artifacts,
or usage guidance belong in Textus CBD Support. The portable handoff identity
is `name`, `kind`, optional organization/version, and evidence URI; local source
and catalog IDs are contextual evidence only.

For an admitted Cozy graph node, accept a CBD handoff only from explicit
`componentRef.kind` and `componentRef.name` after exact same-generation
CAR/SAR-index validation; if declared, `componentRef.organization` and
`componentRef.version` must match the same index entry exactly. Do not recover a handoff from a node ID, label,
title, tag, relationship, or SIE candidate. Web pages may display that
existence identity but must not invoke CBD Support or render CBD-owned detail.

## Knowledge Map Web Surface

The `textus-bok` Static Form page at `/web/bok/textus-bok/map` selects the
public `bok.bok-retrieval.get-knowledge-map` operation. Keep its query bounded
and read-only. The page's operation result is the sole data source for both
the no-JavaScript tables and local progressive SVG enhancement. Runtime JSON
uses generated snake_case property names, so authored tables and JavaScript
must accept those names while request parameters remain the generated CML
selectors. Use `textContent`, not HTML sinks, and permit evidence navigation
only for explicit HTTP(S) URLs.

## Verification

### P7-E1 Representative Profile-Selection Preparation

The representative profile-selection SAR is prepared at
examples/bok-profile-selection-sar and is launched by
scripts/check-bok-profile-selection-sar.sh. It composes the existing Textus
BoK, Semantic Integration Engine, and Scraper SNAPSHOT CARs without changing
their repositories. The private four-binding model is official,
development, project-alpha, and project-beta; each has distinct source,
dataset, generation, registry-evidence, term, and graph-node identities.
Positive REST terminology and Knowledge Map reads, qualified MCP terminology
reads, Static Form map reads, and cyclic foreign-term/foreign-focus negative
probes are checked for isolation, attribution, no union, and no fallback.
Knowledge Map remains outside MCP and replaceKnowledgeSource remains protected
and non-MCP.

The operator prerequisites are the three existing CAR files, a compatible CNCF
runtime, Python 3.10 or newer, zip, curl, and lsof. The lifecycle refuses
non-loopback URLs and occupied ports, uses a private temporary runtime
directory, and cleans only that directory. Repository and input overrides include TEXTUS_SIE_ROOT,
TEXTUS_SCRAPER_ROOT, TEXTUS_SIE_CAR, TEXTUS_BOK_CAR,
TEXTUS_SCRAPER_CAR, TEXTUS_BOK_PROFILE_SELECTION_SAR_FIXTURE_ROOT, and
TEXTUS_BOK_PROFILE_SELECTION_SAR_DESCRIPTOR. Runtime overrides include
CNCF_BIN, CNCF_VERSION, CNCF_RUNTIME_DEV_DIR, CNCF_SERVER_PORT, and
CNCF_HTTP_BASEURL; PYTHON_BIN selects the Python 3.10+ interpreter when the
default python3 is not suitable. Bounded lifecycle overrides are
BOK_PROFILE_SELECTION_SAR_STARTUP_TIMEOUT_SECONDS and
BOK_PROFILE_SELECTION_SAR_SHUTDOWN_TIMEOUT_SECONDS; the example README
documents the complete list.

P7-E1 has prepared and focused/static-validated the fixture, lifecycle, probe,
and executable specification. That evidence does not substitute for live
runtime evidence: the lifecycle has not yet been executed. The Phase 7.4
release gate alone owns live scripts/check-bok-profile-selection-sar.sh
execution, the full test suite, CAR build/lint, final review, and closure.
Expected live markers are:

```text
BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 web_maps=4 mcp_terms=4 negative_rest_terms=4 negative_mcp_terms=4 negative_rest_maps=4 negative_web_maps=4
BOK_PROFILE_SELECTION_SAR_LIFECYCLE_OK profiles=4
```

Maintainers must update the example README, probe assertions, fixture
identities, and reported surface counts together. Any change to the four
positive profiles or the cyclic negative probes requires corresponding
executable-specification and documentation updates.

Run:

```sh
sbt --batch test cozyBuildCAR
cozy lint car .
TEXTUS_BOK_KNOWLEDGE_MAP_SOURCE_ROOT=/path/to/website.d \
  scripts/run-bok-knowledge-map-sar.sh start
```

Executable specifications must cover normalization failures, complete and
degraded replacement, stale removal, exact/candidate distinction, every
reliability state, source-scoped attribution, bounded overfetch, CML surface,
and MCP deny-by-default behavior.
