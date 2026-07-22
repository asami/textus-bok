# Phase 1 Checklist: CAR Baseline

- [x] The project is scaffolded as an independent Cozy CAR.
- [x] `project.yaml` is the source for component identity, Scala and Cozy
  runtime versions, CNCF, and CAR library dependency metadata; the sbt-cozy
  build-plugin coordinate remains in `project/plugins.sbt` for sbt bootstrap.
- [x] CML generation produces typed operation inputs and outputs.
- [x] Generated Scala sources compile with the handwritten component factory.
- [x] The generated executable specification passes.
- [x] `cozyBuildCAR` creates the versioned CAR artifact.
- [x] `ai/directive`, `AGENT.md`, and `RULE.md` provide the standard directive
  layout.
- [x] README, strategy, phase, and checklist navigation describe the baseline
  and its next boundary.
- [x] Release publication of the development `sbt-cozy` coordinate is deferred
  as a (Future Development Candidate: DP-01) to
  `docs/phase/deferred-release-work.md`.

Phase 1 closes only while every item remains checked and the verification
evidence remains recorded in `phase-1.md`.
