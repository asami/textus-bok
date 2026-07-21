# BoK Codex SAR

This operational SAR provides BoK-owned terminology and component-existence
knowledge to Codex. It composes Textus BoK with SIE's provider-neutral component
API and Textus Scraper. The old SIE BoK MCP operations are disabled by the SAR;
SIE's generic retrieval tools remain independent.

Manage the local server from the Textus BoK project root:

```bash
scripts/run-bok-codex-sar.sh start
scripts/run-bok-codex-sar.sh status
scripts/run-bok-codex-sar.sh stop
```

The default endpoint is `http://127.0.0.1:18005/mcp`, using the SIE component's
standard server port. `start` builds all three
CARs, assembles the SAR, starts CNCF `0.5.1-SNAPSHOT`, replaces the canonical
metadata-only fixture through Textus BoK, and verifies a live terminology read.
Runtime state and logs are kept under
`${TEXTUS_BOK_CODEX_RUN_DIR:-$HOME/.cncf/textus-bok-codex}`.
The runner places all three current CARs in its owned component directory,
disables CNCF default component repositories, and selects its generated SAR by
explicit file path. This prevents a same-version SNAPSHOT in
`~/.cncf/local/repository` from shadowing the CARs built for the current run.
The server process is registered as the user launchd job
`${TEXTUS_BOK_CODEX_LAUNCH_LABEL:-org.textus.bok-codex-sar}`, so it remains
available after the starting shell exits and is removed by `stop`.
Cold multi-CAR loading may take several minutes; the default startup timeout is
900 seconds and can be overridden with `TEXTUS_BOK_CODEX_STARTUP_TIMEOUT_SECONDS`.
The live replacement/read probe allows 120 seconds by default and can be
overridden with `TEXTUS_BOK_CODEX_PROBE_TIMEOUT_SECONDS` for slower providers.
Operational trials may set `TEXTUS_BOK_CODEX_VIRTUAL_START_AT` to a CNCF
offset-clock start instant. `TEXTUS_BOK_CODEX_PROBE_QUERY` and
`TEXTUS_BOK_CODEX_PROBE_CATEGORY` adapt the readiness probe to another
metadata-only fixture without changing the four-tool BoK contract.

The default provider mode is external-service-free: RDF and Vector DB both use
`in-memory`. Provider-backed verification may set `TEXTUS_SIE_RDF_DB=fuseki`
and `TEXTUS_SIE_VECTOR_DB=chroma` together with the existing Fuseki, Chroma,
provider timeout, and embedding environment settings. The runner forwards those
settings through launchd and explicit CNCF configuration arguments; provider
lifecycle remains owned by the caller.
Only a SHA-256 configuration fingerprint is persisted in runtime state; endpoint
values are not copied into the state file.

Verify both the in-memory defaults and provider override propagation with:

```sh
scripts/check-bok-codex-provider-config.sh
```

The corresponding global Codex configuration is
[`codex-config.toml`](codex-config.toml). It allows only the four BoK-owned read
tools. Restart Codex or open a new task after changing the global configuration.

The included fixture is operational smoke data, not a production BoK catalog.
Set `TEXTUS_BOK_CODEX_FIXTURE_ROOT` to another canonical KnowledgeSource root
before starting when validating real BoK metadata.
