package org.simplemodeling.textus.bok

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.component.repository.{
  ComponentResourceAuthorization,
  ComponentResourceAvailability,
  ComponentResourceIntegrity
}
import org.goldenport.cncf.knowledge.ComponentKnowledgeManifestConsumerContractCodec
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.*
import org.simplemodeling.textus.bok.value.*

/*
 * Executable acceptance specification for Phase 59.8 / Step P598-S2.
 *
 * This specification exercises only the existing digest-bound, value-only
 * BoK reader and semantic catalog. Its resources are supplied by an
 * in-memory ResourceAccess provider; no filesystem, network, CAR, provider
 * activation, or MCP authority participates in the scenarios.
 *
 * @since   Aug. 27, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class RepresentativeDocumentationProfileRetrievalSpec
    extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  "P598-S2 representative documentation profile BoK retrieval" should {
    "admit exactly attributable public framework, Directive, and Skill Catalog records" in {
      Given("a valid digest-bound cncf.knowledge-source.v1 manifest with exactly one consumer contract and one semantic index")
      val contract = ComponentKnowledgeManifestConsumerContractCodec.decodeC(_consumer_contract).TAKE
      contract.frameworkPublication.map(_.availability.code) shouldBe Some("online")
      contract.resources.map(_.kind.code) should contain allOf (
        "framework-documentation",
        "directive",
        "skill-catalog",
        "source-code"
      )
      contract.resources.find(_.kind.code == "source-code").foreach { resource =>
        resource.availability shouldBe ComponentResourceAvailability.Available
        resource.integrity shouldBe ComponentResourceIntegrity.Verified
        resource.authorization shouldBe ComponentResourceAuthorization.Denied
      }
      val normalized = BokSourceReader.read(
        _context_for(Map(
          "representative/metadata/cncf/knowledge-source.json" -> _source_manifest,
          "representative/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
          "representative/metadata/cncf/semantic-index.json" -> _semantic_index
        )),
        _source
      ).TAKE
      val catalog = new BokKnowledgeCatalog()
      catalog.commit(BokFederationPublication("complete", Record.empty), normalized) shouldBe true
      val access = BokSemanticAccess(_resolved_profile, BokSemanticAccess.CallerPrivilege.Public)

      When("the selected public semantic catalog reads the normalized source")
      val discovered = catalog.discoverSemanticKnowledge(access, 10)
      val framework = normalized.semanticRecords.find(_.identity == _framework_identity).getOrElse(fail("framework record"))
      val directive = normalized.semanticRecords.find(_.identity == _directive_identity).getOrElse(fail("Directive record"))
      val skillcatalog = normalized.semanticRecords.find(_.identity == _skill_catalog_identity).getOrElse(fail("Skill Catalog record"))
      val frameworkresponse = catalog.getSemanticResource(access, _framework_identity)
      val directiveresponse = catalog.getSemanticResource(access, _directive_identity)
      val skillcatalogresponse = catalog.getSemanticResource(access, _skill_catalog_identity)

      Then("only public value-level records are returned with source selection and digest evidence")
      normalized.semanticRecords should have size 3
      normalized.semanticRecords.map(_.identity).distinct shouldBe Vector(
        _framework_identity,
        _directive_identity,
        _skill_catalog_identity
      )
      normalized.semanticRecords.foreach { record =>
        record.visibility shouldBe "public"
        record.sourceId shouldBe "simplemodeling"
        record.datasetId shouldBe "simplemodeling-bok"
        record.generation shouldBe "2026-08-27"
        record.digest should fullyMatch regex "[0-9a-f]{64}"
        record.canonicalUrl should startWith("https://")
      }
      framework.kind shouldBe "framework-publication"
      framework.identity shouldBe _framework_identity
      framework.product shouldBe Some("simplemodeling")
      framework.version shouldBe Some("0.1.0")
      framework.documentId shouldBe "urn:cncf:framework:document:simplemodeling:0.1.0"
      framework.sectionId shouldBe Some("urn:cncf:framework:section:representative")
      framework.canonicalUrl shouldBe "https://simplemodeling.org/framework/0.1.0"
      framework.authority shouldBe "framework-publication"
      framework.digest shouldBe _sha256("framework:source:simplemodeling:0.1.0")
      framework.publicationGeneration shouldBe Some("2026-08-27")
      framework.publicationDigest shouldBe Some(_sha256("framework:publication:simplemodeling:0.1.0"))
      contract.resources.find(_.kind.code == "framework-documentation").map(_.sha256) shouldBe Some(framework.digest)
      directive.kind shouldBe "directive-metadata"
      directive.identity shouldBe _directive_identity
      directive.documentId shouldBe "mounted-directive"
      directive.sectionId shouldBe Some("public-framework-rule")
      directive.canonicalUrl shouldBe "https://simplemodeling.org/directive/public"
      directive.authority shouldBe "mounted-directive-remains-authoritative"
      directive.version shouldBe Some("1.0.0")
      directive.profile shouldBe Some("public-framework")
      directive.logicalPath shouldBe Some("directive/public.yaml")
      directive.digest shouldBe _sha256("directive:public:simplemodeling")
      contract.resources.find(_.kind.code == "directive").map(_.sha256) shouldBe Some(directive.digest)
      skillcatalog.kind shouldBe "skill-metadata"
      skillcatalog.identity shouldBe _skill_catalog_identity
      skillcatalog.documentId shouldBe "public-skill-catalog"
      skillcatalog.canonicalUrl shouldBe "https://simplemodeling.org/skills/public"
      skillcatalog.authority shouldBe "skill-catalog"
      skillcatalog.version shouldBe Some("1.0.0")
      skillcatalog.owner shouldBe Some("simplemodeling")
      skillcatalog.logicalPath shouldBe Some("skills/catalog.json")
      skillcatalog.digest shouldBe _sha256("skill:catalog:simplemodeling")
      contract.resources.find(_.kind.code == "skill-catalog").map(_.sha256) shouldBe Some(skillcatalog.digest)
      normalized.semanticRecords.map(_.identity) should not contain _source_identity
      discovered.status.value shouldBe "matched"
      discovered.results should have size 3
      discovered.selection.sourceId.value shouldBe "simplemodeling"
      discovered.selection.datasetId.value shouldBe "simplemodeling-bok"
      discovered.selection.generation.value shouldBe "2026-08-27"
      discovered.results.map(_.identity).toSet shouldBe Set(
        _framework_identity,
        _directive_identity,
        _skill_catalog_identity
      )
      frameworkresponse.status.value shouldBe "matched"
      frameworkresponse.result.map(_.identity) shouldBe Some(_framework_identity)
      directiveresponse.status.value shouldBe "matched"
      directiveresponse.result.map(_.identity) shouldBe Some(_directive_identity)
      skillcatalogresponse.status.value shouldBe "matched"
      skillcatalogresponse.result.map(_.identity) shouldBe Some(_skill_catalog_identity)
    }

    "reject a semantic-index digest mismatch before projecting any profile record" in {
      Given("the same in-memory representative profile with one altered declared semantic-index digest")
      val mismatchedmanifest = _source_manifest.replace(
        _sha256(_semantic_index),
        "0000000000000000000000000000000000000000000000000000000000000000"
      )

      When("BoK reads the explicitly selected source")
      val result = BokSourceReader.read(
        _context_for(Map(
          "representative/metadata/cncf/knowledge-source.json" -> mismatchedmanifest,
          "representative/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
          "representative/metadata/cncf/semantic-index.json" -> _semantic_index
        )),
        _source
      )

      Then("the digest-bound read fails without producing a semantic result")
      result should matchPattern { case Consequence.Failure(_) => }
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
      .map(byte => String.format("%02x", Integer.valueOf(byte & 0xff)))
      .mkString

  private val _source = BokKnowledgeSource(
    BokSourceId("simplemodeling"),
    BokDatasetId("simplemodeling-bok"),
    BokSourceGeneration("2026-08-27"),
    BokResourceReference("urn:textus:bok:representative")
  )

  private val _resolved_profile = ResolvedBokProfile(
    "official",
    None,
    _source.datasetId,
    _source.sourceId,
    _source.generation,
    BokEvidence(
      BokEvidenceUri("urn:textus:bok:simplemodeling:representative-selection"),
      _source.sourceId,
      None,
      None,
      None
    )
  )

  private val _framework_identity = "urn:cncf:framework:source:simplemodeling:0.1.0"
  private val _directive_identity = "urn:cncf:resource:representative:directive"
  private val _skill_catalog_identity = "urn:cncf:resource:representative:skill-catalog"
  private val _source_identity = "urn:cncf:resource:representative:source-code"

  private lazy val _source_manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-knowledge-consumer-contract", "href": "metadata/cncf/component-knowledge-consumer-contract.json", "sha256": "__CONTRACT_DIGEST__"},
      |    {"kind": "semantic-index", "href": "metadata/cncf/semantic-index.json", "sha256": "__INDEX_DIGEST__"}
      |  ]
      |}""".stripMargin
      .replace("__CONTRACT_DIGEST__", _sha256(_consumer_contract))
      .replace("__INDEX_DIGEST__", _sha256(_semantic_index))

  private lazy val _consumer_contract =
    """{
      |  "schema": "cncf.component-knowledge-consumer.v1",
      |  "componentId": "org.example.RepresentativeDocumentationProfile",
      |  "logicalRelease": "0.1.0",
      |  "frameworkPublication": {
      |    "product": "simplemodeling",
      |    "version": "0.1.0",
      |    "canonicalUrl": "https://simplemodeling.org/framework/0.1.0",
      |    "publicationGeneration": "2026-08-27",
      |    "documentId": "urn:cncf:framework:document:simplemodeling:0.1.0",
      |    "sectionId": "urn:cncf:framework:section:representative",
      |    "sha256": "__FRAMEWORK_PUBLICATION_DIGEST__",
      |    "availability": "online",
      |    "sourceIdentity": "urn:cncf:framework:source:simplemodeling:0.1.0",
      |    "sourceSha256": "__FRAMEWORK_SOURCE_DIGEST__"
      |  },
      |  "publicDirective": {
      |    "logicalIdentity": {
      |      "componentId": "org.example.RepresentativeDocumentationProfile",
      |      "logicalRelease": "0.1.0",
      |      "parentComponentId": null,
      |      "childRole": "Directive",
      |      "logicalResource": "urn:cncf:resource:representative:directive"
      |    },
      |    "directiveId": "mounted-directive",
      |    "profileId": "public-framework",
      |    "ruleId": "public-framework-rule",
      |    "origin": "urn:cncf:directive:simplemodeling:public",
      |    "version": "1.0.0",
      |    "authority": "mounted-directive-remains-authoritative",
      |    "visibility": "public",
      |    "sourceSha256": "__DIRECTIVE_DIGEST__",
      |    "redaction": "source-and-rule-content-withheld",
      |    "guideReference": "https://simplemodeling.org/directive/public"
      |  },
      |  "skillCatalog": {
      |    "logicalIdentity": {
      |      "componentId": "org.example.RepresentativeDocumentationProfile",
      |      "logicalRelease": "0.1.0",
      |      "parentComponentId": null,
      |      "childRole": "SkillCatalog",
      |      "logicalResource": "urn:cncf:resource:representative:skill-catalog"
      |    },
      |    "catalogId": "public-skill-catalog",
      |    "owner": "simplemodeling",
      |    "purpose": "descriptive public Skill Catalog metadata",
      |    "trigger": "explicit user request",
      |    "requirements": ["component knowledge manifest"],
      |    "permissions": ["metadata visibility"],
      |    "sideEffects": ["none"],
      |    "mcpRequirements": ["descriptive only"],
      |    "installationReference": "https://simplemodeling.org/skills/public",
      |    "visibility": "ecosystem",
      |    "version": "1.0.0",
      |    "sourceSha256": "__SKILL_CATALOG_DIGEST__"
      |  },
      |  "resources": [
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "FrameworkDocumentation",
      |        "logicalResource": "urn:cncf:resource:representative:framework"
      |      },
      |      "logicalPath": "framework/simplemodeling-0.1.0.md",
      |      "kind": "framework-documentation",
      |      "role": "framework-documentation",
      |      "language": "en",
      |      "mediaType": "text/markdown",
      |      "size": 1,
      |      "sha256": "__FRAMEWORK_SOURCE_DIGEST__",
      |      "metadata": {
      |        "authority": "framework",
      |        "stability": "stable",
      |        "source": "supplied-phase58",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__FRAMEWORK_SOURCE_DIGEST__"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "Directive",
      |        "logicalResource": "urn:cncf:resource:representative:directive"
      |      },
      |      "logicalPath": "directive/public.yaml",
      |      "kind": "directive",
      |      "role": "directive",
      |      "language": "en",
      |      "mediaType": "application/yaml",
      |      "size": 1,
      |      "sha256": "__DIRECTIVE_DIGEST__",
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
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__DIRECTIVE_DIGEST__"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "SkillCatalog",
      |        "logicalResource": "urn:cncf:resource:representative:skill-catalog"
      |      },
      |      "logicalPath": "skills/catalog.json",
      |      "kind": "skill-catalog",
      |      "role": "skill-catalog",
      |      "language": "en",
      |      "mediaType": "application/json",
      |      "size": 1,
      |      "sha256": "__SKILL_CATALOG_DIGEST__",
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
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__SKILL_CATALOG_DIGEST__"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.RepresentativeDocumentationProfile",
      |        "logicalRelease": "0.1.0",
      |        "parentComponentId": null,
      |        "childRole": "SourceCode",
      |        "logicalResource": "urn:cncf:resource:representative:source-code"
      |      },
      |      "logicalPath": "source/RepresentativeDocumentationProfile.scala",
      |      "kind": "source-code",
      |      "role": "source-code",
      |      "language": "scala",
      |      "mediaType": "text/x-scala",
      |      "size": 1,
      |      "sha256": "__SOURCE_DIGEST__",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "LicenseRef-Internal",
      |        "disclosure": "reference-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "denied",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example:representative-profile:0.1.0",
      |        "logicalSource": "component-car:representative-profile",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": "__SOURCE_DIGEST__"
      |      }
      |    }
      |  ]
      |}""".stripMargin
      .replace("__FRAMEWORK_PUBLICATION_DIGEST__", _sha256("framework:publication:simplemodeling:0.1.0"))
      .replace("__FRAMEWORK_SOURCE_DIGEST__", _sha256("framework:source:simplemodeling:0.1.0"))
      .replace("__DIRECTIVE_DIGEST__", _sha256("directive:public:simplemodeling"))
      .replace("__SKILL_CATALOG_DIGEST__", _sha256("skill:catalog:simplemodeling"))
      .replace("__SOURCE_DIGEST__", _sha256("source:representative:withheld"))

  private lazy val _semantic_index =
    """{
      |  "schemaVersion": "cncf.semantic-index.v1",
      |  "kind": "semantic-index",
      |  "sourceId": "simplemodeling",
      |  "datasetId": "simplemodeling-bok",
      |  "generation": "2026-08-27",
      |  "records": []
      |}""".stripMargin
}
