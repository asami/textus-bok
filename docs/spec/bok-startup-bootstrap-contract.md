# BoK Startup Bootstrap Contract

Status: Phase 8 P8-C managed-runtime acceptance and operator configuration

## Scope and authority

This private specification defines the P8-B consumer implementation of the
already accepted P8-A configuration and supplier handoff, together with the
bounded P8-C operator procedure for accepting a generated SimpleModeling.org
`website.d` source. The procedure observes the normal managed lifecycle and
MCP reads; it does not add a lifecycle API or another publication path.

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

P8-B implements and specifies the following observable scenarios. P8-C managed
runtime/readiness attribution has completed against the accepted baseline and
current SimpleModeling.org publication.

| Scenario | Observable expectation | Mapping |
| --- | --- | --- |
| Activation ordering and once-only | CNCF invokes opted-in components sequentially in final admitted order once per assembly; the BoK callback is inert outside Server mode. | CNCF R3-R4 / P8-B |
| Configured publication and default-off | Absent configuration does nothing; explicit ordered keys publish only selected registered profiles. | P8-B |
| Invalid/degraded/failed startup | Invalid configuration or any required non-`complete` result stops later profiles and returns one structured failure. | P8-B |
| Shared manual/bootstrap publication | Bootstrap and protected manual replacement use one atomic publication authority; mutation remains outside MCP. | P8-B |
| Deterministic restart replacement | Repeating unchanged input replaces the same logical generation without duplicate records. | P8-B |
| Safe diagnostics | Profile/status/count/evidence fields are bounded while private locators, credentials, and raw failures remain absent. | P8-B |
| MCP mutation exclusion | Bootstrap adds no MCP mutation operation and `replaceKnowledgeSource` remains absent from MCP. | P8-B |
| Managed readiness and MCP attribution | Readiness follows complete bootstrap publication and later development reads retain profile/generation attribution. | P8-C external acceptance probe |
| P8-C managed-runtime acceptance | The accepted baseline and current publication start through the managed lifecycle, expose their exact counts and identity transitions, and restore the current publication. | P8-C managed-runtime acceptance |

The active executable decoder specification separately proves absence/default-off,
valid selector normalization, declaration order, blank/unknown/malformed/
empty rejection, duplicate detection, direct+nested ambiguity, and the
project-identity failure inherited from `BokProfileSelection`.

## P8-C Managed Development Acceptance Harness

The managed acceptance uses only the generated project output and the normal
Control Center/supervisor lifecycle. The source repository is never used as a
runtime working directory and is never modified by these tools. From the
Textus BoK repository, prepare one of the accepted historical WIP snapshots:

```sh
snapshot_root="$(mktemp -d /private/tmp/textus-bok-simplemodeling-wip.XXXXXX)"
scripts/test/prepare-simplemodeling-wip-snapshot.sh \
  --version 1.0.56 \
  --output-dir "$snapshot_root"
source_root="$snapshot_root/website.d"
```

The preparer accepts only this proven 1.0.56 baseline; no later historical
snapshot or media-specific adapter is part of the managed path.

The archived historical `etc/runweb-wip.sh` retains its own `dox` and `cozy`
commands. The preparer resolves those `cozy` calls through the verified formal
launcher `/Users/asami/src/dev2025/cozy/out.d/bin/launcher`, with default
SHA-256
`2998aac2317fca4941467ca157abe93c8bcffc282ec7cb6e79af1ef108a3f885`.
The launcher uses the already prepared classpath
`/Users/asami/src/dev2025/cozy/target/classpath.txt`, with default SHA-256
`c790d46829c0b6c683a44f48f31fd2f9ecbce2bfcbb85a32c92ade34583401f2`.
`--cozy-launcher PATH`, `--cozy-launcher-sha256 DIGEST`, `--cozy-classpath PATH`,
and `--cozy-classpath-sha256 DIGEST` are explicit overrides, with
`TEXTUS_SIMPLEMODELING_COZY_LAUNCHER`,
`TEXTUS_SIMPLEMODELING_COZY_LAUNCHER_SHA256`,
`TEXTUS_SIMPLEMODELING_COZY_CLASSPATH`, and
`TEXTUS_SIMPLEMODELING_COZY_CLASSPATH_SHA256` as environment inputs. The
selected launcher is required to be a regular executable non-symlink file and
to match its lowercase SHA-256; the selected classpath is required to be a
nonempty regular non-symlink file and to match its lowercase SHA-256. The
harness does not prepare or update Cozy or SBT. Before the historical command,
it writes one private executable `.cozy-tool-bin/cozy` shim outside `website.d`.
The shim's only execution line safely executes the verified launcher with the
exact argv `--classpath-file <verified classpath> --main-class cozy.Cozy -- "$@"`;
no symlink or inherited `MAIN_CLASS` is used. The historical command
gets only that snapshot-local tool directory in `PATH` and the producer's
existing `node_modules` through `NODE_PATH`, retaining archived source and
toolchain isolation without dynamic `build.sbt` inference.

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
private snapshot before the historical command. They are producer runtime
evidence required by article registration, not semantic glossary/RDF inputs;
the producer checkout remains read-only. The four digest options and their
corresponding `TEXTUS_SIMPLEMODELING_*_FINAL_MP4_SHA256` environment values are
test-fixture overrides only; no other `target` content is admitted.

The preparer archives only the accepted 1.0.56 commit
`f8ada934ad295f618d51a0d769847a9ffdd4bfbf` into a private temporary directory
and runs only that snapshot's `etc/runweb-wip.sh`. It leaves the generated
baseline snapshot available for the following configuration step. The generated root
must contain `metadata/cncf/knowledge-source.json` and its declared glossary
resource. Accepted snapshots reuse only ignored producer build/toolchain
artifacts under exact lockfile identity: the producer root `node_modules` must
be a real directory, the producer root and Antora UI `package-lock.json` bytes
must each match the corresponding archived snapshot lockfile, and the
producer `src/main/antora-ui/build/ui-bundle.zip` must be a nonempty regular
file, not a symlink. The preparer copies the UI bundle into the matching
archived build path and the four verified article-video inputs into matching
archived paths, then binds the historical run's `NODE_PATH` to the
producer's existing `node_modules`. It never copies source, content, website
output, metadata, or `node_modules` into the snapshot, and never installs or
downloads dependencies; source and content remain the historical archive.

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

Configure the selected generated `website.d` with the thin writer:

```sh
scripts/configure-simplemodeling-development.sh \
  --source-root "$source_root"
```

The writer validates the `cncf.knowledge-source.v1` manifest and every
declared resource, derives a `sha256:` generation from their content, and
atomically writes the marked project-local `.textus/config.yaml`. The private
configuration contains exactly one `development` registry binding, the
explicit `development` bootstrap selector, in-memory RDF/Vector SIE policy,
and the exact read-only file-root policy. An existing unmarked config is
never overwritten. Tests may pass `--output PATH` to write another explicit
destination; the normal operator destination remains project-local.

After the build and configuration steps, restart the registered Textus BoK
instance through the normal Textus Control Center/supervisor interface. Do not
invoke a second publication operation or a repository-owned restart wrapper.
When the instance is ready on the assigned local endpoint (the BoK default is
`18014`), run the read-only MCP probe with the expected source count and term
identities:

```sh
scripts/test/probe-managed-bok.py \
  --source-root "$source_root" \
  --present-term-id architecture:availability \
  --absent-term-id literate-modeling:story \
  --expected-term-count 215 \
  --generation sha256:40f96e99b2308e0474584513ff7602928008d57de8e3f3bd2494a5aa83402cec
```

The probe checks the local system status, the four BoK read tools, exact
`development`/dataset/source/generation/evidence attribution, one present term,
an empty absent-term result, the source term count, and the permanent absence
of `replaceKnowledgeSource` from MCP. It never invokes replacement and accepts
only an HTTP loopback base URL.

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

If metadata is malformed or a declared resource is missing, the configuration
writer fails before the existing config is changed; do not restart the managed
instance with that candidate. The Control Center diagnostic must remain a
bounded lifecycle failure and must not expose the source path, credentials,
raw exception, or generated content. To roll back, restore the previously
accepted generated `website.d` and rerun the writer against that root, then
restart through Control Center and rerun the probe. The writer's atomic update
and marked-file guard make restoration explicit and failure-safe.

Updates remain build-then-configure-then-restart operations. This harness adds
no watcher, polling, hot reload, restart controller, Control Center API,
network publication, or deployment behavior.

## Non-goals

P8-B does not:

- add a watcher, polling, hot reload, restart controller, control-center API,
  real `website.d` acceptance, or bound-URL observation;
- make `replaceKnowledgeSource` MCP-ready or public, or change its response;
- change public API, schema, or configuration keys;
- modify Cozy or CNCF repositories;
- claim focused validation, review, Step completion, Phase completion, commit,
  publication, deployment, or push.
