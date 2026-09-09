# BoK Cross-model Integration Contract

Status: Draft detailed contract
Date: 2026-09-05

## Purpose

This document refines the SimpleModeling.org model integration specification for `textus-bok`.

SimpleModeling.org owns the high-level semantic integration rules across Object Model, Knowledge Model, and Literate Model. `textus-bok` owns the BoK-side typed interpretation, normalization, projection, visualization, and review needed to realize those rules.

This contract complements `bok-domain-model.md` and the accepted Knowledge Map design. It does not replace the existing guarantees that semantic candidates are not factual knowledge and that Textus BoK remains the owner of BoK-specific meaning while SIE remains provider-neutral infrastructure.

## Governing Term semantics

A `BokTerm` represents the governing BoK's canonical concept identity and definition.

For SimpleModeling.org, the authoritative definition comes from the SimpleModeling.org Glossary. Textus BoK may normalize, index, explain, and expose the Term, but it MUST NOT silently replace that definition with an external standard, an RDF node, CML metadata, or a semantic-search result.

The core semantic path is:

```text
Governing BoK Term
       |
       | defines
       v
BoK-local semantic concept
```

A BoK-local semantic concept is the concept as understood inside the governing BoK.

## BoK-local RDF node

The Knowledge Model should be able to represent each canonical Term through a BoK-local RDF node or equivalent stable semantic identity.

The exact IRI-generation/versioning scheme remains upstream work, but Textus BoK MUST preserve the distinction between:

- Term identity;
- BoK-local semantic node identity;
- external RDF identities.

These may be related closely, but external identities must not become the canonical BoK identity by label matching or semantic similarity.

## External semantic mapping

External RDF/ontology nodes are mappings from the BoK-local concept.

```text
BokTerm
   |
   v
BoK-local RDF node
   |
   | explicit/evidenced mapping
   v
External RDF node
```

Mappings SHOULD preserve the intended relation strength and direction. Candidate mapping types include SKOS-style exact/close/broad/narrow/related semantics when applicable.

`owl:sameAs` or equivalent identity assertions MUST NOT be inferred from matching labels, embeddings, co-occurrence, `rdfCandidates`, or generic provider results.

Every factual external mapping admitted into a Knowledge Map or review result must have attributable evidence and an explicit source generation.

## Object Model binding

Textus BoK should admit references from Terms to CML/Object Model elements without taking ownership of the full Object Model.

Typical target kinds include:

- Entity
- Value
- Trait
- Powertype
- Aggregate
- View
- State / State Machine
- Operation / Event
- Relationship

The binding is not identity. The relation may express semantics such as representation, realization, classification, specialization, use, or another upstream-defined relation.

Conceptually:

```text
BokTerm
   |
   | typed model binding
   v
ModelElementReference
   |
   v
CML/Object Model element
```

Textus BoK should consume stable producer metadata rather than re-parse arbitrary CML source when a normalized model-metadata contract is available.

For Cozy-produced `cozy.cml.model-metadata.v1`, `termId` is the primary semantic hook. `glossaryPath` is a source/navigation hint and MUST NOT replace Term identity. `rdfCandidates` are candidate external references only and MUST NOT be promoted automatically to factual mappings.

## Literate Model connection

Articles, Scenarios, Examples, rationale, and other authored resources connect to Terms as Information references.

They may:

- explain a Term;
- illustrate it;
- motivate it;
- apply it;
- provide examples or scenarios;
- record design rationale or context.

They do not silently redefine the canonical Term.

A Term-centered projection should therefore be able to expose:

```text
Term
  +-- canonical definition
  +-- Literate Model resources
  +-- Object Model elements
  +-- BoK-local RDF relations
  +-- external mappings
  +-- evidence/provenance
```

## Knowledge Map integration

The existing Knowledge Map invariants remain in force:

- it is a Textus BoK projection, not a raw RDF browser;
- factual edges require explicit admitted source evidence;
- semantic candidates and co-occurrence do not become edges;
- selected-generation consistency is preserved;
- source owners retain their full domain models.

Cross-model nodes and bindings should extend this model without flattening all model families into one untyped graph.

The map may expose a bounded Term-centered or Model-centered projection, but it should preserve type and relation semantics so a user can distinguish:

- canonical concept identity;
- authored Information;
- formal model realization;
- local semantic relations;
- external mappings;
- evidence.

## Visualization

Human-facing visualization should be projection-based rather than raw-triple-first.

Expected views include:

- Information Structure View;
- Term-centered View;
- Object Model View;
- Topic/Context View;
- Evidence/Provenance View;
- Review overlays.

RDF details remain available for drill-down and machine processing.

## Review

Cross-model review can produce findings such as:

- CML/Object Model element with no canonical Term;
- Term whose expected model realization is missing;
- Article/Scenario using a model concept without an explicit Term link;
- model element renamed without synchronizing its Term binding;
- external semantic mapping that conflicts with the canonical Term definition;
- `rdfCandidates` promoted without evidence;
- important Term with weak Literate Model coverage;
- conflicting local/external concept mappings;
- Term definition change with unreviewed dependent model elements.

Findings should retain source-aware evidence and, where practical, point back to the canonical Git-managed source for correction.

## SIE boundary

SIE continues to own:

- provider-neutral document/assertion/evidence transport;
- RDF/vector provider integration;
- generic semantic retrieval;
- candidate scoring.

SIE does not own:

- canonical Term meaning;
- BoK-local concept identity;
- model-binding semantics;
- factual promotion of semantic candidates;
- SimpleModeling-specific external mapping interpretation.

## Cozy boundary

Cozy is a producer of CML/Object Model metadata and BoK publication metadata.

Textus BoK should treat Cozy-provided `termId`, model-element metadata, and factual source topology as producer evidence. It should not treat Cozy as the authority for canonical Term meaning.

## CNCF boundary

CNCF provides generic runtime/integration mechanisms such as resource access, reference transport, evidence/provenance plumbing, operation dispatch, authorization, persistence, and cross-component execution.

CNCF does not own `BokTerm` semantics or SimpleModeling-specific cross-model relation meanings.

## Change-management integration

Cross-model findings should be compatible with the generic `textus-change-management` lifecycle:

```text
Finding
 -> Guidance
 -> Proposed Patch
 -> Candidate Information/Model Projection
 -> Semantic Diff
 -> Candidate Review
 -> Delivery/Pull Request
 -> Human Review
 -> Merge
 -> Re-review
```

Textus BoK owns BoK-specific candidate construction, semantic diff, and review. `textus-change-management` owns only the generic proposal lifecycle.

## Open items

1. Canonical BoK-local RDF IRI scheme.
2. Stable ModelElement reference contract consumed from Cozy/CML metadata.
3. Normative Term-to-ModelElement relation vocabulary.
4. Machine-readable representation of Glossary viewpoints and external mappings.
5. Exact shape of Term-centered Knowledge Map and review overlays.
6. Synchronization rules when Term definitions, CML elements, or Literate Model resources change.
