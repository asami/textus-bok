package org.simplemodeling.textus.bok

import org.goldenport.schema.{DataType, Multiplicity}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 21, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokDomainModelSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK domain model" should {
    "generated operation contracts" which {
      "publish owned response models for administration and public reads" in {
        Given("the generated BoK retrieval service definition")
        val service = BokComponent.BokRetrievalService

        When("its operation response contracts are inspected")
        val responses = service.operations.operations.toVector.map { operation =>
          operation.name -> operation.specification.response.result.collect {
            case DataType.Named(name) => name
          }
        }.toMap

        Then("each administration or read boundary uses its owned response model")
        responses shouldBe Map(
          "replaceKnowledgeSource" -> List("ReplaceKnowledgeSourceResponse"),
          "searchTerms" -> List("SearchTermsResponse"),
          "explainTerm" -> List("ExplainTermResponse"),
          "searchComponentReferences" -> List("ComponentReferenceSearchResponse"),
          "getComponentReference" -> List("ComponentReferenceLookupResponse"),
          "getKnowledgeMap" -> List("GetKnowledgeMapResponse"),
          "searchSemanticKnowledge" -> List("SemanticKnowledgeSearchResponse"),
          "getSemanticManifest" -> List("SemanticManifestResponse"),
          "getSemanticResource" -> List("SemanticResourceResponse"),
          "getSemanticSection" -> List("SemanticSectionResponse"),
          "discoverSemanticKnowledge" -> List("SemanticKnowledgeDiscoveryResponse")
        )
      }

      "retain the component lookup identity request fields" in {
        Given("the generated component-reference lookup operation")
        val lookup = BokComponent.BokRetrievalService.operations.operations.toVector
          .find(_.name == "getComponentReference")
          .getOrElse(fail("getComponentReference operation is missing"))

        When("its request parameters are inspected")
        val parameters = lookup.specification.request.parameters.map(parameter => parameter.name -> parameter).toMap

        Then("name remains required and organization remains optional")
        parameters("name").datatype.name shouldBe "componentname"
        parameters("name").multiplicity shouldBe Multiplicity.One
        parameters("organization").datatype.name shouldBe "componentorganization"
        parameters("organization").multiplicity shouldBe Multiplicity.ZeroOne
      }

      "carry the shared optional selector on every public read" in {
        Given("the generated BoK retrieval operations")
        val operations = BokComponent.BokRetrievalService.operations.operations.toVector

        When("the public read request fields are inspected")
        val reads = Set(
          "searchTerms",
          "explainTerm",
          "searchComponentReferences",
          "getComponentReference",
          "getKnowledgeMap"
        )
        val requestparameters = operations.map { operation =>
          operation.name -> operation.specification.request.parameters.map(parameter => parameter.name -> parameter).toMap
        }.toMap

        Then("profile and projectId are optional only on the five public reads")
        reads.foreach { operation =>
          requestparameters(operation)("profile").datatype.name shouldBe "bokprofile"
          requestparameters(operation)("profile").multiplicity shouldBe Multiplicity.ZeroOne
          requestparameters(operation)("projectId").datatype.name shouldBe "bokprojectid"
          requestparameters(operation)("projectId").multiplicity shouldBe Multiplicity.ZeroOne
        }
        requestparameters("replaceKnowledgeSource").keySet shouldBe Set("source")
      }
    }

    "generated domain and value shapes" which {
      "generate source, evidence, term, reference, and Knowledge Map value models" in {
        Given("the CML domain model compiled into generated Scala types")

        When("the non-selection value classes are resolved")
        val models = Vector(
          classOf[org.simplemodeling.textus.bok.value.BokKnowledgeSource],
          classOf[org.simplemodeling.textus.bok.value.BokEvidence],
          classOf[org.simplemodeling.textus.bok.value.BokTerm],
          classOf[org.simplemodeling.textus.bok.value.BokTermMatch],
          classOf[org.simplemodeling.textus.bok.value.ComponentReference],
          classOf[org.simplemodeling.textus.bok.value.ComponentReferenceMatch],
          classOf[org.simplemodeling.textus.bok.value.BokKnowledgeMapSelectedSource],
          classOf[org.simplemodeling.textus.bok.value.BokKnowledgeMapNode],
          classOf[org.simplemodeling.textus.bok.value.BokKnowledgeMapRelationship]
        )
        val termmethods = classOf[org.simplemodeling.textus.bok.value.BokTerm]
          .getMethods.map(method => method.getName -> method.getReturnType).toMap
        val evidencemethods = classOf[org.simplemodeling.textus.bok.value.BokEvidence]
          .getMethods.map(method => method.getName -> method.getReturnType).toMap
        val mapnodemethods = classOf[org.simplemodeling.textus.bok.value.BokKnowledgeMapNode]
          .getMethods.map(method => method.getName -> method.getReturnType).toMap

        Then("each value boundary keeps its generated type and typed accessors")
        models.map(_.getSimpleName).toSet shouldBe Set(
          "BokKnowledgeSource",
          "BokEvidence",
          "BokTerm",
          "BokTermMatch",
          "ComponentReference",
          "ComponentReferenceMatch",
          "BokKnowledgeMapSelectedSource",
          "BokKnowledgeMapNode",
          "BokKnowledgeMapRelationship"
        )
        termmethods("termId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokTermId]
        termmethods("title") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokTermTitle]
        termmethods("definition") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokTermDefinition]
        termmethods("category") shouldBe classOf[Option[?]]
        termmethods("evidence") shouldBe classOf[org.simplemodeling.textus.bok.value.BokEvidence]
        evidencemethods("uri") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokEvidenceUri]
        evidencemethods("sourceId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokSourceId]
        evidencemethods("sourceVersion") shouldBe classOf[Option[?]]
        mapnodemethods("nodeId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokKnowledgeMapNodeId]
        mapnodemethods("componentReferences") shouldBe classOf[Vector[?]]
      }

      "expose one resolved selection value on every successful read response" in {
        Given("the generated resolved selection and public read response types")
        val selection = classOf[org.simplemodeling.textus.bok.value.BokResolvedSelection]
        val readresponses = Vector(
          classOf[org.simplemodeling.textus.bok.value.SearchTermsResponse],
          classOf[org.simplemodeling.textus.bok.value.ExplainTermResponse],
          classOf[org.simplemodeling.textus.bok.value.ComponentReferenceSearchResponse],
          classOf[org.simplemodeling.textus.bok.value.ComponentReferenceLookupResponse],
          classOf[org.simplemodeling.textus.bok.value.GetKnowledgeMapResponse]
        )

        When("the resolved-selection and response accessors are inspected")
        val selectionmethods = selection.getMethods.map(method => method.getName -> method.getReturnType).toMap
        val accessors = readresponses.map(_.getMethods.find(_.getName == "selection").map(_.getReturnType))

        Then("the selection retains profile, exact generation, and evidence for every read")
        selection.getSimpleName shouldBe "BokResolvedSelection"
        selectionmethods("resolvedProfile") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokProfile]
        selectionmethods("projectId") shouldBe classOf[Option[?]]
        selectionmethods("datasetId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokDatasetId]
        selectionmethods("sourceId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokSourceId]
        selectionmethods("generation") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokSourceGeneration]
        selectionmethods("evidence") shouldBe classOf[org.simplemodeling.textus.bok.value.BokEvidence]
        accessors shouldBe Vector.fill(readresponses.size)(Some(selection))
      }
    }

    "component reference shape" which {
      "exclude CBD-owned detail from the generated component reference" in {
      Given("the existence-only ComponentReference model")
      val referencemethods = classOf[org.simplemodeling.textus.bok.value.ComponentReference]
        .getMethods.map(method => method.getName -> method.getReturnType).toMap

      When("its public generated attributes are inspected")
      val requiredtypes = Map(
        "name" -> classOf[org.simplemodeling.textus.bok.datatype.ComponentName],
        "title" -> classOf[org.simplemodeling.textus.bok.datatype.ComponentTitle],
        "kind" -> classOf[org.simplemodeling.textus.bok.datatype.ComponentKind],
        "evidence" -> classOf[org.simplemodeling.textus.bok.value.BokEvidence]
      )
      val cbdfields = Set(
        "capabilities",
        "dependencies",
        "operations",
        "runtimeCompatibility",
        "manuals",
        "usageGuidance"
      )

      Then("identity and evidence are required while source enrichment remains optional")
      requiredtypes.foreach { case (name, expected) => referencemethods(name) shouldBe expected }
      referencemethods("sourceId") shouldBe classOf[Option[?]]
      referencemethods("catalogId") shouldBe classOf[Option[?]]
      referencemethods("organization") shouldBe classOf[Option[?]]
      referencemethods("version") shouldBe classOf[Option[?]]

      And("no CBD detail accessor is generated")
      referencemethods.keySet.intersect(cbdfields) shouldBe empty
      }
    }

    "MCP readiness" which {
      "keep Knowledge Map public but outside the MCP-ready reads" in {
      Given("the Phase 4 primary component")
      val component = new impl.BokPrimaryComponent()
      val operations = Vector(
        "replaceKnowledgeSource",
        "searchTerms",
        "explainTerm",
        "searchComponentReferences",
        "getComponentReference",
        "getKnowledgeMap"
      )

      When("the component MCP policy is evaluated")
      val readiness = operations.map(operation => operation -> component.isMcpReady("BokRetrieval", operation))

      Then("the four existing MCP reads are ready while mutation and Knowledge Map remain private to MCP")
      readiness.toMap shouldBe Map(
        "replaceKnowledgeSource" -> false,
        "searchTerms" -> true,
        "explainTerm" -> true,
        "searchComponentReferences" -> true,
        "getComponentReference" -> true,
        "getKnowledgeMap" -> false
      )
      }
    }
  }
}
