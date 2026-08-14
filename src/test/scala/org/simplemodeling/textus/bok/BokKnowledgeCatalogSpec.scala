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
 * @version Aug. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokKnowledgeCatalogSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK knowledge catalog" should {
    "preserve exact, candidate, ambiguity, conflict, insufficient-evidence, and no-match states" in {
      Given("multiple source-owned datasets containing reliable, equivalent, conflicting, and weak terms")
      val catalog = new BokKnowledgeCatalog()
      val runtimea = _term("runtime:a", "Runtime", "Runtime definition A.", "source-a", "dataset-a")
      val runtimeb = _term("runtime:b", "Runtime", "Runtime definition B.", "source-b", "dataset-b")
      val platforma = _term("platform:a", "Platform", "Shared platform definition.", "source-a", "dataset-a")
      val platformb = _term("platform:b", "Platform", "Shared platform definition.", "source-b", "dataset-b")
      val weak = _term("weak", "Weak Term", "  ", "source-c", "dataset-c")
      val component = _component("textus-account", "Account Component", "car", "source-a")
      _commit(catalog, _normalized("source-a", "dataset-a", "g1", Vector(runtimea, platforma), Vector(component)))
      _commit(catalog, _normalized("source-b", "dataset-b", "g1", Vector(runtimeb, platformb), Vector.empty))
      _commit(catalog, _normalized("source-c", "dataset-c", "g1", Vector(weak), Vector.empty))

      When("the catalog classifies exact identities, provider candidates, and unreliable grounding")
      val exact = catalog.searchTerms("runtime:a", None, 10)(_ => Consequence.success(Map.empty)).TAKE
      val candidateid = BokCandidateKey(
        "dataset-a",
        "source-a",
        BokFederationPublisher.termDocumentId(runtimea)
      )
      val candidate = catalog.searchTerms("execution environment", None, 10)(
        _ => Consequence.success(Map(candidateid -> 0.82))
      ).TAKE
      val conflict = catalog.explainTerm("Runtime")
      val ambiguity = catalog.explainTerm("Platform")
      val insufficient = catalog.explainTerm("Weak Term")
      val missing = catalog.explainTerm("Missing Term")

      Then("each state remains explicit and semantic evidence is not promoted to exact knowledge")
      exact.status.value shouldBe "matched"
      exact.results.head.matchKind.value shouldBe "exact"
      candidate.status.value shouldBe "matched"
      candidate.results.head.matchKind.value shouldBe "candidate"
      candidate.results.head.score shouldBe 0.82
      conflict.status.value shouldBe "conflict"
      conflict.result.get.term.definition.value shouldBe "Runtime definition A."
      ambiguity.status.value shouldBe "ambiguous"
      insufficient.status.value shouldBe "insufficient-evidence"
      missing.status.value shouldBe "no-match"
      missing.result shouldBe empty

      When("component existence is searched by exact identity, provider score, and an unknown identity")
      val exactcomponent = catalog.searchComponentReferences("textus-account", Some("car"), 10)(
        _ => Consequence.success(Map.empty)
      ).TAKE
      val candidatecomponent = catalog.searchComponentReferences("account capability", None, 10)(
        _ => Consequence.success(Map(
          BokCandidateKey(
            "dataset-a",
            "source-a",
            BokFederationPublisher.componentDocumentId(component)
          ) -> 0.71
        ))
      ).TAKE
      val missingcomponent = catalog.getComponentReference("missing", None, None, None)

      Then("CAR/SAR matching returns only existence records with stable match kinds")
      exactcomponent.status.value shouldBe "matched"
      exactcomponent.results.head.matchKind.value shouldBe "exact"
      candidatecomponent.results.head.matchKind.value shouldBe "candidate"
      missingcomponent.status.value shouldBe "no-match"
      missingcomponent.reference shouldBe empty

      When("another source publishes the same term identity but receives no provider score")
      val duplicateidentity = _term("runtime:a", "Other Runtime", "Other definition.", "source-d", "dataset-d")
      _commit(catalog, _normalized("source-d", "dataset-d", "g1", Vector(duplicateidentity), Vector.empty))
      val isolated = catalog.searchTerms("execution environment", None, 10)(
        _ => Consequence.success(Map(candidateid -> 0.82))
      ).TAKE

      Then("candidate attribution remains scoped by dataset and source identity")
      isolated.results.map(_.term.evidence.sourceId.value) shouldBe Vector("source-a")
    }

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

      When("the name is looked up with omitted, exact, and mismatched organizations")
      val omitted = catalog.getComponentReference("textus-account", Some("0.2.0"), Some("car"), None)
      val com = catalog.getComponentReference("textus-account", Some("0.2.0"), Some("car"), Some("com.simplemodeling.textus"))
      val org = catalog.getComponentReference("textus-account", Some("0.2.0"), Some("car"), Some("org.simplemodeling.textus"))
      val mismatch = catalog.getComponentReference("textus-account", Some("0.2.0"), Some("car"), Some("net.example"))
      val exact = catalog.searchComponentReferences("textus-account", Some("car"), 10)(_ => Consequence.success(Map.empty)).TAKE
      val candidatekeys = Set(
        BokCandidateKey("dataset-qualified", "source-qualified", BokFederationPublisher.componentDocumentId(comcomponent)),
        BokCandidateKey("dataset-qualified", "source-qualified", BokFederationPublisher.componentDocumentId(orgcomponent))
      )
      val candidates = catalog.searchComponentReferences("account capability", Some("car"), 10)(_ =>
        Consequence.success(candidatekeys.map(_ -> 0.71).toMap)
      ).TAKE
      val map = catalog.getKnowledgeMap(Some("dataset-qualified"), Some("source-qualified"), None, None, None, Some(10), Some(10))

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

    "project selected factual topology with deterministic filters and bounded results" in {
      Given("a complete selected generation with term and adjacent article topology")
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

      When("the term type, category, and focus select one seed with a one-node bound")
      val bounded = catalog.getKnowledgeMap(
        Some("dataset-a"),
        Some("source-a"),
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

      When("the same selected topology is requested without a constraining node bound")
      val complete = catalog.getKnowledgeMap(
        Some("dataset-a"),
        Some("source-a"),
        Some("architecture"),
        Some("concept"),
        Some("runtime"),
        Some(10),
        Some(10)
      )
      val allnodes = catalog.getKnowledgeMap(
        Some("dataset-a"),
        Some("source-a"),
        None,
        None,
        None,
        Some(10),
        Some(10)
      )
      val missing = catalog.getKnowledgeMap(None, None, None, None, Some("missing"), None, None)
      val clamped = catalog.getKnowledgeMap(None, None, None, None, None, Some(999), Some(-1))

      Then("the complete closure, no-match response, and effective limits remain explicit")
      complete.nodes.map(_.nodeId.value) shouldBe Vector("term:runtime", "article:runtime")
      allnodes.nodes.find(_.nodeId.value == "component:account").map(_.componentReferences.map(x => x.kind.value -> x.name.value)) shouldBe
        Some(Vector("car" -> "textus-account"))
      complete.relationships.map(_.predicate.value) shouldBe Vector("references")
      complete.truncated shouldBe false
      missing.status.value shouldBe "no-match"
      missing.nodes shouldBe empty
      missing.relationships shouldBe empty
      clamped.nodeLimit shouldBe BokKnowledgeCatalog.DEFAULT_KNOWLEDGE_MAP_NODE_LIMIT
      clamped.relationshipLimit shouldBe 1
      clamped.warnings.map(_.value) should contain ("Knowledge Map node limit 999 is clamped to 128")
      clamped.warnings.map(_.value) should contain ("Knowledge Map relationship limit -1 is clamped to 1")
    }

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

      Then("the stale first generation is absent and the last complete terms and topology remain visible")
      catalog.explainTerm("Stale Term").status.value shouldBe "no-match"
      catalog.explainTerm("Current Term").status.value shouldBe "matched"
      catalog.selectedTopology("dataset-a").map(_.nodes.map(_.id)) shouldBe Some(Vector("current-node"))
    }
  }

  private def _commit(catalog: BokKnowledgeCatalog, normalized: NormalizedBokSource): Unit =
    catalog.commit(
      BokFederationPublication("complete", org.goldenport.record.Record.empty),
      normalized
    ) shouldBe true

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
