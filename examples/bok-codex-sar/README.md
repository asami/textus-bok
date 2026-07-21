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
The server process is registered as the user launchd job
`${TEXTUS_BOK_CODEX_LAUNCH_LABEL:-org.textus.bok-codex-sar}`, so it remains
available after the starting shell exits and is removed by `stop`.

The corresponding global Codex configuration is
[`codex-config.toml`](codex-config.toml). It allows only the four BoK-owned read
tools. Restart Codex or open a new task after changing the global configuration.

The included fixture is operational smoke data, not a production BoK catalog.
Set `TEXTUS_BOK_CODEX_FIXTURE_ROOT` to another canonical KnowledgeSource root
before starting when validating real BoK metadata.
