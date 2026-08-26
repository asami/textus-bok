package org.simplemodeling.textus.bok

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.goldenport.Consequence
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
 * The source is admitted from digest-bound structured ResourceAccess values.
 * The public result is value-only metadata: source contents, permissions, and
 * execution authority are not part of the projection.
 *
 * @since   Aug. 27, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class ComponentKnowledgeRetrievalSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "DOC-07 public semantic retrieval" should {
    "project attributable component knowledge metadata without source authority" in {
      Given("a digest-bound consumer contract and semantic index admitted through in-memory ResourceAccess")
      val context = _context_for(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _source_manifest,
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))
      val normalized = BokSourceReader.read(context, _source).TAKE
      val catalog = new BokKnowledgeCatalog()
      catalog.commit(BokFederationPublication("complete", Record.empty), normalized) shouldBe true
      val access = BokSemanticAccess(_resolved_profile, BokSemanticAccess.CallerPrivilege.Public)

      When("the public catalog reads the exact semantic identity")
      val response = catalog.searchSemanticKnowledge(access, "Component knowledge", 1)(_ => Consequence.success(Map.empty)).TAKE
      val record = response.results.head

      Then("the result preserves exact public metadata and excludes source contents, permissions, and execution authority")
      response.status.value shouldBe "matched"
      record.identity shouldBe "component-knowledge:fixture"
      record.product shouldBe Some("cncf")
      record.version shouldBe Some("1.0.0")
      record.profile shouldBe Some("official")
      record.owner shouldBe Some("cncf")
      record.license shouldBe Some("Apache-2.0")
      record.logicalPath shouldBe Some("documentation/fixture.md")
      record.chunkId shouldBe Some("chunk-0001")
      record.publicationGeneration shouldBe Some("2026-08-26")
      record.publicationDigest shouldBe Some(_sha256("publication:cncf:1.0.0"))
      record.summary shouldBe "Public semantic metadata only."
      record.visibility shouldBe "public"
      record.authority shouldBe "framework-publication"
      record.componentReference shouldBe empty
    }
  }

  private def _context_for(contents: Map[String, String]): ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map(byte => f"${byte & 0xff}%02x")
      .mkString

  private val _source = BokKnowledgeSource(
    BokSourceId("source-a"),
    BokDatasetId("dataset-a"),
    BokSourceGeneration("g1"),
    BokResourceReference("urn:textus:bok:fixture")
  )

  private val _resolved_profile = ResolvedBokProfile(
    "official",
    None,
    _source.datasetId,
    _source.sourceId,
    _source.generation,
    BokEvidence(
      BokEvidenceUri("urn:textus:bok:source-a:selection"),
      _source.sourceId,
      None,
      None,
      None
    )
  )

  private lazy val _source_manifest =
    s"""{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-knowledge-consumer-contract", "href": "metadata/cncf/component-knowledge-consumer-contract.json", "sha256": "${_sha256(_consumer_contract)}"},
      |    {"kind": "semantic-index", "href": "metadata/cncf/semantic-index.json", "sha256": "${_sha256(_semantic_index)}"}
      |  ]
      |}""".stripMargin

  private lazy val _consumer_contract =
    s"""{
      |  "schema": "cncf.component-knowledge-consumer.v1",
      |  "componentId": "org.example.fixture.ComponentKnowledgeRetrieval",
      |  "logicalRelease": "1.0.0",
      |  "resources": [
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.fixture.ComponentKnowledgeRetrieval",
      |        "logicalRelease": "1.0.0",
      |        "parentComponentId": null,
      |        "childRole": "Documentation",
      |        "logicalResource": "urn:cncf:resource:fixture:documentation"
      |      },
      |      "logicalPath": "documentation/fixture.md",
      |      "kind": "documentation",
      |      "role": "documentation",
      |      "language": "en",
      |      "mediaType": "text/markdown",
      |      "size": 1,
      |      "sha256": "${_sha256("documentation:fixture")}",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example.fixture:component-knowledge-retrieval:1.0.0",
      |        "logicalSource": "component-car:fixture",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "${_sha256("documentation:fixture")}"
      |      }
      |    }
      |  ]
      |}""".stripMargin

  private lazy val _semantic_index =
    s"""{
      |  "schemaVersion": "cncf.semantic-index.v1",
      |  "kind": "semantic-index",
      |  "sourceId": "source-a",
      |  "datasetId": "dataset-a",
      |  "generation": "g1",
      |  "records": [{
      |    "kind": "component-manifest",
      |    "identity": "component-knowledge:fixture",
      |    "title": "Component Knowledge Fixture",
      |    "summary": "Public semantic metadata only.",
      |    "documentId": "component-knowledge-fixture",
      |    "sectionId": null,
      |    "canonicalUrl": "https://example.test/components/knowledge",
      |    "indexedAt": "2026-08-26T00:00:00Z",
      |    "visibility": "public",
      |    "authority": "framework-publication",
      |    "stale": false,
      |    "sha256": "${_sha256("publication:cncf:1.0.0")}",
      |    "product": "cncf",
      |    "version": "1.0.0",
      |    "profile": "official",
      |    "owner": "cncf",
      |    "license": "Apache-2.0",
      |    "logicalPath": "documentation/fixture.md",
      |    "chunkId": "chunk-0001",
      |    "publicationGeneration": "2026-08-26",
      |    "publicationDigest": "${_sha256("publication:cncf:1.0.0")}"
      |  }]
      |}""".stripMargin
}
