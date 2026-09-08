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

The stable failure code remains the internal `Reason` facet classification and
is explicitly materialized as `Conclusion.status.appStatus`. `appStatus` is the
canonical structured transport projection for REST, Web, and MCP consumers;
do not parse messages, promote `Reason` framework-wide, or return a fallback
or foreign result when a selection fails.

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

### P8-C Managed Development Bootstrap

The managed development path is an explicit build/configure/restart/read
sequence. Generate a private WIP snapshot with the repository-owned preparer;
it archives an exact accepted SimpleModeling.org commit and runs the snapshot's
`etc/runweb-wip.sh` without changing the source checkout:

```sh
snapshot_root="$(mktemp -d /private/tmp/textus-bok-simplemodeling-wip.XXXXXX)"
scripts/test/prepare-simplemodeling-wip-snapshot.sh \
  --version 1.0.56 \
  --output-dir "$snapshot_root"
source_root="$snapshot_root/website.d"
```

The preparer accepts only this proven 1.0.56 baseline; no later historical
snapshot or media-specific adapter is part of the managed path.

The historical `runweb-wip.sh` keeps its own `dox` and `cozy` commands. The
preparer supplies the historical `cozy` calls through the verified formal
launcher `/Users/asami/src/dev2025/cozy/out.d/bin/launcher`, whose default
SHA-256 is
`2998aac2317fca4941467ca157abe93c8bcffc282ec7cb6e79af1ef108a3f885`.
The launcher uses the prepared classpath
`/Users/asami/src/dev2025/cozy/target/classpath.txt`, whose default SHA-256 is
`c790d46829c0b6c683a44f48f31fd2f9ecbce2bfcbb85a32c92ade34583401f2`.
`--cozy-launcher PATH`, `--cozy-launcher-sha256 DIGEST`, `--cozy-classpath PATH`,
and `--cozy-classpath-sha256 DIGEST` provide explicit overrides;
`TEXTUS_SIMPLEMODELING_COZY_LAUNCHER`,
`TEXTUS_SIMPLEMODELING_COZY_LAUNCHER_SHA256`,
`TEXTUS_SIMPLEMODELING_COZY_CLASSPATH`, and
`TEXTUS_SIMPLEMODELING_COZY_CLASSPATH_SHA256` provide the corresponding
environment defaults. The selected launcher must already be a regular,
executable, non-symlink file with the expected digest; the selected classpath
must be a nonempty regular, non-symlink file with its expected digest. The
harness never prepares or updates Cozy or SBT. It writes one private executable
`.cozy-tool-bin/cozy` shim outside `website.d`; its only execution line safely
executes the verified launcher with `--classpath-file <verified classpath>
--main-class cozy.Cozy -- "$@"`. The historical command gets only that
snapshot-local tool directory in `PATH` and the producer's existing
`node_modules` through `NODE_PATH`, preserving archived source and toolchain
isolation without a symlink, inherited `MAIN_CLASS`, or dynamic `build.sbt`
inference.

The historical article-media descriptors for object-modeling and
knowledge-modeling require four already-rendered, ignored `final.mp4` inputs
that are not included by the Git archive. The preparer reads them from the
frozen read-only runtime-evidence root
`/Users/asami/src/dev2025/simplemodeling-org` (override with
`--runtime-evidence-root PATH` or
`TEXTUS_SIMPLEMODELING_RUNTIME_EVIDENCE_ROOT`) and accepts only these exact
relative files and SHA-256 identities:

| Relative runtime evidence path | SHA-256 |
| --- | --- |
| `target/media/development-process/object-modeling/ja/final.mp4` | `7af98dc34851b30e060e8c6e99b4f7e18e6ad61a7d3c9e8baf79e5e52f826135` |
| `target/media/development-process/object-modeling/en/final.mp4` | `a880a4d4871c766e136d7a6edd5e7321826c119dbff4344fbc5d2f7226b71977` |
| `target/media/development-process/knowledge-modeling/ja/final.mp4` | `123a2210df82ad98929c47059fce435b8f0e41961aafb43c66beb04f4af5afea` |
| `target/media/development-process/knowledge-modeling/en/final.mp4` | `8f71997365f46ca9a53fef8aaf94fa95292d70a4cb7b45971f00b6b9f59b1afc` |

Each source must be a nonempty regular non-symlink file with its exact
lowercase digest. The four files alone are copied to matching paths in the
private snapshot before `runweb-wip.sh`; they are producer runtime evidence
for article registration, not semantic glossary/RDF inputs, and the producer
checkout remains read-only. The four digest options and their corresponding
`TEXTUS_SIMPLEMODELING_*_FINAL_MP4_SHA256` environment values are test-fixture
overrides only; no other `target` content is admitted.

Accepted snapshots reuse only ignored producer build/toolchain artifacts under
exact lockfile identity: the producer root `node_modules` must be a real
directory, both producer `package-lock.json` files must byte-match the
archived snapshot lockfiles, and the producer Antora `ui-bundle.zip` must be a
nonempty regular file rather than a symlink. The preparer copies the UI bundle
and the four verified article-video inputs into matching archived paths and
binds the historical run to
the producer checkout's existing `node_modules` through `NODE_PATH`. It does
not copy source, content, website output, metadata, or dependencies, and it
does not install or download anything.

The archived `etc/runarcadiasite.sh` invokes `arcadia site` without declaring
the libraries required by its historical site. The preparer therefore validates
the already prepared Arcadia classpath
`/Users/asami/src/dev2025/arcadia/target/classpath.txt`, whose default SHA-256
is `71d017afca9c296ee6653fd7ebd7bd68c1cc51cb5128b8b3a5b035c3d3e6b8bc`.
`--arcadia-classpath PATH` and `--arcadia-classpath-sha256 DIGEST`, together
with `TEXTUS_SIMPLEMODELING_ARCADIA_CLASSPATH` and
`TEXTUS_SIMPLEMODELING_ARCADIA_CLASSPATH_SHA256`, are explicit test-fixture
overrides; the selected classpath must be a nonempty regular non-symlink file
with that lowercase digest. In the private snapshot only, it creates
`src/main/website/WEB-INF/webapp.conf` when absent, containing exactly
`name: material-kit-pro`, and generates an adjacent executable `arcadia` shim.
The shim rejects every operation except `site`, then executes the verified
formal launcher with Arcadia's classpath and
`arcadia.standalone.Standalone`, followed by `site --library
src/main/website,src/main/arcadialib` and the untouched historical positional
arguments. This marker and shim are producer-compatibility inputs only; no
Arcadia source, library, template, or application argument is rewritten.

The accepted historical baseline is the complete private WIP snapshot at
`/private/tmp/textus-bok-p8c-wip-1.0.56-r4/website.d`, archived from commit
`f8ada934ad295f618d51a0d769847a9ffdd4bfbf`. Its KnowledgeSource declares the
four resource kinds `glossary-terms`, `rdf-jsonld`, `rdf-turtle`, and
`rdf-graph-summary`; its glossary contains 215 terms, and the selected
`literate-modeling:story`, `literate-modeling:narrative`, and
`literate-modeling:literate-modeling` identities are absent. The clean accepted
current publication is SimpleModeling.org commit
`055061c2f97315fb51bca6a0f2f0b3cbfacfc5eb` (version 1.0.58.2) at
`/Users/asami/src/dev2025/simplemodeling-org/website.d`. Its manifest declares
all four existing nonempty resources at `metadata/glossary/terms.json`,
`rdf/site.jsonld`, `rdf/site.ttl`, and `metadata/rdf/graph.json`; its glossary
contains 212 terms and all three selected identities. Its KnowledgeSource
manifest and glossary were independently cmp-verified byte-identical to the
current `website.d`; the tracked accepted Phase 29 publication in the
SimpleModeling.org repository at `src/main/website` is that same verified
content. Each of the four declared resource paths is checked for
exact presence and nonempty regular-file content before use. Do not regenerate
or mutate this read-only current publication.

Managed acceptance is a complete replacement sequence: configure and start the
1.0.56 baseline, probe its exact generated count and identity absence, then
configure and restart with the current 1.0.58.2 publication and probe its exact
generation, count, and identity presence. A malformed candidate must fail
before readiness. Finally restore the same current `website.d`, restart through
Control Center, and leave that publication live; append semantics are not a
valid substitute because the current complete generation removes terms.

Pass the resulting `website.d` root to
`scripts/configure-simplemodeling-development.sh`. The writer validates the
`cncf.knowledge-source.v1` manifest and its declared resources, derives a
content-bound `sha256:` generation, and atomically writes a marked private
`.textus/config.yaml` containing one development registry binding, the
explicit development bootstrap selector, in-memory SIE policy, and the exact
read-only file-root policy. It refuses an unmarked existing config and accepts
`--output PATH` for isolated tests.

Use the generated root from the executable baseline flow above:

```sh
scripts/configure-simplemodeling-development.sh \
  --source-root "$source_root"
```

Restart the managed Textus BoK instance only through the normal Textus Control
Center/supervisor lifecycle. Then run the read-only MCP probe, supplying the
source term count, one expected-present term identity, one expected-absent term
identity, and the generation emitted by configuration:

```sh
scripts/test/probe-managed-bok.py \
  --source-root "$source_root" \
  --present-term-id architecture:availability \
  --absent-term-id literate-modeling:story \
  --expected-term-count 215 \
  --generation sha256:40f96e99b2308e0474584513ff7602928008d57de8e3f3bd2494a5aa83402cec
```

The probe checks loopback readiness, exact development attribution, source
term counts, present/absent term behavior, and MCP's permanent mutation
exclusion without calling `replaceKnowledgeSource`. A malformed manifest,
unsafe reference, missing resource, or malformed glossary is rejected before
the existing config changes; do not restart that candidate. Rollback is an
explicit restoration of the last accepted `website.d`, followed by the same
writer, Control Center restart, and probe. There is no watcher, polling,
hot-reload, restart controller, or Control Center API in this path.

For final restoration, use the current SimpleModeling.org `website.d` as a
separate source root. Its accepted current values are
`literate-modeling:story` present, `architecture:availability` absent, 212
terms, and generation
`sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f`:

```sh
current_source_root="/Users/asami/src/dev2025/simplemodeling-org/website.d"
scripts/configure-simplemodeling-development.sh \
  --source-root "$current_source_root"
```

Restart `textus-bok` through the normal Textus Control Center/supervisor
lifecycle, then run the current read-only probe:

```sh
scripts/test/probe-managed-bok.py \
  --source-root "$current_source_root" \
  --present-term-id literate-modeling:story \
  --absent-term-id architecture:availability \
  --expected-term-count 212 \
  --generation sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f
```

### P7-E1 Representative Profile-Selection Preparation

The representative profile-selection SAR is prepared at
examples/bok-profile-selection-sar and is launched by
scripts/test/check-bok-profile-selection-sar.sh. It composes the existing Textus
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

Prepared fixture, lifecycle, probe, and executable-specification artifacts do
not substitute for live runtime evidence. The Phase 7.4 release gate alone
owns live scripts/test/check-bok-profile-selection-sar.sh execution, the full test
suite, CAR build/lint, final review, and closure. The incomplete- and
unknown-project probes require MCP's exact
`structuredContent.error.appStatus` and Static Form's HTTP-200 escaped
`error.appStatus` property, with no fallback selection attribution.
Expected live markers are:

```text
BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 web_maps=4 mcp_terms=4 negative_rest_terms=4 negative_mcp_terms=4 negative_rest_maps=4 negative_web_maps=4 mcp_failures=2 web_failures=2
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
  scripts/test/run-bok-knowledge-map-sar.sh start
```

Executable specifications must cover normalization failures, complete and
degraded replacement, stale removal, exact/candidate distinction, every
reliability state, source-scoped attribution, bounded overfetch, CML surface,
and MCP deny-by-default behavior.
