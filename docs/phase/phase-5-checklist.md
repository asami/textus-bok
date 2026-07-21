# Phase 5 Checklist: SAR and CBD Handoff Verification

- [x] An operational SIE, BoK, and Scraper SAR publishes the four BoK-owned read
  tools and disables the four transitional SIE BoK tools.
- [x] Global Codex MCP configuration separates SIE generic reads from BoK-owned
  terminology and component-existence reads.
- [ ] A representative SAR adds Textus CBD Support as a distinct main component
  without crossing SIE, BoK, or CBD knowledge ownership.
- [ ] Shared `/mcp` discovery has stable identities, deny-by-default narrowing,
  and fail-closed collision behavior across all three components.
- [ ] `cncf-bok-terms` uses Textus BoK and returns evidence-bearing terminology
  grounding.
- [ ] `cncf-bok-components` uses Textus BoK for existence discovery and hands
  exact identities to Textus CBD Support for detail and usage guidance.
- [ ] The complete BoK-to-CBD component-development workflow passes from current
  CAR artifacts and documented operator commands.

Phase 5 closes only when every item is checked and validation evidence is
recorded in `phase-5.md`.
