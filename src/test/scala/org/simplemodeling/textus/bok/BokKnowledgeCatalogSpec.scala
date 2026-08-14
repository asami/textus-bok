package org.simplemodeling.textus.bok

import org.goldenport.Consequence
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.*
import org.simplemodeling.textus.bok.value.*

/*
 * @since   Jul. 21, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokKnowledgeCatalogSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK knowledge catalog" should {
    "selected terminology and component classification" which {
      "preserve exact, ambiguity, conflict, insufficient-evidence, and no-match term states" in {
        Given("one selected complete generation and isolated records from another dataset")
        val fixture = _classification_fixture()

        When("exact and explanatory terminology reads are classified")
        val exact = fixture.catalog.searchTerms(fixture.selection, "runtime:a", None, 10)(
          _ => Consequence.success(Map.empty)
        ).TAKE
        val conflict = fixture.catalog.explainTerm(fixture.selection, "Runtime")
        val ambiguity = fixture.catalog.explainTerm(fixture.selection, "Platform")
        val insufficient = fixture.catalog.explainTerm(fixture.selection, "Weak Term")
        val missing = fixture.catalog.explainTerm(fixture.selection, "Missing Term")

        Then("each classification remains explicit and retains the resolved selection")
        exact.status.value shouldBe "matched"
        exact.results.head.matchKind.value shouldBe "exact"
        conflict.status.value shouldBe "conflict"
        conflict.result.get.term.definition.value shouldBe "Runtime definition A."
        ambiguity.status.value shouldBe "ambiguous"
        insufficient.status.value shouldBe "insufficient-evidence"
        missing.status.value shouldBe "no-match"
        missing.result shouldBe empty
        exact.selection.resolvedProfile.value shouldBe "official"
        exact.selection.datasetId.value shouldBe "dataset-a"
        exact.selection.sourceId.value shouldBe "source-a"
        exact.selection.generation.value shouldBe "g1"
      }

      "constrain provider accepted keys before candidate classification and limits" in {
        Given("a selected generation with an identically named outside term")
        val fixture = _classification_fixture()

        When("the provider supplies selected and outside candidate scores for a one-result query")
        var accepted = Set.empty[BokCandidateKey]
        val candidate = fixture.catalog.searchTerms(fixture.selection, "execution environment", None, 1) { keys =>
          accepted = keys
          Consequence.success(Map(
            fixture.candidateid -> 0.82,
            fixture.outsidecandidateid -> 0.99
          ))
        }.TAKE

        Then("only keys from the resolved tuple are sent to the provider and classified")
        accepted should contain (fixture.candidateid)
        accepted should not contain fixture.outsidecandidateid
        accepted.forall(key => key.datasetId == "dataset-a" && key.sourceId == "source-a") shouldBe true
        candidate.status.value shouldBe "matched"
        candidate.results should have size 1
        candidate.results.head.matchKind.value shouldBe "candidate"
        candidate.results.head.score shouldBe 0.82
        candidate.results.map(_.term.evidence.sourceId.value) shouldBe Vector("source-a")
      }

      "classify component existence by exact identity, provider candidate, and no-match" in {
        Given("one selected generation with a CAR component reference")
        val fixture = _classification_fixture()

        When("exact, candidate, and missing component requests are made")
        val exact = fixture.catalog.searchComponentReferences(fixture.selection, "textus-account", Some("car"), 10)(
          _ => Consequence.success(Map.empty)
        ).TAKE
        val candidate = fixture.catalog.searchComponentReferences(fixture.selection, "account capability", None, 10)(
          _ => Consequence.success(Map(fixture.componentcandidateid -> 0.71))
        ).TAKE
        val missing = fixture.catalog.getComponentReference(fixture.selection, "missing", None, None, None)

        Then("only existence records receive stable exact, candidate, and no-match classifications")
        exact.status.value shouldBe "matched"
        exact.results.head.matchKind.value shouldBe "exact"
        candidate.results.head.matchKind.value shouldBe "candidate"
        missing.status.value shouldBe "no-match"
        missing.reference shouldBe empty
      }
    }

    "qualified component identity" which {
      "require organization for an ambiguous component identity and retain deterministic qualified ordering" in {
      Given("two qualified CAR references with the same kind, name, and version")
      val catalog = new BokKnowledgeCatalog()
      val comcomponent = _component("textus-account", "Account Component", "car", "source-com", Some("com.simplemodeling.textus"))
      val orgcomponent = _component("textus-account", "Account Component", "car", "source-org", Some("org.simplemodeling.textus"))
      val topology = BokKnowledgeTopology(
        Vector(BokKnowledgeNode(
          "component:account",
          "Account",
          "component-reference",
          _evidence("component-account"),
          componentReference = Some(BokKnowledgeComponentReference("car", "textus-account"))
        )),
        Vector.empty,
        false
      )
      _commit(catalog, _normalized(
        "source-qualified",
        "dataset-qualified",
        "g1",
        Vector.empty,
        Vector(orgcomponent, comcomponent),
        topology
      ))
      val selection = _resolved("official", None, "dataset-qualified", "source-qualified", "g1")

      When("the name is looked up with omitted, exact, and mismatched organizations")
      val omitted = catalog.getComponentReference(selection, "textus-account", Some("0.2.0"), Some("car"), None)
      val com = catalog.getComponentReference(selection, "textus-account", Some("0.2.0"), Some("car"), Some("com.simplemodeling.textus"))
      val org = catalog.getComponentReference(selection, "textus-account", Some("0.2.0"), Some("car"), Some("org.simplemodeling.textus"))
      val mismatch = catalog.getComponentReference(selection, "textus-account", Some("0.2.0"), Some("car"), Some("net.example"))
      val exact = catalog.searchComponentReferences(selection, "textus-account", Some("car"), 10)(_ => Consequence.success(Map.empty)).TAKE
      val candidatekeys = Set(
        BokCandidateKey("dataset-qualified", "source-qualified", BokFederationPublisher.componentDocumentId(comcomponent)),
        BokCandidateKey("dataset-qualified", "source-qualified", BokFederationPublisher.componentDocumentId(orgcomponent))
      )
      val candidates = catalog.searchComponentReferences(selection, "account capability", Some("car"), 10)(_ =>
        Consequence.success(candidatekeys.map(_ -> 0.71).toMap)
      ).TAKE
      val map = catalog.getKnowledgeMap(selection, None, None, None, Some(10), Some(10))

      Then("omitted organization is ambiguous, exact organization narrows, and all orderings are stable")
      omitted.status.value shouldBe "ambiguous"
      omitted.reference shouldBe empty
      com.status.value shouldBe "matched"
      com.reference.map(_.organization.map(_.value)) shouldBe Some(Some("com.simplemodeling.textus"))
      org.status.value shouldBe "matched"
      org.reference.map(_.organization.map(_.value)) shouldBe Some(Some("org.simplemodeling.textus"))
      mismatch.status.value shouldBe "no-match"
      mismatch.reference shouldBe empty
      exact.results.map(_.reference.organization.map(_.value)) shouldBe Vector(
        Some("com.simplemodeling.textus"),
        Some("org.simplemodeling.textus")
      )
      candidates.results.map(_.reference.organization.map(_.value)) shouldBe Vector(
        Some("com.simplemodeling.textus"),
        Some("org.simplemodeling.textus")
      )
      BokFederationPublisher.componentDocumentId(comcomponent) should not be BokFederationPublisher.componentDocumentId(orgcomponent)
      map.nodes.head.componentReferences.flatMap(_.organization.map(_.value)) shouldBe Vector(
        "com.simplemodeling.textus",
        "org.simplemodeling.textus"
      )
      }
    }

    "selected map projection" which {
      "project a bounded selected topology with deterministic truncation" in {
        Given("a complete selected generation with term and adjacent article topology")
        val fixture = _map_fixture()

        When("the term type, category, and focus select one seed with a one-node bound")
        val bounded = fixture.catalog.getKnowledgeMap(
          fixture.selection,
          Some("architecture"),
          Some("concept"),
          Some("runtime"),
          Some(1),
          Some(1)
        )

        Then("the adjacent factual closure is bounded deterministically and reports truncation")
        bounded.status.value shouldBe "matched"
        bounded.sources.map(_.generation.value) shouldBe Vector("g1")
        bounded.sources.head.sourceReference.map(_.kind.value) shouldBe Some("bok-site")
        bounded.sources.head.sourceReference.map(_.value.value) shouldBe Some("knowledgehub")
        bounded.sources.head.sourceReference.flatMap(_.uri.map(_.value)) shouldBe Some("https://example.test/knowledgehub")
        bounded.nodes.map(_.nodeId.value) shouldBe Vector("term:runtime")
        bounded.nodes.head.terms.map(_.definition.value) shouldBe Vector("Runtime definition.")
        bounded.nodes.head.componentReferences shouldBe empty
        bounded.relationships shouldBe empty
        bounded.truncated shouldBe true
        bounded.warnings.map(_.value) should contain ("Knowledge Map result is truncated")
      }

      "project complete selected topology with resolved-generation attribution" in {
        Given("a complete selected generation with term and component topology")
        val fixture = _map_fixture()

        When("a complete focus projection and all-node projection are requested")
        val complete = fixture.catalog.getKnowledgeMap(
          fixture.selection,
          Some("architecture"),
          Some("concept"),
          Some("runtime"),
          Some(10),
          Some(10)
        )
        val allnodes = fixture.catalog.getKnowledgeMap(
          fixture.selection,
          None,
          None,
          None,
          Some(10),
          Some(10)
        )

        Then("only the resolved generation is projected with its selection attribution")
        complete.nodes.map(_.nodeId.value) shouldBe Vector("term:runtime", "article:runtime")
        allnodes.nodes.find(_.nodeId.value == "component:account").map(_.componentReferences.map(x => x.kind.value -> x.name.value)) shouldBe
          Some(Vector("car" -> "textus-account"))
        complete.relationships.map(_.predicate.value) shouldBe Vector("references")
        complete.truncated shouldBe false
        complete.selection.generation.value shouldBe "g1"
        complete.selection.evidence.uri.value shouldBe "urn:textus:bok:source-a:selection"
      }

      "report no-match for a focus absent from the resolved generation" in {
        Given("a complete selected topology")
        val fixture = _map_fixture()

        When("a focus missing from that generation is requested")
        val missing = fixture.catalog.getKnowledgeMap(fixture.selection, None, None, Some("missing"), None, None)

        Then("the read remains an empty no-match without fallback")
        missing.status.value shouldBe "no-match"
        missing.nodes shouldBe empty
        missing.relationships shouldBe empty
      }

      "normalize map limits before projecting the selected generation" in {
        Given("a complete selected topology")
        val fixture = _map_fixture()

        When("node and relationship limits exceed their valid bounds")
        val clamped = fixture.catalog.getKnowledgeMap(fixture.selection, None, None, None, Some(999), Some(-1))

        Then("effective limits and their warnings are explicit")
        clamped.nodeLimit shouldBe BokKnowledgeCatalog.DEFAULT_KNOWLEDGE_MAP_NODE_LIMIT
        clamped.relationshipLimit shouldBe 1
        clamped.warnings.map(_.value) should contain ("Knowledge Map node limit 999 is clamped to 128")
        clamped.warnings.map(_.value) should contain ("Knowledge Map relationship limit -1 is clamped to 1")
      }
    }

    "generation replacement" which {
      "replace complete generations, remove stale records, and retain the previous generation after degradation" in {
      Given("one committed source generation")
      val catalog = new BokKnowledgeCatalog()
      val stale = _term("stale", "Stale Term", "Old definition.", "source-a", "dataset-a")
      val current = _term("current", "Current Term", "Current definition.", "source-a", "dataset-a")
      _commit(catalog, _normalized(
        "source-a",
        "dataset-a",
        "g1",
        Vector(stale),
        Vector.empty,
        _topology("stale-node")
      ))

      When("a complete replacement is followed by a degraded replacement")
      _commit(catalog, _normalized(
        "source-a",
        "dataset-a",
        "g2",
        Vector(current),
        Vector.empty,
        _topology("current-node")
      ))
      val degraded = BokFederationPublication("degraded", org.goldenport.record.Record.empty)
      catalog.commit(
        degraded,
        _normalized("source-a", "dataset-a", "g3", Vector(stale), Vector.empty, _topology("degraded-node"))
      ) shouldBe false
      val selection = _resolved("official", None, "dataset-a", "source-a", "g2")

      Then("the stale first generation is absent and the last complete terms and topology remain visible")
      catalog.explainTerm(selection, "Stale Term").status.value shouldBe "no-match"
      catalog.explainTerm(selection, "Current Term").status.value shouldBe "matched"
      catalog.selectedTopology("dataset-a").map(_.nodes.map(_.id)) shouldBe Some(Vector("current-node"))
      }
    }
  }

  private def _classification_fixture(): ClassificationFixture = {
    val catalog = new BokKnowledgeCatalog()
    val runtimea = _term("runtime:a", "Runtime", "Runtime definition A.", "source-a", "dataset-a")
    val runtimeb = _term("runtime:b", "Runtime", "Runtime definition B.", "source-a", "dataset-a")
    val platforma = _term("platform:a", "Platform", "Shared platform definition.", "source-a", "dataset-a")
    val platformb = _term("platform:b", "Platform", "Shared platform definition.", "source-a", "dataset-a")
    val weak = _term("weak", "Weak Term", "  ", "source-a", "dataset-a")
    val outside = _term("runtime:a", "Outside Runtime", "Outside definition.", "source-b", "dataset-b")
    val component = _component("textus-account", "Account Component", "car", "source-a")
    _commit(catalog, _normalized(
      "source-a",
      "dataset-a",
      "g1",
      Vector(runtimea, runtimeb, platforma, platformb, weak),
      Vector(component)
    ))
    _commit(catalog, _normalized("source-b", "dataset-b", "g1", Vector(outside), Vector.empty))
    ClassificationFixture(
      catalog,
      _resolved("official", None, "dataset-a", "source-a", "g1"),
      BokCandidateKey("dataset-a", "source-a", BokFederationPublisher.termDocumentId(runtimea)),
      BokCandidateKey("dataset-b", "source-b", BokFederationPublisher.termDocumentId(outside)),
      BokCandidateKey("dataset-a", "source-a", BokFederationPublisher.componentDocumentId(component))
    )
  }

  private def _map_fixture(): MapFixture = {
    val catalog = new BokKnowledgeCatalog()
    val runtime = _term("architecture:runtime", "Runtime", "Runtime definition.", "source-a", "dataset-a")
    val component = _component("textus-account", "Account Component", "car", "source-a")
    val topology = BokKnowledgeTopology(
      Vector(
        BokKnowledgeNode("term:runtime", "Runtime", "term", _evidence("term-runtime"), Some("architecture"), Vector("architecture:runtime"), Vector("core")),
        BokKnowledgeNode("article:runtime", "Runtime Article", "article", _evidence("article-runtime"), Some("documentation")),
        BokKnowledgeNode("component:account", "Account", "component-reference", _evidence("component-account"), componentReference = Some(BokKnowledgeComponentReference("car", "textus-account"))),
        BokKnowledgeNode("rdf:runtime", "Runtime RDF", "rdf", _evidence("rdf-runtime"))
      ),
      Vector(BokKnowledgeRelationship(
        "term:runtime",
        "references",
        "article:runtime",
        Some("References"),
        _evidence("runtime-reference"),
        Some("architecture"),
        Vector("architecture:runtime"),
        Vector("core")
      )),
      false,
      Some(BokKnowledgeSourceReference("bok-site", "knowledgehub", Some("https://example.test/knowledgehub")))
    )
    _commit(catalog, _normalized("source-a", "dataset-a", "g1", Vector(runtime), Vector(component), topology))
    MapFixture(catalog, _resolved("official", None, "dataset-a", "source-a", "g1"))
  }

  private final case class ClassificationFixture(
    catalog: BokKnowledgeCatalog,
    selection: ResolvedBokProfile,
    candidateid: BokCandidateKey,
    outsidecandidateid: BokCandidateKey,
    componentcandidateid: BokCandidateKey
  )

  private final case class MapFixture(
    catalog: BokKnowledgeCatalog,
    selection: ResolvedBokProfile
  )

  private def _commit(catalog: BokKnowledgeCatalog, normalized: NormalizedBokSource): Unit =
    catalog.commit(
      BokFederationPublication("complete", org.goldenport.record.Record.empty),
      normalized
    ) shouldBe true

  private def _resolved(
    profile: String,
    projectid: Option[String],
    datasetid: String,
    sourceid: String,
    generation: String
  ): ResolvedBokProfile =
    ResolvedBokProfile(
      profile,
      projectid,
      BokDatasetId(datasetid),
      BokSourceId(sourceid),
      BokSourceGeneration(generation),
      BokEvidence(BokEvidenceUri(s"urn:textus:bok:$sourceid:selection"), BokSourceId(sourceid), None, None, None)
    )

  private def _normalized(
    sourceid: String,
    datasetid: String,
    generation: String,
    terms: Vector[BokTerm],
    components: Vector[ComponentReference],
    topology: BokKnowledgeTopology = BokKnowledgeTopology.empty
  ): NormalizedBokSource =
    NormalizedBokSource(
      BokKnowledgeSource(
        BokSourceId(sourceid),
        BokDatasetId(datasetid),
        BokSourceGeneration(generation),
        BokResourceReference(s"urn:textus:bok:$sourceid")
      ),
      terms,
      components,
      Vector.empty,
      topology
    )

  private def _topology(id: String): BokKnowledgeTopology =
    BokKnowledgeTopology(
      Vector(BokKnowledgeNode(id, id, "term", _evidence(id))),
      Vector.empty,
      false,
      Some(BokKnowledgeSourceReference("bok-site", "knowledgehub", None))
    )

  private def _evidence(id: String): BokEvidence =
    BokEvidence(BokEvidenceUri(s"urn:textus:bok:source-a:$id"), BokSourceId("source-a"), None, None, None)

  private def _term(
    id: String,
    title: String,
    definition: String,
    sourceid: String,
    datasetid: String
  ): BokTerm =
    BokTerm(
      BokTermId(id),
      BokTermTitle(title),
      BokTermDefinition(definition),
      Some(BokTermCategory("architecture")),
      BokTermType("concept"),
      BokDatasetId(datasetid),
      BokEvidence(BokEvidenceUri(s"urn:textus:bok:$sourceid:$id"), BokSourceId(sourceid), None, None, None)
    )

  private def _component(
    name: String,
    title: String,
    kind: String,
    sourceid: String,
    organization: Option[String] = None
  ): ComponentReference =
    ComponentReference(
      Some(ComponentSourceId(sourceid)),
      Some(ComponentCatalogId(s"$kind/$name.yaml")),
      organization.map(ComponentOrganization.apply),
      ComponentName(name),
      ComponentTitle(title),
      ComponentKind(kind),
      Some(ComponentVersion("0.2.0")),
      BokEvidence(BokEvidenceUri(s"urn:textus:bok:$sourceid:$name"), BokSourceId(sourceid), None, None, None)
    )
}
