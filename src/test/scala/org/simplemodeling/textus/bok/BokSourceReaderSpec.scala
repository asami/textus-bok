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
import org.simplemodeling.textus.bok.runtime.{BokKnowledgeComponentReference, BokSourceReader}
import org.simplemodeling.textus.bok.value.BokKnowledgeSource

/*
 * @since   Jul. 21, 2026
 *  version Jul. 24, 2026
 * @version Aug. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokSourceReaderSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK source reader" should {
    "normalize valid metadata and topology" which {
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
      result.components.map(_.organization.map(_.value)) shouldBe Vector(
        Some("org.simplemodeling.textus"),
        None
      )
      result.components.map(_.version.map(_.value)) shouldBe Vector(Some("0.2.0"), Some("1.0.0-SNAPSHOT"))
      result.components.map(_.evidence.uri.value) shouldBe Vector(
        "urn:textus:bok:fixture/repository/catalog/car/org/simplemodeling/textus/textus-account.yaml",
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

    "normalize a versioned Cozy graph summary with attributable metadata and truncation" in {
      Given("a metadata-only source with one versioned graph-summary resource")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> _topology_graph
      ))

      When("the source reader admits the structured graph summary")
      val result = BokSourceReader.read(context, _source).TAKE

      Then("topology ordering, source attribution, retained metadata, and truncation remain explicit")
      result.topology.sourceRef.map(x => (x.kind, x.value, x.uri)) shouldBe Some(
        ("bok-site", "knowledgehub", Some("https://example.test/knowledgehub"))
      )
      result.topology.nodes.map(_.id) shouldBe Vector("article:runtime", "term:runtime")
      result.topology.nodes.find(_.id == "term:runtime").map(x => (x.category, x.terms, x.tags)) shouldBe Some(
        (Some("architecture"), Vector("architecture:runtime"), Vector("core", "runtime"))
      )
      result.topology.relationships.map(x => (x.subjectId, x.predicate, x.objectId, x.terms, x.tags)) shouldBe Vector(
        ("term:runtime", "references", "article:runtime", Vector("architecture:runtime"), Vector("source"))
      )
      result.warnings.map(_.value) should contain("BoK graph summary is truncated")
    }

    "retain an explicit Cozy componentRef only when it exactly matches the selected component index" in {
      Given("a component-reference graph node with the current Cozy componentRef contract")
      val graph = _topology_graph.replace(
        """    {"id": "article:runtime", "label": "Runtime Article", "node_type": "article", "tags": ["doc"]}""",
        """    {"id": "component:account", "label": "Account", "node_type": "component-reference", "componentRef": {"kind": "car", "name": "textus-account", "version": "0.2.0"}, "tags": ["doc"]}"""
      ).replace("article:runtime", "component:account")
      val missing = graph.replace("textus-account", "missing-account")
      val inferred = graph.replace("\"componentRef\": {\"kind\": \"car\", \"name\": \"textus-account\", \"version\": \"0.2.0\"}", "\"componentRef\": {\"kind\": \"car\"}")
      val matchedorganization = graph.replace("\"version\": \"0.2.0\"", "\"organization\": \"org.simplemodeling.textus\", \"version\": \"0.2.0\"")
      val mismatchedversion = graph.replace("\"version\": \"0.2.0\"", "\"version\": \"0.3.0\"")
      val mismatchedorganization = graph.replace("\"version\": \"0.2.0\"", "\"organization\": \"org.textus\"")
      val malformedoptional = graph.replace("\"version\": \"0.2.0\"", "\"version\": \"\"")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> graph
      ))

      When("the graph is normalized without label or identifier inference")
      val result = BokSourceReader.read(context, _source).TAKE
      val missingresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> missing
      )), _source)
      val inferredresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> inferred
      )), _source)
      val matchedorganizationresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> matchedorganization
      )), _source)
      val mismatchedversionresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> mismatchedversion
      )), _source)
      val mismatchedorganizationresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> mismatchedorganization
      )), _source)
      val malformedoptionalresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/rdf/graph.json" -> malformedoptional
      )), _source)

      Then("only the declared CAR identity is retained and optional component identity fields match exactly when declared")
      result.topology.nodes.find(_.id == "component:account").flatMap(_.componentReference) shouldBe
        Some(BokKnowledgeComponentReference("car", "textus-account", None, Some("0.2.0")))
      matchedorganizationresult.TAKE.topology.nodes.find(_.id == "component:account").flatMap(_.componentReference) shouldBe
        Some(BokKnowledgeComponentReference("car", "textus-account", Some("org.simplemodeling.textus"), Some("0.2.0")))
      missingresult should matchPattern { case Consequence.Failure(_) => }
      inferredresult should matchPattern { case Consequence.Failure(_) => }
      mismatchedversionresult should matchPattern { case Consequence.Failure(_) => }
      mismatchedorganizationresult should matchPattern { case Consequence.Failure(_) => }
      malformedoptionalresult should matchPattern { case Consequence.Failure(_) => }
    }

    }

    "reject invalid metadata and topology" which {

    "reject malformed, incompatible, dangling, conflicting, and every finite graph-summary limit without reading referenced pages" in {
      Given("one admitted graph resource and variants that violate the producer contract")
      val dangling = _topology_graph.replace("\"target\": \"article:runtime\"", "\"target\": \"missing:runtime\"")
      val unsupportedsource = _topology_graph.replace("\"kind\": \"bok-site\"", "\"kind\": \"outside-site\"")
      val unsupportedschema = _topology_graph.replace("cozy.rdf-graph-summary.v1", "cozy.rdf-graph-summary.v2")
      val malformedtruncation = _topology_graph.replace("\"truncated\": true", "\"truncated\": \"true\"")
      val conflicting = _topology_graph.replace(
        """    {"id": "article:runtime", "label": "Runtime Article", "node_type": "article", "tags": ["doc"]}""",
        """    {"id": "term:runtime", "label": "Other Runtime", "node_type": "term"}"""
      )
      val oversizednodes = (1 to 513).map { index =>
        s"""{"id":"term:$index","label":"Term $index","node_type":"term"}"""
      }.mkString(",")
      val oversized =
        s"""{"schemaVersion":"cozy.rdf-graph-summary.v1","kind":"rdf-graph-summary","sourceRef":{"kind":"bok-site","value":"knowledgehub"},"nodes":[$oversizednodes],"edges":[],"truncated":false}"""
      val oversizededges = (1 to 2049).map { _ =>
        """{"source":"term:runtime","predicate":"references","target":"term:runtime"}"""
      }.mkString(",")
      val oversizerelationships =
        s"""{"schemaVersion":"cozy.rdf-graph-summary.v1","kind":"rdf-graph-summary","sourceRef":{"kind":"bok-site","value":"knowledgehub"},"nodes":[{"id":"term:runtime","label":"Runtime","node_type":"term"}],"edges":[$oversizededges],"truncated":false}"""
      val overlongidentifier = _topology_graph.replace("term:runtime", "x" * 1025)
      val overlonglabel = _topology_graph.replace("\"label\": \"Runtime\"", s"\"label\": \"${"x" * 513}\"")
      val oversizedterms = _topology_graph.replace(
        "[\"architecture:runtime\"]",
        (1 to 33).map(index => s"\"term:$index\"").mkString("[", ",", "]")
      )
      val oversizedtags = _topology_graph.replace(
        "[\"runtime\", \"core\", \"runtime\"]",
        (1 to 33).map(index => s"\"tag-$index\"").mkString("[", ",", "]")
      )

      When("each invalid graph is normalized through an in-memory resource provider")
      val results = Vector(
        dangling,
        unsupportedsource,
        unsupportedschema,
        malformedtruncation,
        conflicting,
        oversized,
        oversizerelationships,
        overlongidentifier,
        overlonglabel,
        oversizedterms,
        oversizedtags
      ).map { graph =>
        BokSourceReader.read(_context(Map(
          "fixture/metadata/cncf/knowledge-source.json" -> _topology_manifest,
          "fixture/metadata/glossary/terms.json" -> _terms,
          "fixture/repository/catalog/index.json" -> _repository_index,
          "fixture/metadata/rdf/graph.json" -> graph
        )), _source)
      }

      Then("the reader rejects every violation without requiring a rendered page or referenced URI")
      results.foreach(_ should matchPattern { case Consequence.Failure(_) => })
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
        "\"kind\": \"car\", \"namespace\": \"org.simplemodeling.textus\", \"id\": \"Account\", \"artifactId\": \"textus-account\", \"catalog\": \"car/org/simplemodeling/textus/textus-account.yaml\""
      )
      val crossnamespacecomponents = _repository_index.replace(
        "\"kind\": \"sar\", \"artifactId\": \"textus-runtime\", \"catalog\": \"sar/textus-runtime.yaml\"",
        "\"kind\": \"car\", \"namespace\": \"com.simplemodeling.textus\", \"id\": \"Account\", \"artifactId\": \"textus-account\", \"catalog\": \"car/com/simplemodeling/textus/textus-account.yaml\""
      )
      val aggregatemanifest =
        """{
          |  "schemaVersion": "cncf.knowledge-source.v1",
          |  "resources": [
          |    {"kind": "component-repository-index", "href": "repository/catalog/index.json"},
          |    {"kind": "component-reference-index", "href": "metadata/cncf/component-references/sar.json"},
          |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
          |  ]
          |}""".stripMargin
      val aggregatereference = _sar_reference_index.replace("nict-knowledgehub", "textus-runtime")

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
      val crossnamespaceresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> crossnamespacecomponents
      )), _source)
      val aggregateresult = BokSourceReader.read(_context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> aggregatemanifest,
        "fixture/metadata/glossary/terms.json" -> _terms,
        "fixture/repository/catalog/index.json" -> _repository_index,
        "fixture/metadata/cncf/component-references/sar.json" -> aggregatereference
      )), _source)

      Then("conflicting identities are rejected while equal artifact names in distinct namespaces remain distinct")
      termresult should matchPattern { case Consequence.Failure(_) => }
      componentresult should matchPattern { case Consequence.Failure(_) => }
      aggregateresult should matchPattern { case Consequence.Failure(_) => }
      crossnamespaceresult.TAKE.components.map(x => (x.kind.value, x.organization.map(_.value), x.name.value)) shouldBe Vector(
        ("car", Some("com.simplemodeling.textus"), "textus-account"),
        ("car", Some("org.simplemodeling.textus"), "textus-account")
      )
    }
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

  private val _topology_manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-repository-index", "href": "repository/catalog/index.json"},
      |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"},
      |    {"kind": "rdf-graph-summary", "href": "metadata/rdf/graph.json"}
      |  ]
      |}""".stripMargin

  private val _topology_graph =
    """{
      |  "schemaVersion": "cozy.rdf-graph-summary.v1",
      |  "kind": "rdf-graph-summary",
      |  "sourceRef": {"kind": "bok-site", "value": "knowledgehub", "uri": "https://example.test/knowledgehub"},
      |  "nodes": [
      |    {"id": "term:runtime", "label": "Runtime", "node_type": "term", "category": "architecture", "terms": ["architecture:runtime"], "tags": ["runtime", "core", "runtime"]},
      |    {"id": "article:runtime", "label": "Runtime Article", "node_type": "article", "tags": ["doc"]}
      |  ],
      |  "edges": [
      |    {"source": "term:runtime", "predicate": "references", "target": "article:runtime", "label": "References", "category": "documentation", "terms": ["architecture:runtime"], "tags": ["source", "source"]}
      |  ],
      |  "truncated": true
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
      |  "schemaVersion": "cncf.component-repository-index.v2",
      |  "generatedAt": "2026-07-21T00:00:00Z",
      |  "artifacts": [
      |    {"kind": "sar", "artifactId": "textus-runtime", "catalog": "sar/textus-runtime.yaml", "status": "active", "latestSnapshot": "1.0.0-SNAPSHOT"},
      |    {"kind": "car", "namespace": "org.simplemodeling.textus", "id": "Account", "artifactId": "textus-account", "catalog": "car/org/simplemodeling/textus/textus-account.yaml", "status": "active", "recommended": "0.2.0", "latestStable": "0.2.0"}
      |  ]
      |}""".stripMargin

  private val _empty_repository_index =
    """{
      |  "schemaVersion": "cncf.component-repository-index.v2",
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
