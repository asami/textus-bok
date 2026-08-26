package org.simplemodeling.textus.bok

import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.*
import org.simplemodeling.textus.bok.value.*

/*
 * Failing-first executable acceptance specification for DOC-07
 * (Phase 59.7 / Step P597-S2 / Slice P597-S2A).
 *
 * BoK returns existence-only coordinates for CBD handoff. The returned value
 * retains identity and evidence and does not select or derive CBD detail,
 * usage, compatibility, or review information.
 *
 * @since   Aug. 27, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokCbdComponentReferenceHandoffSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "DOC-07 BoK component-reference handoff" should {
    "return an exact existence-only reference with all handoff coordinates" in {
      Given("a selected BoK generation containing one qualified CAR existence reference")
      val fixture = _catalog_fixture

      When("BoK resolves the exact name, kind, organization, and version")
      val response = fixture.catalog.getComponentReference(
        fixture.selection,
        "example-textus-order",
        Some("1.2.0"),
        Some("car"),
        Some("org.example")
      )

      Then("the result preserves identity and evidence without adding CBD detail or usage")
      response.status.value shouldBe "matched"
      response.reference shouldBe Some(fixture.primaryReference)
      response.reference.map(_.name.value) shouldBe Some("example-textus-order")
      response.reference.map(_.kind.value) shouldBe Some("car")
      response.reference.flatMap(_.organization.map(_.value)) shouldBe Some("org.example")
      response.reference.flatMap(_.version.map(_.value)) shouldBe Some("1.2.0")
      response.reference.map(_.evidence.uri.value) shouldBe Some("urn:textus:bok:fixture/repository/catalog/car/org/example/example-textus-order.yaml")
      response.reference.map(_.title.value) shouldBe Some("example-textus-order")
    }

    "make ambiguous and missing existence coordinates explicit" in {
      Given("two qualified CAR references with the same name, kind, and version")
      val fixture = _catalog_fixture

      When("BoK resolves an unqualified identity and a version absent from the selected generation")
      val ambiguous = fixture.catalog.getComponentReference(
        fixture.selection,
        "example-textus-order",
        Some("1.2.0"),
        Some("car"),
        None
      )
      val missing = fixture.catalog.getComponentReference(
        fixture.selection,
        "example-textus-order",
        Some("9.9.9"),
        Some("car"),
        Some("org.example")
      )

      Then("BoK returns no selected reference for ambiguity or absence")
      ambiguous.status.value shouldBe "ambiguous"
      ambiguous.reference shouldBe empty
      ambiguous.warnings.map(_.value) should contain("Multiple BoK records satisfy the request; select an explicit identity.")
      missing.status.value shouldBe "no-match"
      missing.reference shouldBe empty
      missing.warnings shouldBe empty
    }
  }

  private final case class CatalogFixture(
    catalog: BokKnowledgeCatalog,
    selection: ResolvedBokProfile,
    primaryReference: ComponentReference
  )

  private def _catalog_fixture: CatalogFixture = {
    val source = BokKnowledgeSource(
      BokSourceId("source-a"),
      BokDatasetId("dataset-a"),
      BokSourceGeneration("g1"),
      BokResourceReference("urn:textus:bok:fixture")
    )
    val normalized = BokSourceReader.read(
      _context_for(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _source_manifest,
        "fixture/repository/catalog/index.json" -> _repository_index
      )),
      source
    ).TAKE
    val primary = normalized.components.find(_.organization.map(_.value).contains("org.example")).get
    val catalog = new BokKnowledgeCatalog()
    catalog.commit(BokFederationPublication("complete", Record.empty), normalized) shouldBe true
    CatalogFixture(
      catalog,
      ResolvedBokProfile(
        "official",
        None,
        source.datasetId,
        source.sourceId,
        source.generation,
        BokEvidence(
          BokEvidenceUri("urn:textus:bok:source-a:selection"),
          source.sourceId,
          None,
          None,
          None
        )
      ),
      primary
    )
  }

  private def _context_for(contents: Map[String, String]): ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private val _source_manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-repository-index", "href": "repository/catalog/index.json"}
      |  ]
      |}""".stripMargin

  private val _repository_index =
    """{
      |  "schemaVersion": "cncf.component-repository-index.v2",
      |  "generatedAt": "2026-08-27T00:00:00Z",
      |  "artifacts": [
      |    {
      |      "kind": "car",
      |      "namespace": "org.example",
      |      "id": "TextusOrder",
      |      "artifactId": "example-textus-order",
      |      "catalog": "car/org/example/example-textus-order.yaml",
      |      "status": "active",
      |      "recommended": "1.2.0"
      |    },
      |    {
      |      "kind": "car",
      |      "namespace": "org.other.example",
      |      "id": "TextusOrder",
      |      "artifactId": "example-textus-order",
      |      "catalog": "car/org/other/example/example-textus-order.yaml",
      |      "status": "active",
      |      "recommended": "1.2.0"
      |    }
      |  ]
      |}""".stripMargin
}
