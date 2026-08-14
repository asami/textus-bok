# Phase 7 Hygiene Ledger — 2026-08-14

Status: persisted Phase 7 closure evidence

## Context

Phase 7 P7-A changed only the approved phase-plan, stable design,
specification, README, strategy, and deferred-work documents. The Step commit
`263eb27891c3e0651a9cc7585f6a0b7bd863eba3` did not stage the pre-existing
CAR/configuration/Web-spec work that remained dirty before and after the
commit.

Normal CAR lint was still collected as review input because `textus-bok` is a
CAR project. The following findings are outside the P7-A paths and do not make
the documentation-only profile-selection contract unsafe. This journal is a
chronological hygiene record; it does not add normative behavior.

## P7-HY-01: Project and Generated CAR Identity Metadata

Disposition: hygiene follow-up.

Evidence:

- `project.component.className.missing` in `project.yaml`;
- `project.component.name.missing` in `project.yaml`;
- `project.scalaPackage.missing` in `project.yaml`; and
- `car-descriptor.contract` in the generated
  `target/textus-bok-0.1.0-SNAPSHOT.car`, whose descriptor has the expected
  version but lacks the expected `name` and `component` values.

Risk: normal CAR lint and later CAR release validation remain incomplete until
the project metadata and generated descriptor agree.

Separation evidence: `project.yaml`,
`src/main/car/component-descriptor.json`, and the related assembly/configuration
work were explicitly preserved and remained unstaged by the P7-A Step commit.
The profile-selection design/spec does not consume these descriptor fields.

Follow-up boundary: complete and validate the already-open CAR identity and
configuration work as its own coherent task. Do not reopen P7-A or change its
selection semantics merely to clear these metadata findings.

## P7-HY-02: Release ABI and Development Tool Coordinate

Disposition: hygiene/release-readiness follow-up.

Evidence:

- `abi.baseline.missing` for `src/main/car/abi-manifest.json`; and
- `build.sbt-cozy-latest` at `project/plugins.sbt:5`, because the project uses
  `0.1.20-SNAPSHOT` during explicit development work.

Risk: compatibility against a previously released CAR is unverified, and the
development plugin coordinate is not a publish-readiness coordinate.

Separation evidence: these warnings pre-date P7-A and neither file is in the
Phase 7 Step or closure path set. The ABI item is already owned by DP-01 in
`docs/phase/deferred-release-work.md`.

Follow-up boundary: retain the SNAPSHOT during development; resolve the ABI
baseline and published plugin coordinate only in an explicitly selected
release-readiness workflow.

## P7-HY-03: Existing Nominal String Wrappers

Disposition: hygiene follow-up.

Evidence: `cml.datatype.nominal-string-wrapper` warns that the existing
string-backed identifiers and values in `src/main/cozy/textus-bok.cml` do not
declare distinct scalar constraints. The warnings cover the established BoK
term, dataset/source, generation/evidence, Knowledge Map, and component
reference datatypes beginning at lines 196 through 468.

Risk: nominal wrappers provide weaker generated scalar constraints than
constrained values or accepted predefined types.

Separation evidence: P7-A changes no CML or executable behavior and freezes
selection semantics independently of the later Phase 7.2 public type
integration. The entire CML file is outside the P7-A Step and closure path set.

Follow-up boundary: evaluate these existing datatypes as one CML modeling task
or within the exact Phase 7.2 types it must touch. Do not sweep unrelated
wrappers into Phase 7 closure.
