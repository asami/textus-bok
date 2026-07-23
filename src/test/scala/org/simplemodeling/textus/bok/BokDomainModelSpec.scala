package org.simplemodeling.textus.bok

import org.goldenport.schema.DataType
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class BokDomainModelSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK domain model" should {
    "publish typed terminology and component-existence operation contracts" in {
      Given("the generated BoK retrieval service definition")
      val service = BokComponent.BokRetrievalService

      When("its operation and response contracts are inspected")
      val operations = service.operations.operations.toVector
      val responses = operations.map { operation =>
        operation.name -> operation.specification.response.result.collect {
          case DataType.Named(name) => name
        }
      }.toMap

      Then("the administration and five read boundaries use owned response models")
      responses shouldBe Map(
        "replaceKnowledgeSource" -> List("ReplaceKnowledgeSourceResponse"),
        "searchTerms" -> List("SearchTermsResponse"),
        "explainTerm" -> List("ExplainTermResponse"),
        "searchComponentReferences" -> List("ComponentReferenceSearchResponse"),
        "getComponentReference" -> List("ComponentReferenceLookupResponse"),
        "getKnowledgeMap" -> List("GetKnowledgeMapResponse")
      )
    }

    "generate source, evidence, term, and existence-only reference models" in {
      Given("the CML domain model compiled into generated Scala types")

      When("the owned model classes are resolved")
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

      Then("each BoK-owned boundary has a distinct generated type")
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

      And("the generated term and evidence attributes retain their CML types and multiplicities")
      val termmethods = classOf[org.simplemodeling.textus.bok.value.BokTerm]
        .getMethods.map(method => method.getName -> method.getReturnType).toMap
      val evidencemethods = classOf[org.simplemodeling.textus.bok.value.BokEvidence]
        .getMethods.map(method => method.getName -> method.getReturnType).toMap
      termmethods("termId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokTermId]
      termmethods("title") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokTermTitle]
      termmethods("definition") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokTermDefinition]
      termmethods("category") shouldBe classOf[Option[?]]
      termmethods("evidence") shouldBe classOf[org.simplemodeling.textus.bok.value.BokEvidence]
      evidencemethods("uri") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokEvidenceUri]
      evidencemethods("sourceId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokSourceId]
      evidencemethods("sourceVersion") shouldBe classOf[Option[?]]

      And("the Knowledge Map node keeps its stable identity as a typed nodeId")
      val mapnodemethods = classOf[org.simplemodeling.textus.bok.value.BokKnowledgeMapNode]
        .getMethods.map(method => method.getName -> method.getReturnType).toMap
      mapnodemethods("nodeId") shouldBe classOf[org.simplemodeling.textus.bok.datatype.BokKnowledgeMapNodeId]
      mapnodemethods("componentReferences") shouldBe classOf[Vector[?]]
    }

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
