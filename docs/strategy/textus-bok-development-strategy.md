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
2. Phase 2: BoK Domain Model and Source Normalization
3. Phase 3: SIE-backed Dataset Publication
4. Phase 4: BoK MCP Migration
5. Phase 5: SAR and CBD Handoff Verification

## Current Priority

Phase 2 is active. The BoK-owned terminology, component-existence, evidence,
source, request, and response model is defined first; CNCF resource DSL source
normalization follows before any SIE publication or MCP exposure.
