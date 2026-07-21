# Phase 5: SAR and CBD Handoff Verification

## Stage Status

- Status: `CLOSED`
- Current step: Complete.
- Closure basis: Every Phase 5 checklist item is complete and representative
  SAR, Codex, skill, and CBD handoff evidence is recorded below.

## Objective

Operate Textus BoK as the sole owner of BoK terminology and CAR/SAR existence
MCP tools while SIE remains the domain-neutral federation component and Textus
CBD Support owns component detail and usage guidance.

## BoK Codex SAR Evidence

On 2026-07-21, `examples/bok-codex-sar` introduced an operational SIE, BoK, and
Scraper SAR for Codex use. During migration, its descriptor disabled the four
transitional SIE BoK operations while retaining SIE's generic retrieval
service. Following extraction, the current SIE CAR no longer declares those
BoK operations and the SAR requires no compatibility disable policy. The
repository-owned launcher builds current CARs, runs CNCF `0.5.1-SNAPSHOT` as a
macOS user launchd job, replaces one metadata-only BoK generation, and verifies
terminology search through the public `/mcp` endpoint.

The live probe reported `BOK_CODEX_SAR_OK` at
`http://127.0.0.1:18005/mcp`, the SIE standard server endpoint, with four BoK
tools and no legacy SIE BoK tool.
The global Codex configuration now allowlists only SIE's three generic reads on
the existing SIE endpoint and the four BoK-owned reads on the new endpoint.

This proves the first consumer migration boundary.

Provider-configurable SAR boundary implemented on 2026-07-21 JST:

- The operational runner retains in-memory RDF and Vector defaults but now
  forwards the established SIE Fuseki, Chroma, provider timeout, and embedding
  settings through launchd and explicit CNCF arguments.
- This removes the configuration blocker for moving KS-14 provider-backed
  smoke to the BoK-owned SAR. The provider-backed lifecycle itself remains open
  until that smoke runs against current artifacts.
- `scripts/check-bok-codex-provider-config.sh` verifies the in-memory defaults
  and captures Fuseki, Chroma, endpoint, and embedding overrides at the CNCF
  process boundary without starting external providers.
- A live restart on the standard `18005` endpoint wrote the requested provider
  fingerprint; a same-configuration start reused the running SAR, while an
  unknown or different fingerprint was rejected without stopping it.
- Subsequent cold discovery exceeded both 240-second and 360-second bounds
  without a component failure. The operational bound is temporarily 900
  seconds so initialization can finish; discovery latency remains an explicit
  runtime-hardening concern rather than being hidden by repeated restarts.

Provider-backed SAR verification completed on 2026-07-21 JST:

- The runner now makes its owned current-CAR directory the SAR descriptor's
  sole component search repository. This prevents an older same-version CAR in
  `~/.cncf/local/repository` from shadowing the artifacts built for the run.
- The KS-14 Fuseki and Chroma smoke passed on SIE's standard `18005` endpoint
  with `BOK_CODEX_SAR_OK`, exposing four BoK-owned tools and no legacy SIE BoK
  tools. RDF assertion object types remained literals across the CAR boundary.
- Replacing metadata-only BoK knowledge caused zero HTML fetches and created no
  HTML dataset. A separate explicit SIE `HtmlIndexer` request then indexed one
  page and reported `SIE_HTML_INDEX_BOUNDARY_OK`.
- After provider verification, the in-memory BoK Codex SAR was restored on
  `http://127.0.0.1:18005/mcp` for normal development use.

Actual Cozy BoK trial compatibility verified on 2026-07-21 JST:

- `BokSourceReader` now accepts Cozy's current
  `cncf.component-reference-index.v1` CAR and SAR resources while preserving
  the older component repository index contract.
- The projection retains only existence identity, kind, version, and public
  metadata evidence. CBD capabilities, dependencies, and usage detail are not
  imported into BoK.
- A focused executable specification passed seven reader behaviors, and the
  rebuilt BoK CAR served a live Fuseki/Chroma trial at the SIE standard
  `18005` endpoint.
- The actual KnowledgeHub source published two terms, one CAR, and one SAR;
  an independent trial check repeated BoK reads and the CBD usage handoff
  before all owned launchd jobs were stopped.

Representative SAR and Codex skill verification completed on 2026-07-21 JST:

- The CBD representative SAR composes SIE, Textus BoK, Textus CBD Support, and
  Textus Scraper as independently owned components. Its four-profile live MCP
  matrix verifies 12 CBD reads, four BoK reads, three generic SIE reads,
  deterministic identities, narrowing, mutation denial, and collision
  rejection.
- The operational BoK runner embeds all three required CARs in its SAR, keeps
  an owned component directory, and selects `textus-bok-codex` by subsystem
  name. Its provider-config check verifies that packaging and startup arguments.
- A live restart passed `BOK_CODEX_SAR_OK` at the SIE standard endpoint
  `http://127.0.0.1:18005/mcp`. The probe now sends MCP protocol revision
  `2025-11-25`; omission is treated as a protocol error rather than an empty
  tool catalog.
- Fresh ephemeral Codex runs loaded `cncf-bok-terms` and
  `cncf-bok-components`. The terms skill used only Textus BoK MCP to return the
  exact `architecture:component` definition and source evidence.
- The components skill used BoK only for the exact
  `car:textus-semantic-integration-engine@0.2.0-SNAPSHOT` existence identity,
  then handed name, kind, and version to CBD Support. CBD reported the exact
  development-version detail gap, a conflicting published `0.1.0` observation,
  and absent usage/dependency metadata without mixing ownership or inventing
  details.
- Both skills passed the system skill validator.
- Final validation passed 18 Textus BoK tests in six suites and 142 SIE tests
  in 29 suites; one explicitly opt-in SIE provider-backed test was canceled.
  Both CAR builds succeeded, and normal CAR lint reported no failure. The
  representative current-artifact baseline passed with 12 CBD, four BoK, and
  three generic SIE reads, 16 denied non-public operations, source ownership,
  and the complete BoK-to-CBD design flow at the standard `18005` endpoint.
- The final review found and corrected stale wording that described removed SIE
  BoK operations as current SAR disable policy. With every checklist item and
  closure evidence complete, Phase 5 is closed.
