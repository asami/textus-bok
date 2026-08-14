# Phase 7.1 Checklist: Profile Registry and Resolution

This checklist is the authoritative Phase 7.1 state ledger. It was split from
the original open Phase 7 on 2026-08-14 and exclusively owns pre-split P7-02.
The summary and current step are recorded in `phase-7.1.md`.

## P7-02: Profile Registry and Resolution

- [ ] A private registry represents the published `simplemodeling.org` source,
  an explicitly prepared `simplemodeling-org` generation, and project-local
  sources keyed by explicit project identity.
- [ ] Component/SAR configuration binds profile identities to admitted logical
  resources without exposing raw locations through public read operations.
- [ ] Resolution uses CNCF resource access, accepts only complete generations,
  preserves source evidence, and prevents development/project data from
  claiming or replacing the official profile.
- [ ] Executable specifications cover deterministic resolution, duplicate
  binding rejection, degraded-generation retention, missing profiles, and
  absence of direct filesystem/network/ambient-context discovery.

Phase 7.1 closes only while every P7-02 item remains checked and validation
evidence remains recorded in `phase-7.1.md`.
