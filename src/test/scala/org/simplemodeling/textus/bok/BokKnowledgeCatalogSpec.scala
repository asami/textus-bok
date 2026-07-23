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
 * @version Jul. 23, 2026
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
      val missingcomponent = catalog.getComponentReference("missing", None, None)

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
    sourceid: String
  ): ComponentReference =
    ComponentReference(
      Some(ComponentSourceId(sourceid)),
      Some(ComponentCatalogId(s"$kind/$name.yaml")),
      None,
      ComponentName(name),
      ComponentTitle(title),
      ComponentKind(kind),
      Some(ComponentVersion("0.2.0")),
      BokEvidence(BokEvidenceUri(s"urn:textus:bok:$sourceid:$name"), BokSourceId(sourceid), None, None, None)
    )
}
