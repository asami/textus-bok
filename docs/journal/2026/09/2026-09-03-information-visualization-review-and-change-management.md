# Information Visualization, Review, and Change Management

**Date:** 2026-09-03  
**Repository:** `textus-bok`  
**Status:** Discussion / design direction

## 1. Background

This journal records the design discussion around extending Textus BoK from retrieval and the existing Knowledge Map toward human-oriented Information visualization, knowledge-structure review, and a Git-based improvement loop.

The central premise is that the canonical BoK is a **GitHub-managed, text-based BoK site**. Textus BoK should not replace that canonical source or become an independent knowledge editor/database. Instead it should project the canonical source into semantic models that can be understood, visualized, reviewed, and improved through normal Git history and Pull Request workflows.

The desired high-level loop is:

```text
Git-managed BoK Site
        |
        | build / ingest
        v
Textus BoK Information Model
        |
        +--> Visualization
        |
        +--> Review
                |
                v
             Finding
                |
                v
          Proposed Change
                |
                v
          Candidate Model
                |
                v
          Semantic Diff
                |
                v
        Candidate Review
                |
                v
          Pull Request
                |
          Human Review
                |
              Merge
                |
        rebuild / ingest
                |
                v
            Re-review
```

## 2. Canonical source principle

The BoK site source is the source of truth.

Important properties of the canonical source are:

- text-based representation;
- Git history;
- branch-based changes;
- textual diff;
- Pull Request review;
- attribution of changes;
- recoverability and auditability;
- suitability for both human and AI-assisted editing.

Typical source forms include Markdown, YAML, JSON, and structured metadata generated or maintained as part of the BoK site project.

Textus BoK should therefore remain primarily an **analysis/projection surface**. It should not normally mutate the canonical main branch directly.

A core policy is:

> AI proposes changes; humans accept canonical knowledge changes through Git review.

## 3. RDF is not the human visualization model

RDF remains important as a machine-readable knowledge representation and evidence/semantic layer, but directly drawing an RDF graph is not considered sufficient human visualization.

A large RDF graph typically exposes too many nodes and predicates at the wrong semantic granularity. It may show connectivity without communicating the structure a human needs to understand or review the BoK.

The preferred architecture is:

```text
BoK Site Source
      |
      v
Information semantics / structured metadata
      |
      v
Information Model
      |
      +--> human-oriented visualization
      |
      +--> review
      |
      +--> RDF / semantic enrichment / evidence
```

RDF should be available for drill-down and evidence, but the primary visualization should be based on an Information Model with normalized human-oriented semantics.

The design should avoid making RDF reverse-engineering the only way to reconstruct Information structure when the canonical BoK source already expresses that structure directly.

## 4. Information Model

The Information Model should provide an abstraction layer between raw BoK/RDF data and human-oriented diagrams.

Tentative concepts include:

```text
Information Model
|
├─ Information
│  ├─ identity
│  ├─ information type
│  ├─ category / topic
│  └─ source location
│
├─ Classification
│
├─ Composition
│  ├─ constituent information
│  ├─ attachment
│  └─ source material
│
├─ Association
│  ├─ references
│  ├─ related-to
│  ├─ derived-from
│  ├─ applies-to
│  └─ component-reference
│
└─ Evidence / Provenance
```

The distinction between **composition/attachment** and **association** is particularly important.

For example, an interview, scanned source document, editorial note, or other raw material may be part of the composition of an Information entity, while a reference to another article, scenario, project, term, or external concept is an association.

Treating all of these as undifferentiated RDF edges would lose useful human semantics.

## 5. Information types

The visualization should be capable of representing the important information/resource categories exposed by a BoK site, including at least the categories already considered in the Knowledge Map work:

- Term;
- Article;
- Scenario;
- Project;
- Bibliography/reference;
- Tag/category/topic;
- RDF resource;
- ComponentReference;
- Evidence/source material.

These types should not imply that Textus BoK owns the complete domain model of every referenced resource. They are typed Information nodes sufficient for visualization, navigation, review, and source traceability.

## 6. Information Visualization

Visualization should help a human understand the BoK at an appropriate level of detail rather than rendering one monolithic graph.

### 6.1 Information Overview

The overview should answer questions such as:

- What kinds of Information exist in this BoK?
- How much Information exists in each category?
- What are the major topics/categories?
- Which Information is central?
- Which areas appear isolated or underdeveloped?

Example:

```text
BoK: Software Architecture

Information Types
  Article       32
  Scenario      18
  Term         126
  Project       12
  Bibliography  48
  Component     27
```

### 6.2 Information Structure View

This is the primary human-oriented graph.

It should emphasize normalized relations such as:

```text
Information
|
├─ Classification
│  ├─ Type
│  ├─ Category
│  └─ Topic
│
├─ Composition
│  ├─ contains
│  ├─ attachment
│  └─ source material
│
└─ Association
   ├─ references
   ├─ related-to
   ├─ derived-from
   ├─ applies-to
   └─ component-reference
```

Raw RDF predicates may be available as a lower-level overlay or detail view but should not dominate the default diagram.

### 6.3 Topic / Context View

A user should be able to select a topic or Information node and see a bounded semantic neighborhood at a useful level of detail.

Example:

```text
Topic: Component-Based Development

              CBD
               |
       +-------+--------+
       |       |        |
       v       v        v
    Article   Term    Scenario
       |       |        |
       +---- Component -+
                 |
                 v
          textus-cbd-support
```

### 6.4 Evidence View

Evidence and provenance should be drill-down information.

```text
Information
    |
    v
Relation / Claim
    |
    v
Evidence
  ├─ RDF triple
  ├─ source document
  ├─ article
  ├─ external authority
  └─ attributable URI/resource
```

This preserves the current Textus BoK principle that exact knowledge, semantic candidates, and attributable evidence are distinct concepts.

### 6.5 Level of detail

The Web visualization should support multiple levels of detail, conceptually such as:

```text
Overview
   ↓
Information Structure
   ↓
Detailed Relations
   ↓
Evidence / RDF / Provenance
```

The default level should optimize human comprehension. Detailed RDF/source material should be available by explicit drill-down.

## 7. Relationship with the BoK site

The generated/readable BoK site and Textus BoK visualization have different roles.

```text
                 BoK
                  |
       +----------+----------+
       |          |          |
      Read      Explore     Review
       |          |          |
   BoK Site   Visualization  Review
```

### BoK Site

Primary purpose:

- read Information;
- navigate articles/scenarios/terms;
- present authored content;
- remain tied to the canonical Git-managed source.

### Textus BoK Visualization

Primary purpose:

- understand Information relationships;
- inspect classification/composition/association;
- navigate semantic neighborhoods;
- inspect evidence and provenance;
- understand the structure of the BoK as a whole.

### Textus BoK Review

Primary purpose:

- evaluate Information structure;
- identify missing or weak knowledge areas;
- identify consistency problems;
- assess evidence quality;
- produce actionable Findings linked back to canonical source.

## 8. Stable identity and bidirectional navigation

Information identities should remain stable across the BoK site, Textus BoK Information Model, visualization, review findings, and source locations.

Conceptually:

```text
Article: article:cbd-ai-era
Scenario: scenario:order-design
Term: term:aggregate
Project: project:textus-cbd-support
```

This allows bidirectional navigation:

```text
BoK Site Information Page
        ⇅
Textus BoK Information Map focus
```

A BoK site page should eventually be able to offer a `View in Knowledge/Information Map` action.

Likewise, selecting an Information node in Textus BoK should allow the user to return to the canonical BoK page and, where authorized, the canonical Git source.

## 9. Review capability

Review should operate over the Information Model rather than treating RDF validity as equivalent to BoK quality.

Tentative review categories are:

```text
Knowledge Review
├─ Structure Review
├─ Coverage Review
├─ Consistency Review
├─ Evidence Review
└─ Information Quality Review
```

### 9.1 Structure Review

Possible findings include:

- isolated Information;
- excessive association density;
- cyclic composition;
- unusually large compositions;
- areas represented only by loose associations with no meaningful structure;
- unclassified Information;
- inappropriate relationship types.

A graph may be technically valid RDF and still be structurally poor as a BoK.

### 9.2 Coverage Review

Coverage review should identify missing knowledge dimensions.

Example:

```text
Topic
  ├─ Term       ✓
  ├─ Article    ✓
  ├─ Scenario   ✕
  ├─ Reference  ✓
  └─ Component  ✕
```

This can reveal biases such as a topic having extensive terminology and articles but no concrete scenarios, references, or implementation examples.

### 9.3 Consistency Review

Possible findings include:

- conflicting definitions;
- duplicated concepts under different Information identities;
- conflicting classifications;
- inconsistent relationship direction;
- dangling references;
- inconsistent ComponentReferences;
- mixing independent classification axes.

### 9.4 Evidence Review

Possible findings include:

- missing evidence;
- single-source dependence;
- stale evidence;
- provenance gaps;
- conflicting sources;
- semantic candidate treated incorrectly as factual knowledge.

The current distinction between exact knowledge and provider-backed semantic candidates should remain visible in review semantics.

## 10. Review findings on visualization

Review Findings should not be restricted to a separate findings table.

The Information Map should support a Review overlay so a Finding can be seen in its semantic context.

Example controls may include:

```text
Display
[x] Composition
[x] Association
[x] Classification
[ ] RDF relations
[ ] Evidence
[x] Review findings

Review
[x] Structure
[x] Coverage
[ ] Consistency
[ ] Evidence
```

Examples of diagram annotations include:

- an isolated node marked with a coverage warning;
- a relation with missing evidence;
- a topic with no Scenario coverage;
- a conflicting definition shown at the affected Term;
- an overly dense association region highlighted for structural review.

Thus:

- Visualization without Review explains the structure;
- Review without Visualization identifies issues;
- Visualization plus Review explains issues in their Information context.

## 11. Source-aware Findings

Every actionable Finding should carry enough source traceability to guide a correction in the canonical BoK repository.

Tentative shape:

```text
InformationReviewFinding
  subjectId
  findingType
  severity
  message
  evidence
  source:
    resource
    sourcePath
    sourceAnchor?
```

The source information should allow a user or AI assistant to locate the relevant Markdown/YAML/metadata source rather than editing Textus BoK's projection directly.

## 12. Git and Pull Request improvement loop

The preferred correction path is through Git Pull Requests.

```text
BoK Site Repository
      |
      | build / ingest
      v
Information Model
      |
      +--> Visualization
      |
      +--> Review
             |
             v
          Finding
             |
             v
       Proposed Change
             |
             v
       Candidate Source
             |
             v
    Candidate Information Model
             |
             v
        Semantic Diff
             |
             v
      Candidate Review
             |
             v
        Pull Request
             |
        Human Review
             |
           Merge
             |
             v
        rebuild / ingest
             |
             v
          Re-review
```

The AI-assisted system should normally produce a branch/patch and Pull Request rather than directly update canonical `main`.

## 13. Semantic diff

Git text diff remains essential because the canonical source is textual, but Textus BoK should additionally be able to explain the semantic impact of a proposed change.

Example:

```text
Knowledge / Information Diff

Added
  + 3 Terms
  + 1 Scenario
  + 7 Associations

Changed
  ~ DDD → Aggregate relation

Removed
  - obsolete ComponentReference

Quality impact
  orphan Terms:       4 → 1
  Scenario coverage: 62% → 78%
```

The semantic diff does not replace Git diff. It supplements Git diff with the meaning of the change at the Information Model level.

This creates two complementary review layers:

```text
Git Review
  text diff
  syntax
  schema
       |
       v
BoK Semantic Review
  Information structure
  classification
  composition
  association
  coverage
  evidence
  consistency
```

## 14. Candidate-before-PR principle

A proposed source patch should preferably be evaluated before a Pull Request is opened.

```text
Current BoK Source
       |
       v
Finding
       |
       v
Guidance
       |
       v
Proposed Source Patch
       |
       v
Candidate Build
       |
       v
Candidate Information Model
       |
       v
Semantic Diff
       |
       v
Candidate Review
       |
       v
Ready for Pull Request
```

This allows the system to detect that a proposed textual change makes the Information Model worse before asking a human to review the PR.

## 15. Shared change-management mechanism

The same improvement mechanism is also required by `textus-cbd-support`, whose canonical model source is primarily CML.

The shared pattern is:

```text
Canonical Source
      |
      v
Semantic Model
      |
      +--> Visualization
      |
      +--> Review
             |
             v
          Finding
             |
             v
          Guidance
             |
             v
       Proposed Patch
             |
             v
      Candidate Model
             |
             v
       Semantic Diff
             |
             v
     Candidate Review
             |
             v
       Pull Request
             |
       Human Review
             |
           Merge
             |
             v
        Re-verification
```

This mechanism should not be duplicated independently in Textus BoK and CBD Support.

## 16. `textus-change-management`

The discussion proposes a new helper Component tentatively named:

`textus-change-management`

Its responsibility is the generic semantic lifecycle of proposed changes, while domain-specific components retain their own semantics.

```text
textus-bok ----------------+
                           |
textus-cbd-support --------+--> textus-change-management --> CNCF
```

Tentative generic responsibilities include:

- Finding intake/reference;
- Guidance orchestration;
- Change Proposal lifecycle;
- proposed patch/artifact references;
- Candidate Build orchestration;
- Semantic Diff handling;
- Candidate Review;
- approval/readiness;
- external Delivery such as Pull Request;
- merge tracking;
- post-merge verification.

The core model should use a generic `Delivery` abstraction rather than make Pull Request a mandatory core type. GitHub PR is the first expected delivery provider.

Textus BoK remains responsible for:

- BoK-specific guidance;
- Markdown/YAML/metadata patch semantics;
- candidate Information Model construction;
- Knowledge/Information semantic diff;
- BoK-specific candidate review.

`textus-change-management` must not need to understand RDF, BokTerm, CML, or Component Design Model semantics.

## 17. CNCF responsibility boundary

The first implementation of the change loop should be developed in `textus-change-management`, not prematurely added to CNCF.

CNCF should remain the execution substrate and may eventually absorb primitives demonstrated to be genuinely generic through multiple consumers.

Potential future CNCF primitives include:

- resumable long-running workflow/job execution;
