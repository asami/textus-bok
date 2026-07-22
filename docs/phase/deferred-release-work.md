# Deferred Release Work

Status: deferred until the next Textus BoK release-readiness phase starts.

This ledger records work intentionally outside the completed Phase 1–5 scope
and outside the current Phase 6 Knowledge Map scope. It is the relocation
target required by the completed-phase checklists. These items must not be
started as incidental Phase 6 work; they become active only when an explicitly
selected release-readiness phase takes ownership.

## DP-01: Released ABI Baseline and Build-Plugin Coordinate

Source phases: Phase 1, Phase 3, and Phase 4.

At release-readiness time:

- publish or otherwise consume the required released `sbt-cozy` coordinate in
  place of the current development SNAPSHOT as appropriate;
- establish the first released CAR ABI baseline without overwriting any
  published coordinate locally; and
- validate normal repository resolution, CAR lint, ABI comparison, and release
  packaging against the selected release versions.

This item is release-track work. It does not block development-SNAPSHOT CAR
builds or the Phase 6 Knowledge Map implementation.

## DP-02: Live `meta.help` Verification

Source phase: Phase 3.

At release-readiness time, run `meta.help` in a representative assembled
runtime, compare the live generated Help selectors with the packaged CML/API
contract, and record the evidence in the operator/reference documentation.

This is an operational verification gap, not a change to the generated
component contract.

## DP-03: Cold Discovery Runtime Hardening

Source phase: Phase 5.

At release-readiness time, measure cold component discovery using the selected
release artifact set, set and justify an operational timeout from reproducible
evidence, and remove the provisional 900-second development workaround or
record the approved supported bound. The verification must distinguish slow
discovery from component startup failure and retain provider-backed SAR
coverage.

This item does not reopen the completed provider-backed smoke verification in
Phase 5. It hardens the release operational bound.
