package org.simplemodeling.textus.bok

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.resource.{
  InMemoryTextusUrnResourceProvider,
  ResourceAccessTestProfile
}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.BokSourceReader
import org.simplemodeling.textus.bok.value.BokKnowledgeSource

/*
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokSourceReaderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK source reader" should {
    "normalize metadata-only terms and component existence through CNCF resources" in {
      Given("an in-memory Textus URN source with a manifest, glossary, and canonical repository index")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index
      ))

      When("the logical source is normalized")
      val result = BokSourceReader.read(context, _source).TAKE

      Then("terms and CAR/SAR references are deterministic and carry metadata evidence")
      result.terms.map(_.termId.value) shouldBe Vector("architecture:component", "architecture:runtime")
      result.terms.map(_.definition.value) shouldBe Vector("Reusable unit.", "Runtime summary.")
      result.components.map(x => x.kind.value -> x.name.value) shouldBe Vector(
        "car" -> "textus-account",
        "sar" -> "textus-runtime"
      )
      result.components.map(_.version.map(_.value)) shouldBe Vector(Some("0.2.0"), Some("1.0.0-SNAPSHOT"))
      result.components.map(_.evidence.uri.value) shouldBe Vector(
        "urn:textus:bok:fixture/repository/catalog/car/textus-account.yaml",
        "urn:textus:bok:fixture/repository/catalog/sar/textus-runtime.yaml"
      )
      result.warnings shouldBe empty
    }

    "normalize current Cozy CAR and SAR reference indexes without CBD detail" in {
      Given("a current Cozy KnowledgeSource with separate CAR and SAR component-reference indexes")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _reference_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/metadata/cncf/component-references/car.json" -> _car_reference_index,
        "fixture/metadata/cncf/component-references/sar.json" -> _sar_reference_index
      ))

      When("the metadata-only source is normalized")
      val result = BokSourceReader.read(context, _source).TAKE

      Then("CAR and SAR existence retain current versions and public metadata evidence")
      result.components.map(x => (x.kind.value, x.name.value, x.version.map(_.value))) shouldBe Vector(
        ("car", "nict-knowledgehub", Some("0.1.0-smoke")),
        ("sar", "nict-knowledgehub", Some("0.1.0-smoke"))
      )
      result.components.map(_.evidence.uri.value) shouldBe Vector(
        "urn:textus:bok:fixture/repository/car/nict-knowledgehub/index.html",
        "urn:textus:bok:fixture/repository/sar/nict-knowledgehub/index.html"
      )
      result.warnings shouldBe empty
    }

    "reject incompatible current Cozy component reference contracts" in {
      Given("one unsupported index schema and one entry whose kind conflicts with its index")
      val unsupported = _car_reference_index.replace(
        "cncf.component-reference-index.v1",
        "cncf.component-reference-index.v2"
      )
      val mismatched = _car_reference_index.replace(
        "\"kind\": \"car\",\n    \"recommended\"",
        "\"kind\": \"sar\",\n    \"recommended\""
      )

      When("each malformed component-reference index is normalized")
      val unsupportedresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _reference_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/metadata/cncf/component-references/car.json" -> unsupported,
        "fixture/metadata/cncf/component-references/sar.json" -> _sar_reference_index
      )), _source)
      val mismatchedresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _reference_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/metadata/cncf/component-references/car.json" -> mismatched,
        "fixture/metadata/cncf/component-references/sar.json" -> _sar_reference_index
      )), _source)

      Then("neither incompatible contract is accepted")
      unsupportedresult should matchPattern { case Consequence.Failure(_) => }
      mismatchedresult should matchPattern { case Consequence.Failure(_) => }
    }

    "reject non-relative and unsupported source contracts before reading child content" in {
      Given("manifests with an absolute child reference and an unsupported schema")
      val absolute = _manifest.replace(
        "metadata/glossary/terms.json",
        "https://outside.example/terms.json"
      )
      val unsupported = _manifest.replace(
        "cncf.knowledge-source.v1",
        "cncf.knowledge-source.v2"
      )

      When("both logical sources are read through the resource DSL")
      val absoluteresult = BokSourceReader.read(
        _context(Map("fixture/metadata/cncf/knowledge-source.json" -> absolute)),
        _source
      )
      val unsupportedresult = BokSourceReader.read(
        _context(Map("fixture/metadata/cncf/knowledge-source.json" -> unsupported)),
        _source
      )

      Then("both violations remain structured resource failures")
      absoluteresult should matchPattern { case Consequence.Failure(_) => }
      unsupportedresult should matchPattern { case Consequence.Failure(_) => }
    }

    "operate without rendered HTML or host filesystem access" in {
      Given("only structured metadata resources in an in-memory CNCF provider")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index
      ))

      When("the source is normalized without any page resource")
      val result = BokSourceReader.read(context, _source).TAKE

      Then("metadata alone produces the complete normalization result")
      result.terms should have size 2
      result.components should have size 2
    }

    "accept recognized empty metadata as an explicit empty source" in {
      Given("an empty glossary and repository index")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/glossary/terms.json" -> """{"terms": []}""",
        "fixture/repository/catalog/index.json" -> _empty_repository_index
      ))

      When("the empty source is normalized")
      val result = BokSourceReader.read(context, _source).TAKE

      Then("the source succeeds with empty-result warnings")
      result.terms shouldBe empty
      result.components shouldBe empty
      result.warnings.map(_.value) should contain allOf (
        "KnowledgeSource glossary is empty",
        "KnowledgeSource component repository is empty"
      )
    }

    "reject duplicate term and component identities deterministically" in {
      Given("duplicate glossary terms and duplicate repository artifact identities")
      val duplicateterms = _terms.replace(
        "\"architecture:component\"",
        "\"architecture:runtime\""
      )
      val duplicatecomponents = _repository_index.replace(
        "\"kind\": \"sar\", \"artifactId\": \"textus-runtime\", \"catalog\": \"sar/textus-runtime.yaml\"",
        "\"kind\": \"car\", \"artifactId\": \"textus-account\", \"catalog\": \"car/textus-account.yaml\""
      )

      When("each malformed source is normalized")
      val termresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/glossary/terms.json" -> duplicateterms,
        "fixture/repository/catalog/index.json" -> _repository_index
      )), _source)
      val componentresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> duplicatecomponents
      )), _source)

      Then("neither conflicting dataset is accepted")
      termresult should matchPattern { case Consequence.Failure(_) => }
      componentresult should matchPattern { case Consequence.Failure(_) => }
    }
  }

  private def _context(contents: Map[String, String]): ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private val _source = BokKnowledgeSource(
    BokSourceId("simplemodeling"),
    BokDatasetId("simplemodeling-bok"),
    BokSourceGeneration("2026-07-21"),
    BokResourceReference("urn:textus:bok:fixture")
  )

  private val _manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-repository-index", "href": "repository/catalog/index.json"},
      |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
      |  ]
      |}""".stripMargin

  private val _reference_manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-reference-index", "href": "metadata/cncf/component-references/car.json"},
      |    {"kind": "component-reference-index", "href": "metadata/cncf/component-references/sar.json"},
      |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
      |  ]
      |}""".stripMargin

  private val _terms =
    """{
      |  "terms": [
      |    {
      |      "id": "architecture:runtime",
      |      "title": "Runtime",
      |      "category": "architecture",
      |      "definition_html": "<p>Runtime definition.</p>",
      |      "summary": "Runtime summary.",
      |      "term_type": "concept"
      |    },
      |    {
      |      "id": "architecture:component",
      |      "title": "Component",
      |      "category": "architecture",
      |      "definition_text": "Reusable unit.",
      |      "term_type": "concept"
      |    }
      |  ]
      |}""".stripMargin

  private val _repository_index =
    """{
      |  "schemaVersion": "cncf.component-repository-index.v1",
      |  "generatedAt": "2026-07-21T00:00:00Z",
      |  "artifacts": [
      |    {"kind": "sar", "artifactId": "textus-runtime", "catalog": "sar/textus-runtime.yaml", "status": "active", "latestSnapshot": "1.0.0-SNAPSHOT"},
      |    {"kind": "car", "artifactId": "textus-account", "catalog": "car/textus-account.yaml", "status": "active", "recommended": "0.2.0", "latestStable": "0.2.0"}
      |  ]
      |}""".stripMargin

  private val _empty_repository_index =
    """{
      |  "schemaVersion": "cncf.component-repository-index.v1",
      |  "generatedAt": "2026-07-21T00:00:00Z",
      |  "artifacts": []
      |}""".stripMargin

  private val _car_reference_index =
    """{
      |  "schemaVersion": "cncf.component-reference-index.v1",
      |  "kind": "car",
      |  "entries": [{
      |    "name": "nict-knowledgehub",
      |    "title": "nict-knowledgehub",
      |    "kind": "car",
      |    "recommended": "0.1.0-smoke",
      |    "latest_stable": "0.1.0-smoke",
      |    "latest_snapshot": null,
      |    "public_path": "repository/car/nict-knowledgehub/index.html"
      |  }]
      |}""".stripMargin

  private val _sar_reference_index =
    """{
      |  "schemaVersion": "cncf.component-reference-index.v1",
      |  "kind": "sar",
      |  "entries": [{
      |    "name": "nict-knowledgehub",
      |    "title": "nict-knowledgehub",
      |    "kind": "sar",
      |    "recommended": "0.1.0-smoke",
      |    "latest_stable": "0.1.0-smoke",
      |    "latest_snapshot": null,
      |    "public_path": "repository/sar/nict-knowledgehub/index.html"
      |  }]
      |}""".stripMargin
}
