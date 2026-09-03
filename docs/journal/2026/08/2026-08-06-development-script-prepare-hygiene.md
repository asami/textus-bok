# Development Script Prepare Separation Hygiene

Status: Open hygiene

## Finding

The normal server consumes a separately generated classpath, but development
SAR/check scripts such as `scripts/run-bok-codex-sar.sh` and
`scripts/check-bok-migration-sar.sh` build CARs through top-level SBT in the
runtime/check path. Preparation has no aggregate entry or digest-backed
freshness manifest comparable to ArtScene.

## Required direction

- Move required local CAR builds, dependency preparation, and runtime evidence
  into one bounded prepare entry routed through the serialized SBT runner.
- Keep the maintained development runtime and SAR verification paths SBT-free.
- Reject absent or stale prepared artifacts by explicit digest evidence.
- Do not rebuild the CAR set for a restart when its inputs are unchanged.

## Completion evidence

The prepare entry succeeds under serialization; runtime/check scripts are
statically free of top-level SBT; current evidence runs; stale or absent
evidence fails before runtime execution.
