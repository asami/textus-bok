package org.simplemodeling.textus.bok

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ResolvedConfiguration}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.value.BokKnowledgeSource
import org.simplemodeling.textus.semanticintegration.SemanticIntegrationEngineComponent
import org.simplemodeling.textus.semanticintegration.SemanticIntegrationEngineComponent.{KnowledgeFederationService, SemanticRetrievalService}

/*
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokFederationPublicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK knowledge-source replacement" should {
    "publish complete generations through the generic SIE component contract" in {
      Given("BoK and generated-contract SIE components in one subsystem with metadata-only resources")
      val assembly = _assembly(includeSie = true)
      val firstcontext = _context(_first_resources)
      val firstsource = _source("generation-1")

      When("the first generation and its identical retry are replaced through the BoK action")
      val first = _replace(assembly.bok, firstcontext, firstsource)
      val repeated = _replace(assembly.bok, firstcontext, firstsource)
      val firstinspection = _inspect(assembly.sie.get, firstcontext, firstsource)

      Then("SIE reports one complete provider-neutral dataset without duplicate records")
      first.getString("status") shouldBe Some("complete")
      first.getInt("termCount") shouldBe Some(2)
      first.getInt("componentCount") shouldBe Some(2)
      repeated.getString("status") shouldBe Some("complete")
      firstinspection.getString("state") shouldBe Some("complete")
      firstinspection.getString("generation") shouldBe Some("generation-1")
      firstinspection.getInt("documentCount") shouldBe Some(4)
      firstinspection.getInt("assertionCount") shouldBe Some(4)
      firstinspection.getInt("evidenceCount") shouldBe Some(3)

      When("a semantic term candidate is hidden behind unrelated generic SIE results")
      val candidate = _search_terms(assembly.bok, firstcontext, "execution environment", 1)

      Then("the generated SIE query contract is overfetched and the BoK candidate remains source-scoped")
      candidate.getString("status") shouldBe Some("matched")
      val matched = candidate.getVector("results").get.collect { case x: Record => x }
      matched should have size 1
      matched.head.getString("matchKind") shouldBe Some("candidate")
      assembly.recorder.get.queryLimits shouldBe Vector(10, 20)

      When("a later complete generation contains one term and no component references")
      val secondcontext = _context(_second_resources)
      val secondsource = _source("generation-2")
      val second = _replace(assembly.bok, secondcontext, secondsource)
      val secondinspection = _inspect(assembly.sie.get, secondcontext, secondsource)

      Then("the generic replacement removes every stale document assertion and evidence")
      second.getString("status") shouldBe Some("complete")
      second.getInt("termCount") shouldBe Some(1)
      second.getInt("componentCount") shouldBe Some(0)
      secondinspection.getString("generation") shouldBe Some("generation-2")
      secondinspection.getInt("documentCount") shouldBe Some(1)
      secondinspection.getInt("assertionCount") shouldBe Some(1)
      secondinspection.getInt("evidenceCount") shouldBe Some(1)
      secondinspection.getString("rdfStatus") shouldBe Some("ready")
      secondinspection.getString("vectorStatus") shouldBe Some("ready")
    }

    "fail without bypassing the component boundary when SIE is absent" in {
      Given("a BoK component whose subsystem has no SemanticIntegrationEngine component")
      val assembly = _assembly(includeSie = false)

      When("knowledge-source replacement is requested")
      val result = _replace_c(assembly.bok, _context(_first_resources), _source("generation-1"))

      Then("the operation returns a structured service failure instead of using a provider directly")
      result should matchPattern { case Consequence.Failure(_) => }
    }
  }

  private def _assembly(includeSie: Boolean): Assembly = {
    val subsystem = new Subsystem(
      name = "textus-bok-federation-spec",
      configuration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
    )
    val params = ComponentCreate(subsystem, ComponentOrigin.Main)
    val bok = new impl.ComponentFactory().create(params).primary
    val recorder = Option.when(includeSie)(new RecordingSieFactory())
    val sie = recorder.map(_.create(params).primary)
    subsystem.add(sie.toVector :+ bok)
    Assembly(bok, sie, recorder)
  }

  private def _replace(
    bok: Component,
    context: ExecutionContext,
    source: BokKnowledgeSource
  ): Record =
    _record(_replace_c(bok, context, source).TAKE)

  private def _replace_c(
    bok: Component,
    context: ExecutionContext,
    source: BokKnowledgeSource
  ): Consequence[OperationResponse] = {
    val action = BokComponent.BokRetrievalService.ReplaceKnowledgeSourceRequest.unsafeForTest(
      null,
      Record.dataAuto("source" -> source.toRecord())
    )
    action.createCall(ActionCall.Core(action, context, Some(bok), None)).execute()
  }

  private def _inspect(
    sie: Component,
    context: ExecutionContext,
    source: BokKnowledgeSource
  ): Record = {
    val request = Request.of(
      component = sie.name,
      service = "KnowledgeFederation",
      operation = "inspectDataset",
      properties = List(
        Property("datasetId", source.datasetId.value, None),
        Property("sourceId", source.sourceId.value, None)
      )
    )
    val action = KnowledgeFederationService.FederationDatasetInspectionRequest.create(request).TAKE
    _record(action.createCall(ActionCall.Core(action, context, Some(sie), None)).execute().TAKE)
  }

  private def _search_terms(
    bok: Component,
    context: ExecutionContext,
    query: String,
    limit: Int
  ): Record = {
    val action = BokComponent.BokRetrievalService.SearchTermsRequest.unsafeForTest(
      null,
      Record.dataAuto("query" -> query, "limit" -> limit)
    )
    _record(action.createCall(ActionCall.Core(action, context, Some(bok), None)).execute().TAKE)
  }

  private def _record(response: OperationResponse): Record =
    response match {
      case OperationResponse.RecordResponse(record) => record
      case other => fail(s"Expected RecordResponse but got ${other.getClass.getName}")
    }

  private def _context(contents: Map[String, String]): ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _source(generation: String): BokKnowledgeSource =
    BokKnowledgeSource(
      BokSourceId("simplemodeling"),
      BokDatasetId("simplemodeling-bok"),
      BokSourceGeneration(generation),
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

  private val _first_resources = Map(
    "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
    "fixture/metadata/glossary/terms.json" ->
      """{"terms":[
        |{"id":"architecture:runtime","title":"Runtime","definition_text":"Runtime environment.","term_type":"concept"},
        |{"id":"architecture:component","title":"Component","definition_text":"Reusable component.","term_type":"concept"}
        |]}""".stripMargin,
    "fixture/repository/catalog/index.json" ->
      """{"schemaVersion":"cncf.component-repository-index.v1","generatedAt":"2026-07-21T00:00:00Z","artifacts":[
        |{"kind":"car","artifactId":"textus-account","catalog":"car/textus-account.yaml","status":"active","recommended":"0.2.0"},
        |{"kind":"sar","artifactId":"textus-runtime","catalog":"sar/textus-runtime.yaml","status":"active","latestSnapshot":"1.0.0-SNAPSHOT"}
        |]}""".stripMargin
  )

  private val _second_resources = Map(
    "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
    "fixture/metadata/glossary/terms.json" ->
      """{"terms":[
        |{"id":"architecture:component","title":"Component","definition_text":"Updated reusable component.","term_type":"concept"}
        |]}""".stripMargin,
    "fixture/repository/catalog/index.json" ->
      """{"schemaVersion":"cncf.component-repository-index.v1","generatedAt":"2026-07-21T01:00:00Z","artifacts":[]}"""
  )

  private final case class Assembly(
    bok: Component,
    sie: Option[Component],
    recorder: Option[RecordingSieFactory]
  )

  private final class RecordingSieFactory extends SemanticIntegrationEngineComponent.Factory {
    private var _replacement: Option[Record] = None
    private var _query_limits = Vector.empty[Int]

    def queryLimits: Vector[Int] = _query_limits

    override protected def create_Component(params: ComponentCreate): Component =
      new RecordingSieComponent()

    override val KnowledgeFederation: SemanticIntegrationEngineComponent.KnowledgeFederationServiceFactory =
      new RecordingKnowledgeFederationServiceFactory()

    override val SemanticRetrieval: SemanticIntegrationEngineComponent.SemanticRetrievalServiceFactory =
      new RecordingSemanticRetrievalServiceFactory()

    private final class RecordingSemanticRetrievalServiceFactory
      extends SemanticIntegrationEngineComponent.SemanticRetrievalServiceFactory {
      import SemanticRetrievalService.*

      override def createQueryActionCall(
        core: ActionCall.Core,
        action: SemanticQueryRequest
      ): QueryActionCall =
        RecordingQueryActionCall(core, action)
    }

    private final case class RecordingQueryActionCall(
      core: ActionCall.Core,
      override val action: SemanticRetrievalService.SemanticQueryRequest
    ) extends SemanticRetrievalService.QueryActionCall {
      protected def build_Program: ExecUowM[OperationResponse] =
        exec_from {
          val limit = action.record.getInt("limit").getOrElse(10)
          _query_limits = _query_limits :+ limit
          val sourceid = action.record.getString("sourceId").getOrElse("")
          val decoys = Vector.tabulate(12) { index =>
            Record.dataAuto(
              "documentId" -> s"generic-$index",
              "sourceId" -> sourceid,
              "score" -> 0.99,
              "metadata" -> "{\"domain\":\"other\"}"
            )
          }
          val published = _replacement.toVector.flatMap(
            _.getVector("documents").toVector.flatten.collect { case x: Record => x }
          ).map { document =>
            Record.dataAuto(
              "documentId" -> document.getString("id").getOrElse(""),
              "sourceId" -> document.getString("sourceId").getOrElse(""),
              "score" -> (if (document.getString("title").contains("Runtime")) 0.9 else 0.1),
              "metadata" -> document.getString("metadata").getOrElse("{}")
            )
          }
          Consequence.success(OperationResponse(Record.dataAuto(
            "query" -> action.record.getString("text").getOrElse(""),
            "results" -> (decoys ++ published).take(limit),
            "rdfResults" -> Vector.empty[Record]
          )))
        }
    }

    private final class RecordingKnowledgeFederationServiceFactory
      extends SemanticIntegrationEngineComponent.KnowledgeFederationServiceFactory {
      import KnowledgeFederationService.*

      override def createReplaceDatasetActionCall(
        core: ActionCall.Core,
        action: FederationDatasetReplacementRequest
      ): ReplaceDatasetActionCall =
        RecordingReplaceActionCall(core, action)

      override def createInspectDatasetActionCall(
        core: ActionCall.Core,
        action: FederationDatasetInspectionRequest
      ): InspectDatasetActionCall =
        RecordingInspectActionCall(core, action)
    }

    private final case class RecordingReplaceActionCall(
      core: ActionCall.Core,
      override val action: KnowledgeFederationService.FederationDatasetReplacementRequest
    ) extends KnowledgeFederationService.ReplaceDatasetActionCall {
      protected def build_Program: ExecUowM[OperationResponse] =
        exec_from {
          _replacement = Some(action.record)
          Consequence.success(OperationResponse(_response(action.record, "complete")))
        }
    }

    private final case class RecordingInspectActionCall(
      core: ActionCall.Core,
      override val action: KnowledgeFederationService.FederationDatasetInspectionRequest
    ) extends KnowledgeFederationService.InspectDatasetActionCall {
      protected def build_Program: ExecUowM[OperationResponse] =
        exec_from {
          val response = _replacement.filter { replacement =>
            replacement.getString("datasetId") == action.record.getString("datasetId") &&
              replacement.getString("sourceId") == action.record.getString("sourceId")
          }.map(_response(_, "complete")).getOrElse(
            Record.dataAuto(
              "datasetId" -> action.record.getString("datasetId").getOrElse(""),
              "sourceId" -> action.record.getString("sourceId").getOrElse(""),
              "state" -> "absent",
              "documentCount" -> 0,
              "assertionCount" -> 0,
              "evidenceCount" -> 0,
              "rdfStatus" -> "absent",
              "vectorStatus" -> "absent",
              "failedProviders" -> Vector.empty[String]
            )
          )
          Consequence.success(OperationResponse(response))
        }
    }

    private def _response(replacement: Record, state: String): Record =
      Record.dataAuto(
        "datasetId" -> replacement.getString("datasetId").getOrElse(""),
        "sourceId" -> replacement.getString("sourceId").getOrElse(""),
        "generation" -> replacement.getString("generation"),
        "state" -> state,
        "documentCount" -> replacement.getVector("documents").fold(0)(_.size),
        "assertionCount" -> replacement.getVector("assertions").fold(0)(_.size),
        "evidenceCount" -> replacement.getVector("evidence").fold(0)(_.size),
        "rdfStatus" -> "ready",
        "vectorStatus" -> "ready",
        "failedProviders" -> Vector.empty[String]
      )
  }

  private final class RecordingSieComponent extends Component
}
