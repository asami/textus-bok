# From Terminology to Generative Pattern Languages

Date: 2026-11-23
Status: Discussion / architectural hypothesis
Scope: Textus BoK, KnowledgeHub, Pattern Language, Executable Knowledge

## Problem

Defining terms and realizing the generative quality of a pattern language are fundamentally different activities. More detailed definitions alone cannot bridge this gap. A glossary describes what concepts mean; a generative pattern language guides context-sensitive transformations that produce coherent new structures.

## Layers of knowledge

- **Concept**: meaning and boundaries of a term (e.g. Component, Service).
- **Proposition / Relationship**: statements and relations between concepts.
- **Principle**: criteria and heuristics for judgment.
- **Pattern**: reusable context-sensitive problem-solving knowledge, including Context, Problem, Forces, Solution and Resulting Context.
- **Pattern Language**: connected patterns and guidance for selecting, sequencing, composing, adapting and evaluating their application.

These are distinct forms, not merely progressively longer term definitions. A catalog of patterns does not automatically constitute a generative pattern language.

## Generativity as transformation

A useful conceptual model is:

`Pattern: (Context, Forces) -> Transformed Context`

Applying one pattern changes the context, which may make another pattern relevant. Generativity lies in this context-sensitive sequence of transformations and in the quality of the resulting whole, not simply in the combinability of components. The formula is illustrative rather than a complete formal semantics.

A generative system needs: interpretation of the situation; recognition and prioritization of forces; pattern selection; composition and sequencing; transformation; and evaluation against goals, constraints and human values.

## Three distinct kinds of generation

1. **Knowledge generation**: discover and validate reusable patterns from experience, research materials, decisions, failures and evidence.
2. **Structure generation**: use a pattern language to construct a coherent new design or organization. This is the central generativity of pattern languages.
3. **Artifact / executable generation**: realize a design as an executable model or system, where supported by Textus/CNCF.

Do not conflate the three. Execution success is not proof of design quality.

## Division of responsibilities: formal knowledge, AI, humans

BoK should preserve definitions, relationships, constraints, provenance, applicability conditions, counterexamples and evaluation evidence. Pattern languages should organize transformation knowledge and application relationships. Generative AI can help interpret context, identify competing forces, propose patterns and compositions, and discover candidate patterns from materials. Deterministic checks and executable tests should validate formal constraints; human review remains necessary for goals, interpretation and overall quality.

AI reasoning is fallible. Proposed patterns extracted from materials are **Candidates**, not automatically admitted knowledge. Preserve evidence, alternate interpretations, counterexamples, applicability limits, review decisions and history through Knowledge Lake / KnowledgeHub admission workflows.

## Textus implications

Example flow:

`Concepts -> Principles -> Patterns -> Generative Design -> Executable Model (CNCF) -> Tests / Evaluation -> Knowledge Revision`

For CBD, concepts such as Component, Service and Operation are connected to responsibility separation, service design and transaction-boundary patterns. AI-assisted design can propose component structures; CNCF execution and tests provide feedback, but human and domain-specific evaluation are still required.

BoK is the knowledge representation foundation; Pattern Language is an organized system for generative application of knowledge. They should be connected without forcing every BoK term to become a pattern.

## Central hypothesis

**Generativity is not simply a stored property of a knowledge entry. It emerges through interaction between represented knowledge, context, and an interpreting/applying agent.** Historically that agent was human; generative AI may now participate, with explicit validation and human oversight.

KnowledgeHub should support a loop of discovery, interpretation, selection, transformation, evaluation and revision, bringing together human tacit knowledge, AI's implicit learned capabilities and explicit knowledge. This relates to the proposed extension of SECI with two sources of tacit knowledge, but remains an architectural hypothesis to test.

## Open design questions

- How should BoK represent Concept, Proposition, Principle and Pattern while allowing type-specific schemas and free-form commentary?
- What explicit relations express pattern preconditions, resulting contexts, dependencies, conflicts and composition?
- How can a pattern language describe a generative process without overformalizing tacit judgment?
- What evaluation criteria and evidence are needed for pattern admission and revision?
- How should KnowledgeHub distinguish AI proposals, verified constraints, execution results and human judgments?
- How can NICT Heritage Curator extract candidate patterns with provenance and competing interpretations?

This journal records conceptual direction; it does not by itself authorize a phase or schema change.
