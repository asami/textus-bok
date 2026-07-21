# BoK Migration SAR

This temporary SAR loads the legacy BoK read surface in SIE and the replacement
read surface in Textus BoK as separate main components. Textus Scraper supplies
SIE's declared scraper component API.

Run the complete local lifecycle from the Textus BoK project root:

```bash
scripts/check-bok-migration-sar.sh
```

The check builds all three CARs, assembles the descriptor as a temporary SAR,
starts one owned loopback CNCF server, and loads the same metadata-only fixture
through the old SIE ingestion path and the new BoK replacement path. It then
uses only the shared `/mcp` endpoint to verify:

- four old/new operation pairs have equal generated input schemas;
- tool identities are sorted, deterministic, and collision-free;
- term search, term explanation, component-reference search, and exact lookup
  return equivalent semantic results; and
- both component-qualified tool sets remain visible during migration.

The script refuses a non-loopback URL or an already-running server, owns the
listener it starts, and removes its temporary SAR workspace on exit. The SAR is
a migration fixture only and is removed after consumers switch to Textus BoK.
