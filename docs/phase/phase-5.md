# Phase 5: SAR and CBD Handoff Verification

## Stage Status

- Status: `IN_PROGRESS`
- Current step: Add Textus CBD Support to the representative SAR and migrate
  the existence-to-detail consumer flow.
- Closure basis: Every Phase 5 checklist item is complete and representative
  SAR, Codex, skill, and CBD handoff evidence is recorded below.

## Objective

Operate Textus BoK as the sole owner of BoK terminology and CAR/SAR existence
MCP tools while SIE remains the domain-neutral federation component and Textus
CBD Support owns component detail and usage guidance.

## BoK Codex SAR Evidence

On 2026-07-21, `examples/bok-codex-sar` introduced an operational SIE, BoK, and
Scraper SAR for Codex use. Its descriptor disables the four transitional SIE
BoK operations while retaining SIE's generic retrieval service. The
repository-owned launcher builds current CARs, runs CNCF `0.5.1-SNAPSHOT` as a
macOS user launchd job, replaces one metadata-only BoK generation, and verifies
terminology search through the public `/mcp` endpoint.

The live probe reported `BOK_CODEX_SAR_OK` at
`http://127.0.0.1:18005/mcp`, the SIE standard server endpoint, with four BoK
tools and no legacy SIE BoK tool.
The global Codex configuration now allowlists only SIE's three generic reads on
the existing SIE endpoint and the four BoK-owned reads on the new endpoint.

This proves the first consumer migration boundary. CBD Support integration,
the `cncf-bok-*` skills, and the complete existence-to-detail workflow remain
open in this phase.
