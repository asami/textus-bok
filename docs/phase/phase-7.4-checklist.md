# Phase 7.4 Checklist: Representative Verification and Closure

This checklist is the authoritative Phase 7.4 state ledger. It was split from
the original open Phase 7 on 2026-08-14 and exclusively owns pre-split P7-05.
The summary and current step are recorded in `phase-7.4.md`.

## P7-05: Verification, Documentation, and Closure

- [ ] Focused executable specifications cover profile vocabulary, resolver
  isolation, read filtering, attribution, defaulting, Web forms, MCP schemas,
  authorization, and failure behavior.
- [ ] One representative SAR registers official, development, and two distinct
  project-local generations and proves that no request observes records from a
  non-selected profile or project.
- [ ] REST, Static Form Web, and MCP apply equivalent profile resolution and
  attribution to their respective read operations, including resolved
  generation/evidence and structured unavailable behavior; the Knowledge Map
  remains outside MCP.
- [ ] The full Textus BoK test suite, `cozyBuildCAR`, normal CAR lint, and the
  representative selection checks pass with recorded evidence.
- [ ] README, strategy, stable design/specification, developer/operator guides,
  phase evidence, and final review describe the delivered surface and remaining
  boundaries accurately; validated work is committed and Phase 7.4 is closed.

Phase 7.4 closes only while every P7-05 item remains checked and validation
evidence remains recorded in `phase-7.4.md`.
