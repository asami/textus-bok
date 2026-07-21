# Textus BoK Development Strategy

## Purpose

Textus BoK owns BoK terminology, component-existence knowledge, source
interpretation, and BoK-specific MCP operations. Generic knowledge federation
belongs to Textus Semantic Integration Engine, while detailed CBD usage
guidance belongs to Textus CBD Support.

## Development Principles

- Keep BoK domain models and match semantics inside this CAR.
- Access SIE only through its public component contract.
- Read source material through the CNCF resource DSL.
- Keep generated component surfaces separate from handwritten domain behavior.
- Preserve deterministic behavior with executable specifications before
  migrating MCP consumers.

## Phase Overview

1. Phase 1: CAR Baseline
2. Phase 2: BoK Domain and Federation Baseline
3. Phase 3: Operational Documentation
4. Phase 4: BoK MCP Migration
5. Phase 5: SAR and CBD Handoff Verification

## Current Priority

Phase 3 is complete. The BoK-owned model, CNCF resource DSL normalization,
provider-neutral SIE replacement, and domain matching states have executable
specifications, and packaged operator/reference plus repository developer
documentation define ingestion, matching, MCP policy, and CBD handoff. Phase 4
BoK MCP migration is the current priority.
