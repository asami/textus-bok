package org.simplemodeling.textus.bok
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.cncf.spi.SpiResolver
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.testutil.RuntimeBindingAdmissionFixture
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.Protocol
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent
import org.simplemodeling.textus.bok.runtime.{BokCandidateKey, BokFederationPublisher, BokFederationRetriever, BokProfileAuthorization, BokProfileCompatibilityFilter, BokProfileKey, BokProfileSelection, BokSourceReader}
import org.simplemodeling.textus.bok.value.{BokEvidence, BokKnowledgeSource}
import org.simplemodeling.textus.semanticintegration.SemanticIntegrationEngineComponent.KnowledgeFederationService
import org.simplemodeling.textus.semanticintegration.value.{FederationDatasetQueryRequest, FederationDatasetQueryResult}
import org.simplemodeling.textus.scraper.api.TextusScraperApi

/*
 * @since   Jul. 21, 2026
 *  version Jul. 23, 2026
 * @version Aug. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokFederationPublicationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK knowledge-source replacement" should {
    "publish provider-neutral generations" which {
      "encode literal assertion object types in the generated SIE request" in {
        Given("one normalized BoK generation with terms and component references")
        val context = _context(_first_resources)
        val normalized = BokSourceReader.read(context, _source("generation-1")).TAKE

        When("the generation is converted to the provider-neutral replacement contract")
        val replacement = BokFederationPublisher.replacementRequest(normalized).TAKE
        val externalassertions = replacement.toRecord().getVector("assertions").get.collect {
          case assertion: Record => assertion
        }

        Then("typed and external assertions both retain literal object types")
        replacement.assertions.flatMap(_.objectType).map(_.value).distinct shouldBe Vector("literal")
        externalassertions.flatMap(_.getAny("objectType")).map(_.toString).distinct shouldBe
          Vector("literal")
      }

      "replace one complete generation idempotently without duplicate records" in {
        Given("BoK and generated-contract SIE components with one metadata-only generation")
        val assembly = _assembly(includeSie = true)
        val context = _context(_first_resources)
        val source = _source("generation-1")

        When("the generation and its identical retry are replaced through the BoK action")
        val first = _replace(assembly.bok, context, source)
        val repeated = _replace(assembly.bok, context, source)
        val inspection = _inspect(assembly.sie.get, context, source)

        Then("SIE reports one complete dataset with stable record counts")
        first.getString("status") shouldBe Some("complete")
        first.getInt("termCount") shouldBe Some(2)
        first.getInt("componentCount") shouldBe Some(2)
        repeated.getString("status") shouldBe Some("complete")
        inspection.getString("state") shouldBe Some("complete")
        inspection.getString("generation") shouldBe Some("generation-1")
        inspection.getInt("documentCount") shouldBe Some(4)
        inspection.getInt("assertionCount") shouldBe Some(4)
        inspection.getInt("evidenceCount") shouldBe Some(3)
      }

      "remove stale records when a later complete generation replaces the first" in {
        Given("a first complete generation already published through the BoK action")
        val assembly = _assembly(includeSie = true)
        val firstcontext = _context(_first_resources)
        _replace(assembly.bok, firstcontext, _source("generation-1"))
        val secondcontext = _context(_second_resources)
        val secondsource = _source("generation-2")

        When("the later generation contains one term and no component references")
        val second = _replace(assembly.bok, secondcontext, secondsource)
        val inspection = _inspect(assembly.sie.get, secondcontext, secondsource)

        Then("the generic replacement removes every stale document assertion and evidence")
        second.getString("status") shouldBe Some("complete")
        second.getInt("termCount") shouldBe Some(1)
        second.getInt("componentCount") shouldBe Some(0)
        inspection.getString("generation") shouldBe Some("generation-2")
        inspection.getInt("documentCount") shouldBe Some(1)
        inspection.getInt("assertionCount") shouldBe Some(1)
        inspection.getInt("evidenceCount") shouldBe Some(1)
        inspection.getString("rdfStatus") shouldBe Some("ready")
        inspection.getString("vectorStatus") shouldBe Some("ready")
      }
    }

    "project admitted knowledge" which {
      "return factual Knowledge Map topology without semantic retrieval" in {
        Given("one complete generation published into the BoK catalog")
        val assembly = _assembly(includeSie = true)
        val context = _context(_first_resources)
        val source = _source("generation-1")
        _replace(assembly.bok, context, source)

        When("the public Knowledge Map operation projects the selected catalog topology")
        val map = _knowledge_map(assembly.bok, context, source)

        Then("the typed result contains the admitted factual nodes and relationships")
        map.getString("status") shouldBe Some("matched")
        map.getVector("sources").get should have size 1
        map.getVector("nodes").get.collect { case node: Record => node.getString("nodeId") } shouldBe
          Vector(Some("term:runtime"), Some("article:runtime"))
        map.getVector("nodes").get.collect { case node: Record =>
          node.getVector("terms").getOrElse(Vector.empty).collect { case term: Record => term.getString("definition") }
        } shouldBe Vector(Vector(Some("Runtime environment.")), Vector.empty)
        map.getVector("relationships").get.collect { case relationship: Record => relationship.getString("predicate") } shouldBe
          Vector(Some("references"))
      }

      "retain dataset and source attribution for semantic term candidates" in {
        Given("one complete generation published through BoK and the generic SIE contract")
        val assembly = _assembly(includeSie = true)
        val context = _context(_first_resources)
        val source = _source("generation-1")
        _replace(assembly.bok, context, source)
        val normalized = BokSourceReader.read(context, source).TAKE
        val runtime = normalized.terms.find(_.title.value == "Runtime").get

        When("a semantic term candidate is requested through the generated federation API")
        val federationresults = _query_federation(assembly.bok, context, "runtime environment", "simplemodeling")
        val candidate = _search_terms(assembly.bok, context, "runtime environment", 1)
        val runtimecandidate = federationresults.find(
          _.documentId.value == BokFederationPublisher.termDocumentId(runtime)
        ).get
        val runtimekey = BokCandidateKey(
          source.datasetId.value,
          source.sourceId.value,
          runtimecandidate.documentId.value
        )
        val candidatescores = _candidate_scores(
          assembly.bok,
          context,
          "runtime environment",
          runtimekey
        )

        Then("the BoK candidate remains source-scoped after generic SIE retrieval")
        runtimecandidate.score should be >= 0.2
        runtimecandidate.metadata.get.value should include ("\"datasetId\":\"simplemodeling-bok\"")
        federationresults.map(_.sourceId.value) should contain ("simplemodeling")
        federationresults should not be empty
        federationresults.exists(_.metadata.exists(_.value.contains("\"domain\":\"bok\""))) shouldBe true
        candidatescores.keySet should contain (runtimekey)
        candidate.getString("status") shouldBe Some("matched")
        val matched = candidate.getVector("results").get.collect { case x: Record => x }
        matched should have size 1
        matched.head.getString("matchKind") shouldBe Some("candidate")
      }
    }

    "enforce component and private-binding boundaries" which {
      "fail without bypassing the component boundary when SIE is absent" in {
        Given("a BoK component whose subsystem has no SemanticIntegrationEngine component")
        val assembly = _assembly(includeSie = false)

        When("knowledge-source replacement is requested")
        val result = _replace_c(assembly.bok, _context(_first_resources), _source("generation-1"))

        Then("the operation returns a structured service failure instead of using a provider directly")
        result should matchPattern { case Consequence.Failure(_) => }
      }

      "load and admit a private profile binding through production component wiring" in {
        Given("an official subsystem binding and an administrative request carrying a different raw resource")
        val configuredsource = _source("generation-1")
        val requestedsource = configuredsource.copy(
          resource = BokResourceReference("urn:textus:bok:request-must-not-be-read")
        )
        val assembly = _assembly(
          includeSie = true,
          configuration = _profile_configuration(configuredsource)
        )
        val context = _context(_first_resources)

        When("the existing non-MCP replacement action publishes the matching configured identity")
        val response = _replace(assembly.bok, context, requestedsource)
        val resolved = assembly.bok.asInstanceOf[BokPrimaryComponent].resolveProfile(
          BokProfileSelection.official,
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(BokProfileKey.official)
        ).TAKE

        Then("component bootstrap supplies the configured resource and admits only the complete SIE generation")
        response.getString("status") shouldBe Some("complete")
        response.getString("generation") shouldBe Some("generation-1")
        resolved.resolvedProfile shouldBe "official"
        resolved.datasetId shouldBe configuredsource.datasetId
        resolved.sourceId shouldBe configuredsource.sourceId
        resolved.generation shouldBe configuredsource.generation
      }
    }
  }

  private def _assembly(
    includeSie: Boolean,
    configuration: ResolvedConfiguration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
  ): Assembly = {
    val subsystem = RuntimeBindingAdmissionFixture.admit(
      new Subsystem(
        name = "textus-bok-federation-spec",
        configuration = configuration
      )
    )
    val params = ComponentCreate(subsystem, ComponentOrigin.Main)
    val bok = new impl.ComponentFactory().create(params).primary
    val scraper = Option.when(includeSie)(
      new ScraperProviderFactory().createPrimary(params)
    )
    val sie = Option.when(includeSie)(
      new org.simplemodeling.textus.semanticintegration.impl.ComponentFactory().create(params).primary
    )
    subsystem.add(scraper.toVector ++ sie.toVector :+ bok)
    if (includeSie) {
      given ExecutionContext = ExecutionContext.create()
      val resolution = SpiResolver.resolveAssembly(subsystem.components.toVector).TAKE
      subsystem.withComponentApiResolver(resolution.componentApiResolver)
    }
    Assembly(bok, sie)
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

  private def _knowledge_map(
    bok: Component,
    context: ExecutionContext,
    source: BokKnowledgeSource
  ): Record = {
    val action = BokComponent.BokRetrievalService.GetKnowledgeMapRequest.unsafeForTest(
      null,
      Record.dataAuto(
        "datasetId" -> source.datasetId.value,
        "sourceId" -> source.sourceId.value,
        "focus" -> "runtime"
      )
    )
    _record(action.createCall(ActionCall.Core(action, context, Some(bok), None)).execute().TAKE)
  }

  private def _query_federation(
    bok: Component,
    context: ExecutionContext,
    query: String,
    sourceid: String
  ): Vector[FederationDatasetQueryResult] = {
    val component = bok.asInstanceOf[BokPrimaryComponent]
    val api = component.semanticIntegrationFederation()(using context).TAKE
    val request = FederationDatasetQueryRequest.createC(
      Record.dataAuto(
        "text" -> query,
        "sourceId" -> sourceid,
        "limit" -> 10
      )
    ).TAKE
    api.queryDataset(request)(using context).TAKE.results
  }

  private def _candidate_scores(
    bok: Component,
    context: ExecutionContext,
    query: String,
    candidate: BokCandidateKey
  ): Map[BokCandidateKey, Double] = {
    val action = BokComponent.BokRetrievalService.SearchTermsRequest.unsafeForTest(
      null,
      Record.dataAuto("query" -> query, "limit" -> 1)
    )
    val core = ActionCall.Core(action, context, Some(bok), None)
    BokFederationRetriever.candidateScores(core, query, Set(candidate), 1).TAKE
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

  private def _profile_configuration(source: BokKnowledgeSource): ResolvedConfiguration = {
    val sourcevalue = ConfigurationValue.ObjectValue(Map(
      "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value),
      "datasetId" -> ConfigurationValue.StringValue(source.datasetId.value),
      "generation" -> ConfigurationValue.StringValue(source.generation.value),
      "resource" -> ConfigurationValue.StringValue(source.resource.value)
    ))
    val evidence = BokEvidence(
      BokEvidenceUri("https://evidence.example/simplemodeling"),
      source.sourceId,
      None,
      None,
      None
    )
    val evidencevalue = ConfigurationValue.ObjectValue(Map(
      "uri" -> ConfigurationValue.StringValue(evidence.uri.value),
      "sourceId" -> ConfigurationValue.StringValue(evidence.sourceId.value)
    ))
    val binding = ConfigurationValue.ObjectValue(Map(
      "profile" -> ConfigurationValue.StringValue("official"),
      "source" -> sourcevalue,
      "evidence" -> evidencevalue
    ))
    val registry = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(List(binding))
    ))
    val nested = ConfigurationValue.ObjectValue(Map(
      "bok" -> ConfigurationValue.ObjectValue(Map(
        "profile-registry" -> registry
      ))
    ))
    ResolvedConfiguration(
      Configuration(Map("textus" -> nested)),
      ConfigurationTrace.empty
    )
  }

  private val _manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-repository-index", "href": "repository/catalog/index.json"},
      |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
      |  ]
      |}""".stripMargin

  private val _first_manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "component-repository-index", "href": "repository/catalog/index.json"},
      |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"},
      |    {"kind": "rdf-graph-summary", "href": "metadata/rdf/graph.json"}
      |  ]
      |}""".stripMargin

  private val _first_resources = Map(
    "fixture/metadata/cncf/knowledge-source.json" -> _first_manifest,
    "fixture/metadata/glossary/terms.json" ->
      """{"terms":[
        |{"id":"architecture:runtime","title":"Runtime","definition_text":"Runtime environment.","term_type":"concept"},
        |{"id":"architecture:component","title":"Component","definition_text":"Reusable component.","term_type":"concept"}
        |]}""".stripMargin,
    "fixture/repository/catalog/index.json" ->
      """{"schemaVersion":"cncf.component-repository-index.v2","generatedAt":"2026-07-21T00:00:00Z","artifacts":[
        |{"kind":"car","namespace":"org.simplemodeling.textus","id":"Account","artifactId":"textus-account","catalog":"car/org/simplemodeling/textus/textus-account.yaml","status":"active","recommended":"0.2.0"},
        |{"kind":"sar","artifactId":"textus-runtime","catalog":"sar/textus-runtime.yaml","status":"active","latestSnapshot":"1.0.0-SNAPSHOT"}
        |]}""".stripMargin,
    "fixture/metadata/rdf/graph.json" ->
      """{
        |  "schemaVersion": "cozy.rdf-graph-summary.v1",
        |  "kind": "rdf-graph-summary",
        |  "sourceRef": {"kind": "bok-site", "value": "knowledgehub", "uri": "https://example.test/knowledgehub"},
        |  "nodes": [
        |    {"id": "term:runtime", "label": "Runtime", "node_type": "term", "category": "architecture", "terms": ["architecture:runtime"]},
        |    {"id": "article:runtime", "label": "Runtime Article", "node_type": "article"}
        |  ],
        |  "edges": [
        |    {"source": "term:runtime", "predicate": "references", "target": "article:runtime", "label": "References"}
        |  ],
        |  "truncated": false
        |}""".stripMargin
  )

  private val _second_resources = Map(
    "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
    "fixture/metadata/glossary/terms.json" ->
      """{"terms":[
        |{"id":"architecture:component","title":"Component","definition_text":"Updated reusable component.","term_type":"concept"}
        |]}""".stripMargin,
    "fixture/repository/catalog/index.json" ->
      """{"schemaVersion":"cncf.component-repository-index.v2","generatedAt":"2026-07-21T01:00:00Z","artifacts":[]}"""
  )

  private final case class Assembly(
    bok: Component,
    sie: Option[Component]
  )
}

private final class ScraperProviderComponent extends Component {
  override def componentApiProviders: Vector[org.goldenport.cncf.spi.SpiProvider[?]] =
    Vector(TextusScraperApi.Provider)
}

private final class ScraperProviderFactory extends Component.Factory {
  protected def create_Component(params: ComponentCreate): Component =
    new ScraperProviderComponent

  protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core = {
    val componentid = ComponentId("org.simplemodeling.textus.bok.fixture.ScraperProvider")
    Component.Core.create(
      componentid.name,
      componentid,
      ComponentInstanceId.default(componentid),
      Protocol.empty,
      this
    )
  }
}
