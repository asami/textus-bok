# BoK Profile Selection SAR

This representative SAR prepares one isolated four-profile verification for the
Phase 7.4 profile-selection contract. It keeps one complete fixture generation
per logical selection and uses distinct terms, Knowledge Map nodes, and source
evidence so that a read cannot silently union or fall back to another profile.
The runtime check is prepared here but has not been run by this implementation
Slice; live execution belongs to the Phase release gate.

The private registry contains exactly these bindings:

| Selection | Dataset | Source | Generation | Registry evidence URI |
| --- | --- | --- | --- | --- |
| omitted (`official`) | `profile-official-dataset` | `profile-official-source` | `2026-08-15T00:00:00Z` | `https://evidence.example/textus-bok/profile-official` |
| `development` | `profile-development-dataset` | `profile-development-source` | `2026-08-15T01:00:00Z` | `https://evidence.example/textus-bok/profile-development` |
| `project`, `project-alpha` | `profile-project-alpha-dataset` | `profile-project-alpha-source` | `2026-08-15T02:00:00Z` | `https://evidence.example/textus-bok/profile-project-alpha` |
| `project`, `project-beta` | `profile-project-beta-dataset` | `profile-project-beta-source` | `2026-08-15T03:00:00Z` | `https://evidence.example/textus-bok/profile-project-beta` |

Each fixture root contains only its own
`cncf.knowledge-source.v1` manifest, glossary, and
`cozy.rdf-graph-summary.v1` graph. The private configuration supplies file URI
resources under the fixture root; callers cannot supply a source location.

Prerequisites are the existing SNAPSHOT CARs for Textus BoK, Textus Semantic
Integration Engine, and Textus Scraper, a compatible `cncf` runtime, Python
3.10 or newer, `zip`, `curl`, and `lsof`. The one-shot lifecycle is:

```sh
scripts/check-bok-profile-selection-sar.sh
```

The script does not build, publish, or mutate the source repositories. It
copies the pre-existing CARs and descriptor into a private temporary SAR
workspace, writes a private YAML runtime configuration, starts one loopback
server with no project classpath, runs the probe, and removes only that
temporary workspace. Useful overrides are:

- `TEXTUS_SIE_ROOT`, `TEXTUS_SCRAPER_ROOT`, `TEXTUS_SIE_CAR`,
  `TEXTUS_BOK_CAR`, and `TEXTUS_SCRAPER_CAR` for repository/CAR locations;
- `TEXTUS_BOK_PROFILE_SELECTION_SAR_FIXTURE_ROOT` and
  `TEXTUS_BOK_PROFILE_SELECTION_SAR_DESCRIPTOR` for representative inputs;
- `PYTHON_BIN` for the Python 3.10+ interpreter when the default `python3` is
  not suitable;
- `CNCF_BIN`, `CNCF_VERSION`, `CNCF_RUNTIME_DEV_DIR`, `CNCF_SERVER_PORT`, and
  `CNCF_HTTP_BASEURL` for the runtime; and
- `BOK_PROFILE_SELECTION_SAR_STARTUP_TIMEOUT_SECONDS` and
  `BOK_PROFILE_SELECTION_SAR_SHUTDOWN_TIMEOUT_SECONDS` for bounded lifecycle
  waits.

On a successful live run the probe prints
`BOK_PROFILE_SELECTION_SAR_OK profiles=4 rest_terms=4 rest_maps=4 web_maps=4 mcp_terms=4 negative_rest_terms=4 negative_mcp_terms=4 negative_rest_maps=4 negative_web_maps=4`,
followed by the lifecycle marker
`BOK_PROFILE_SELECTION_SAR_LIFECYCLE_OK profiles=4`.

The probe verifies the resolved profile, optional project identity, dataset,
source, generation, registry evidence, record evidence, exact fixture marker,
and exact graph node on positive REST and Static Form Web. MCP `tools/list`
verifies exactly the four qualified MCP read-tool names, and the probe invokes
qualified `searchTerms` for four positive and four negative profile term checks.
For every selected profile it also uses the next profile's foreign marker and
graph node to verify no-match, empty results/nodes/relationships, selected-
profile attribution, and no foreign leakage on negative REST search, qualified
MCP search, REST Knowledge Map, and Static Form Web map checks; the Web and REST
negative map projections must agree. Knowledge Map node and topology evidence is
verified only through REST and Static Form Web and remains outside MCP. The
final marker reports four positive and four negative checks for each of those
surfaces. It also verifies structured incomplete-project and unknown-project
failures, with no fallback. Source replacement remains a protected administrative
operation and is not an MCP tool. This example does not change either boundary.
