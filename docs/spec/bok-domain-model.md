# BoK Domain Model Contract

## Purpose

`textus-bok` owns the typed vocabulary used to interpret BoK terminology and
CAR/SAR existence knowledge. SIE transports provider-neutral documents,
assertions, evidence, and provenance; it does not own these BoK meanings.

## Terminology

A `BokTerm` has a stable BoK identity, title, definition, optional category,
term type, source-owned dataset identity, and attributable `BokEvidence`.
Search and explanation results add match kind, score, and rationale without
changing the curated term.

## Component Existence

A `ComponentReference` proves only that a named CAR or SAR exists. It may carry
organization, catalog, source, and version identity plus evidence. It must not
carry capabilities, dependencies, operations, runtime compatibility, manuals,
or usage guidance; those details belong to `textus-cbd-support`.

## Source And Evidence

`BokKnowledgeSource` identifies one logical resource and generation owned by a
dataset. The resource value is interpreted only through the CNCF resource DSL
in the next implementation step. `BokEvidence` attributes a result to its
logical source and URI and may record source version, observed time, and
freshness.

## Requests And Responses

The component defines typed request and response models for source replacement,
term search and explanation, and component-reference search and lookup. Query
responses use explicit status, evidence-bearing result types, and typed
warnings. The operation declarations are not MCP Ready in this phase.

## Exclusions

This model contract does not implement source reading, SIE dataset publication,
matching behavior, MCP publication, or CBD detail resolution. It exposes no
Fuseki, Chroma, embedding, filesystem, or network type.
