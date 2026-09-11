# BoK Two-Source Acceptance Contract

Status: Phase 10.1 P10-04A authoritative runtime-acceptance specification

## Scope

This contract defines the bounded acceptance evidence for one private
documentation catalog made from the two current generated `website.d` sources.
It realizes neither source registration nor public catalog administration.
The existing Phase 7 direct-profile isolation and the Phase 10.1 catalog
selection contract remain authoritative.

## Frozen private composition

The private configuration contains exactly these direct bindings, in this
bootstrap order:

| Direct key | Source ID | Dataset ID | Generation | Evidence URI |
| --- | --- | --- | --- | --- |
| `development` | `simplemodeling-development` | `simplemodeling-bok-development` | `sha256:5cf33a2697035e3b53c6920446e9c9ce2c94a3c4192d56441a308d5f9b956b5f` | `https://www.simplemodeling.org/` |
| `project:knowledgehub` | `knowledgehub` | `knowledgehub-bok` | `sha256:ce991b30a90b6bb6c65f4b3593124e78d2e2ecd28d1cacbcd2258abb30e16608` | `https://www.asamioffice.com/kokubunji/knowledgehub/` |

Its only private aggregate is `catalogId = documentation`, with the two direct
members `development` and `project:knowledgehub`. The configuration explicitly
selects `textus.bok.knowledge-backend.backend: memory`, retains the exact two
file roots, and contains no `textus.sie` provider policy. A configured or
explicit `sie` backend remains the existing optional provider boundary and
fails closed when unavailable; it is not part of this assembly.

The acceptance SAR packages SIE, BoK, and Scraper as exactly three sibling CAR
components, in that order, plus one local user security subject. SIE supplies
the typed federation API artifact required by BoK's optional socket at CAR/SAR
assembly time. Scraper joins only as SIE's required sibling dependency: it is
not a BoK embedded implementation, catalog source, catalog member, selected
knowledge backend, MCP capability, or external source access path. No SIE or
Scraper implementation is embedded in the BoK CAR or selected as the BoK
knowledge backend. The harness starts SIE only with its existing in-memory
settings. It does not package a duplicate BoK, a BoK-specific server, or any
source, catalog, reload, backend-selection, or administration capability.

## Public-read and replacement boundary

`profile = catalog` and `catalogId = documentation` select both admitted
members as one source-qualified result set. A catalog response has no synthetic
primary source tuple. Its `selection.members` contains the complete direct
attribution for both sources in deterministic source-qualified order.

The complete BoK MCP projection is exactly this set of ten read tools. The
common endpoint can also publish framework-owned common tools; they are neither
BoK capabilities nor SIE/Scraper publication. SIE remains a sibling CAR/API
provider, but is non-MCP-published through `cncf.mcp.enabled: "false"`. That
setting does not disable SIE's typed component API or provider role:

- `org.simplemodeling.textus.Bok.BokRetrieval.searchTerms`
- `org.simplemodeling.textus.Bok.BokRetrieval.explainTerm`
- `org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences`
- `org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference`
- `org.simplemodeling.textus.Bok.BokRetrieval.getKnowledgeMap`
- `org.simplemodeling.textus.Bok.BokRetrieval.searchSemanticKnowledge`
- `org.simplemodeling.textus.Bok.BokRetrieval.getSemanticManifest`
- `org.simplemodeling.textus.Bok.BokRetrieval.getSemanticResource`
- `org.simplemodeling.textus.Bok.BokRetrieval.getSemanticSection`
- `org.simplemodeling.textus.Bok.BokRetrieval.discoverSemanticKnowledge`

`replaceKnowledgeSource` remains the protected REST-only operation boundary
and is never MCP-ready. The acceptance probe uses that existing replacement operation
only to replace the KnowledgeHub source, then proves that the SimpleModeling
source remains resident in the same selected catalog. No new transport,
operation, mutation tool, or request-side source location is introduced.

## Evidence procedure

`scripts/configure-bok-two-source-acceptance.sh` accepts two explicit absolute
source roots. It validates each real generated root, its KnowledgeSource
manifest, and every declared non-symlink resource; JSON resources must parse.
It derives generation evidence from the manifest plus all declared resource
bytes and atomically writes only a marked private configuration. Its only
permitted output is the exact project `.textus/config.yaml` default or a file
strictly below the real project `target/` tree; dot segments and symbolic-link
ancestors are refused before it creates a directory or writes a file. It
refuses to overwrite an unmarked configuration, except that the exact default
may migrate the established `textus-bok-managed-profile-config: v1` marker to
the current two-source marker. It never modifies either external source tree.

`scripts/test/check-bok-two-source-acceptance-sar.sh` takes the same explicit
roots, writes its runtime artifacts only below this repository's `target/`,
packages the bounded three-CAR sibling SAR, and starts one captured loopback
runtime with `--textus.config.file` and `--no-default-components`. Before
launch it verifies exactly the SIE-then-BoK-then-Scraper components and their
component/API descriptors, including both dependency edges: SIE supplies
BoK's optional `SemanticIntegrationFederation` API, and Scraper supplies
SIE's required `Scrapers` API. The BoK SPI remains optional
(`required: false`); the SIE Scrapers SPI remains required (`required: true`).

The loopback probe proves all of the following through existing transports:

- MCP publishes exactly the ten qualified BoK read tools above, no
  SIE-qualified or Scraper-qualified tool, and no replacement tool. The probe
  separately records the actual total endpoint tool count, which can include
  framework-owned common tools.
- MCP `explainTerm` resolves `literate-modeling:story` and
  `technology:embedding` through `catalog/documentation` with their exact
  record and member attribution.
- MCP `getKnowledgeMap` returns both source/dataset/generation tuples in one
  response.
- One protected KnowledgeHub replacement completes without evicting the
  SimpleModeling Story from the selected catalog.

The harness stops only its captured process and removes only its owned target
runtime directory. It does not use a working directory, request, host state,
network source, or an external source tree to derive a binding location.
