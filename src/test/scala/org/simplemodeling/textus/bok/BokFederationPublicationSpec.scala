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
import org.simplemodeling.textus.bok.runtime.{BokCandidateKey, BokFederationPublisher, BokFederationRetriever, BokProfileAuthorization, BokProfileCompatibilityFilter, BokProfileKey, BokProfileResolutionFailure, BokProfileSelection, BokSourceReader}
import org.simplemodeling.textus.bok.value.{BokEvidence, BokKnowledgeSource}
import org.simplemodeling.textus.semanticintegration.SemanticIntegrationEngineComponent.KnowledgeFederationService
import org.simplemodeling.textus.semanticintegration.value.{FederationDatasetQueryRequest, FederationDatasetQueryResult}
import org.simplemodeling.textus.scraper.api.TextusScraperApi

/*
 * @since   Jul. 21, 2026
 *  version Jul. 23, 2026
 * @version Aug. 15, 2026
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
        val assembly = _assembly(includesie = true)
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

      "publish same-artifact CAR references from different organizations as distinct identities" in {
        Given("one valid generation containing the same CAR artifact in two namespaces")
        val resources = _first_resources.updated(
          "fixture/repository/catalog/index.json",
          """{"schemaVersion":"cncf.component-repository-index.v2","generatedAt":"2026-07-21T00:00:00Z","artifacts":[
            |{"kind":"car","namespace":"com.simplemodeling.textus","id":"Account","artifactId":"textus-account","catalog":"car/com/simplemodeling/textus/textus-account.yaml","status":"active","recommended":"0.2.0"},
            |{"kind":"car","namespace":"org.simplemodeling.textus","id":"Account","artifactId":"textus-account","catalog":"car/org/simplemodeling/textus/textus-account.yaml","status":"active","recommended":"0.2.0"},
            |{"kind":"sar","artifactId":"textus-runtime","catalog":"sar/textus-runtime.yaml","status":"active","latestSnapshot":"1.0.0-SNAPSHOT"}
            |]}""".stripMargin
        )
        val assembly = _assembly(includesie = true)
        val context = _context(resources)
        val source = _source("generation-qualified")
        val normalized = BokSourceReader.read(context, source).TAKE
        val replacement = BokFederationPublisher.replacementRequest(normalized).TAKE
        val qualified = normalized.components.filter(_.kind.value == "car")
        val qualifieddocumentids = qualified.map(BokFederationPublisher.componentDocumentId)
        val qualifiedassertions = replacement.assertions.filter(_.predicate.value == "urn:textus:bok:predicate:kind")
        val qualifiedassertionids = qualifiedassertions.map(_.id)
        val qualifiedsubjects = qualifiedassertions.map(_.subject.value)

        When("the qualified generation is replaced through SIE")
        val response = _replace(assembly.bok, context, source)
        val inspection = _inspect(assembly.sie.get, context, source)

        Then("document and assertion IDs and subjects remain distinct and both records are inspected")
        qualifieddocumentids.distinct should have size 2
        qualifiedassertionids.distinct should have size 3
        qualifiedsubjects.distinct should have size 3
        response.getString("status") shouldBe Some("complete")
        inspection.getInt("documentCount") shouldBe Some(5)
        inspection.getInt("assertionCount") shouldBe Some(5)
      }

      "remove stale records when a later complete generation replaces the first" in {
        Given("a first complete generation already published through the BoK action")
        val assembly = _assembly(includesie = true)
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
        val source = _source("generation-1")
        val assembly = _assembly(includesie = true, configuration = _profile_configuration(source))
        val context = _context(_first_resources)
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
        val source = _source("generation-1")
        val assembly = _assembly(includesie = true, configuration = _profile_configuration(source))
        val context = _context(_first_resources)
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

    "integrate one resolved profile into public reads" which {
      "default to official, isolate explicit development and project-alpha and project-beta reads, and retain full attribution" in {
        Given("private official, development, project-alpha, and project-beta bindings with distinct admitted generations")
        val fixture = _profile_read_fixture()

        When("positive profile requests and cyclic foreign-marker requests are dispatched")
        val omitted = _search_terms(fixture.assembly.bok, fixture.context, "Official Profile Marker", 1)
        val developmentread = _search_terms(
          fixture.assembly.bok,
          fixture.context,
          "Development Profile Marker",
          1,
          Some("development")
        )
        val projectalpharead = _search_terms(
          fixture.assembly.bok,
          fixture.context,
          "Project Alpha Marker",
          1,
          Some("project"),
          Some("project-alpha")
        )
        val projectbetaread = _search_terms(
          fixture.assembly.bok,
          fixture.context,
          "Project Beta Marker",
          1,
          Some("project"),
          Some("project-beta")
        )
        val officialforeign = _search_terms(fixture.assembly.bok, fixture.context, "Development", 1)
        val developmentforeign = _search_terms(
          fixture.assembly.bok,
          fixture.context,
          "Alpha",
          1,
          Some("development")
        )
        val projectalphaforeign = _search_terms(
          fixture.assembly.bok,
          fixture.context,
          "Beta",
          1,
          Some("project"),
          Some("project-alpha")
        )
        val projectbetaforeign = _search_terms(
          fixture.assembly.bok,
          fixture.context,
          "Official",
          1,
          Some("project"),
          Some("project-beta")
        )

        Then("each request exposes only its resolved profile generation and record evidence")
        val officialterm = _term_record(omitted)
        val developmentterm = _term_record(developmentread)
        val projectalphaterm = _term_record(projectalpharead)
        val projectbetaterm = _term_record(projectbetaread)
        val officialselection = omitted.getRecord("selection").getOrElse(fail("official selection is missing"))
        val developmentselection = developmentread.getRecord("selection").getOrElse(fail("development selection is missing"))
        val projectalphaselection = projectalpharead.getRecord("selection").getOrElse(fail("project-alpha selection is missing"))
        val projectbetaselection = projectbetaread.getRecord("selection").getOrElse(fail("project-beta selection is missing"))
        omitted.getString("status") shouldBe Some("matched")
        developmentread.getString("status") shouldBe Some("matched")
        projectalpharead.getString("status") shouldBe Some("matched")
        projectbetaread.getString("status") shouldBe Some("matched")
        Vector(omitted, developmentread, projectalpharead, projectbetaread).foreach { response =>
          response.getVector("results").getOrElse(Vector.empty) should have size 1
        }
        officialselection.getString("resolvedProfile") shouldBe Some("official")
        officialselection.getString("datasetId") shouldBe Some("official-dataset")
        officialselection.getString("sourceId") shouldBe Some("official-source")
        officialselection.getString("generation") shouldBe Some("official-g1")
        officialselection.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("official-source")
        officialterm.getString("termId") shouldBe Some("official-marker")
        officialterm.getString("title") shouldBe Some("Official Profile Marker")
        officialterm.getString("definition") shouldBe Some("Official representative generation.")
        officialterm.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("official-source")
        developmentselection.getString("resolvedProfile") shouldBe Some("development")
        developmentselection.getString("datasetId") shouldBe Some("development-dataset")
        developmentselection.getString("sourceId") shouldBe Some("development-source")
        developmentselection.getString("generation") shouldBe Some("development-g1")
        developmentselection.getString("projectId") shouldBe None
        developmentterm.getString("termId") shouldBe Some("development-marker")
        developmentterm.getString("title") shouldBe Some("Development Profile Marker")
        developmentterm.getString("definition") shouldBe Some("Development representative generation.")
        developmentterm.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("development-source")
        projectalphaselection.getString("resolvedProfile") shouldBe Some("project")
        projectalphaselection.getString("projectId") shouldBe Some("project-alpha")
        projectalphaselection.getString("datasetId") shouldBe Some("project-alpha-dataset")
        projectalphaselection.getString("sourceId") shouldBe Some("project-alpha-source")
        projectalphaselection.getString("generation") shouldBe Some("project-alpha-g1")
        projectalphaterm.getString("termId") shouldBe Some("project-alpha-marker")
        projectalphaterm.getString("title") shouldBe Some("Project Alpha Marker")
        projectalphaterm.getString("definition") shouldBe Some("Project Alpha representative generation.")
        projectalphaterm.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("project-alpha-source")
        projectbetaselection.getString("resolvedProfile") shouldBe Some("project")
        projectbetaselection.getString("projectId") shouldBe Some("project-beta")
        projectbetaselection.getString("datasetId") shouldBe Some("project-beta-dataset")
        projectbetaselection.getString("sourceId") shouldBe Some("project-beta-source")
        projectbetaselection.getString("generation") shouldBe Some("project-beta-g1")
        projectbetaterm.getString("termId") shouldBe Some("project-beta-marker")
        projectbetaterm.getString("title") shouldBe Some("Project Beta Marker")
        projectbetaterm.getString("definition") shouldBe Some("Project Beta representative generation.")
        projectbetaterm.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("project-beta-source")
        developmentselection.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("development-source")
        projectalphaselection.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("project-alpha-source")
        projectbetaselection.getRecord("evidence").flatMap(_.getString("sourceId")) shouldBe Some("project-beta-source")
        _assert_no_match_terms(officialforeign, "official", None, fixture.official)
        _assert_no_match_terms(developmentforeign, "development", None, fixture.development)
        _assert_no_match_terms(projectalphaforeign, "project", Some("project-alpha"), fixture.projectalpha)
        _assert_no_match_terms(projectbetaforeign, "project", Some("project-beta"), fixture.projectbeta)
      }

      "accept agreeing Knowledge Map filters for the resolved generation and reject conflicting legacy filters" in {
        Given("private official, development, project-alpha, and project-beta bindings with distinct admitted generations")
        val fixture = _profile_read_fixture()

        When("an agreeing project map, a foreign focus, and conflicting legacy filters are dispatched")
        val projectmap = _knowledge_map(
          fixture.assembly.bok,
          fixture.context,
          fixture.projectalpha,
          Some("project"),
          Some("project-alpha"),
          focus = None
        )
        val foreignfocusmap = _knowledge_map(
          fixture.assembly.bok,
          fixture.context,
          fixture.projectalpha,
          Some("project"),
          Some("project-alpha"),
          focus = Some("project-beta-node")
        )
        val conflicting = _knowledge_map_c(
          fixture.assembly.bok,
          fixture.context,
          Some(fixture.development.datasetId.value),
          Some(fixture.development.sourceId.value),
          None,
          None,
          None
        )

        Then("the agreeing map returns only the project generation and foreign or conflicting requests have no fallback response")
        val mapselection = projectmap.getRecord("selection").getOrElse(fail("Knowledge Map selection is missing"))
        projectmap.getString("status") shouldBe Some("matched")
        mapselection.getString("resolvedProfile") shouldBe Some("project")
        mapselection.getString("projectId") shouldBe Some("project-alpha")
        mapselection.getString("datasetId") shouldBe Some("project-alpha-dataset")
        mapselection.getString("sourceId") shouldBe Some("project-alpha-source")
        mapselection.getString("generation") shouldBe Some("project-alpha-g1")
        projectmap.getVector("sources").getOrElse(Vector.empty).collect {
          case source: Record => source.getString("sourceId")
        } shouldBe Vector(Some("project-alpha-source"))
        _failure_code(conflicting) shouldBe Some(BokProfileResolutionFailure.ConflictingSelection)
        _assert_no_match_map(foreignfocusmap, "project", Some("project-alpha"), fixture.projectalpha)
      }

      "return project-identity-required for an incomplete project selector" in {
        Given("a BoK component without an inferred project binding")
        val assembly = _assembly(includesie = false)
        val context = _context(_first_resources)

        When("a terminology read selects project without projectId")
        val result = _search_terms_c(
          assembly.bok,
          context,
          "Runtime",
          1,
          Some("project"),
          None
        )

        Then("the request fails with its stable structured selection code")
        _failure_code(result) shouldBe Some(BokProfileResolutionFailure.ProjectIdentityRequired)
      }
    }

    "enforce component and private-binding boundaries" which {
      "fail without bypassing the component boundary when SIE is absent" in {
        Given("a BoK component whose subsystem has no SemanticIntegrationEngine component")
        val assembly = _assembly(includesie = false)

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
          includesie = true,
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
    includesie: Boolean,
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
    val scraper = Option.when(includesie)(
      new ScraperProviderFactory().createPrimary(params)
    )
    val sie = Option.when(includesie)(
      new org.simplemodeling.textus.semanticintegration.impl.ComponentFactory().create(params).primary
    )
    subsystem.add(scraper.toVector ++ sie.toVector :+ bok)
    if (includesie) {
      given ExecutionContext = ExecutionContext.create()
      val resolution = SpiResolver.resolveAssembly(subsystem.components.toVector).TAKE
      subsystem.withComponentApiResolver(resolution.componentApiResolver)
    }
    Assembly(bok, sie)
  }

  private def _profile_read_fixture(): ProfileReadFixture = {
    val official = _profile_source("official", "official-g1", "official-source", "official-dataset")
    val development = _profile_source("development", "development-g1", "development-source", "development-dataset")
    val projectalpha = _profile_source("project-alpha", "project-alpha-g1", "project-alpha-source", "project-alpha-dataset")
    val projectbeta = _profile_source("project-beta", "project-beta-g1", "project-beta-source", "project-beta-dataset")
    val assembly = _assembly(
      includesie = true,
      configuration = _profile_configuration(Vector(
        ("official", None, official),
        ("development", None, development),
        ("project", Some("project-alpha"), projectalpha),
        ("project", Some("project-beta"), projectbeta)
      ))
    )
    val context = _context(_profile_resources)
    _replace(assembly.bok, context, official)
    _replace(assembly.bok, context, development)
    _replace(assembly.bok, context, projectalpha)
    _replace(assembly.bok, context, projectbeta)
    ProfileReadFixture(assembly, context, official, development, projectalpha, projectbeta)
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
    limit: Int,
    profile: Option[String] = None,
    projectid: Option[String] = None
  ): Record =
    _record(_search_terms_c(bok, context, query, limit, profile, projectid).TAKE)

  private def _search_terms_c(
    bok: Component,
    context: ExecutionContext,
    query: String,
    limit: Int,
    profile: Option[String],
    projectid: Option[String]
  ): Consequence[OperationResponse] = {
    val action = BokComponent.BokRetrievalService.SearchTermsRequest.unsafeForTest(
      null,
      Record.dataAuto(
        "query" -> query,
        "limit" -> limit,
        "profile" -> profile,
        "projectId" -> projectid
      )
    )
    action.createCall(ActionCall.Core(action, context, Some(bok), None)).execute()
  }

  private def _knowledge_map(
    bok: Component,
    context: ExecutionContext,
    source: BokKnowledgeSource,
    profile: Option[String] = None,
    projectid: Option[String] = None,
    focus: Option[String] = Some("runtime")
  ): Record =
    _record(_knowledge_map_c(
      bok,
      context,
      Some(source.datasetId.value),
      Some(source.sourceId.value),
      focus,
      profile,
      projectid
    ).TAKE)

  private def _knowledge_map_c(
    bok: Component,
    context: ExecutionContext,
    datasetid: Option[String],
    sourceid: Option[String],
    focus: Option[String],
    profile: Option[String],
    projectid: Option[String]
  ): Consequence[OperationResponse] = {
    val action = BokComponent.BokRetrievalService.GetKnowledgeMapRequest.unsafeForTest(
      null,
      Record.dataAuto(
        "datasetId" -> datasetid,
        "sourceId" -> sourceid,
        "focus" -> focus,
        "profile" -> profile,
        "projectId" -> projectid
      )
    )
    action.createCall(ActionCall.Core(action, context, Some(bok), None)).execute()
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

  private def _term_record(response: Record): Record = {
    val terms = response.getVector("results").getOrElse(Vector.empty).collect {
      case result: Record => result.getRecord("term").getOrElse(result)
    }
    terms should have size 1
    terms.head
  }

  private def _assert_no_match_terms(
    response: Record,
    resolvedprofile: String,
    projectid: Option[String],
    source: BokKnowledgeSource
  ): Unit = {
    response.getString("status") shouldBe Some("no-match")
    response.getVector("results").getOrElse(Vector.empty) should have size 0
    val selection = response.getRecord("selection").getOrElse(fail("No-match selection is missing"))
    selection.getString("resolvedProfile") shouldBe Some(resolvedprofile)
    selection.getString("projectId") shouldBe projectid
    selection.getString("datasetId") shouldBe Some(source.datasetId.value)
    selection.getString("sourceId") shouldBe Some(source.sourceId.value)
    selection.getString("generation") shouldBe Some(source.generation.value)
  }

  private def _assert_no_match_map(
    response: Record,
    resolvedprofile: String,
    projectid: Option[String],
    source: BokKnowledgeSource
  ): Unit = {
    response.getString("status") shouldBe Some("no-match")
    response.getVector("nodes").getOrElse(Vector.empty) should have size 0
    response.getVector("relationships").getOrElse(Vector.empty) should have size 0
    val selection = response.getRecord("selection").getOrElse(fail("No-match Knowledge Map selection is missing"))
    selection.getString("resolvedProfile") shouldBe Some(resolvedprofile)
    selection.getString("projectId") shouldBe projectid
    selection.getString("datasetId") shouldBe Some(source.datasetId.value)
    selection.getString("sourceId") shouldBe Some(source.sourceId.value)
    selection.getString("generation") shouldBe Some(source.generation.value)
    val sources = response.getVector("sources").getOrElse(Vector.empty).collect { case value: Record => value }
    sources should have size 1
    sources.head.getString("datasetId") shouldBe Some(source.datasetId.value)
    sources.head.getString("sourceId") shouldBe Some(source.sourceId.value)
    sources.head.getString("generation") shouldBe Some(source.generation.value)
  }

  private def _failure_code(
    result: Consequence[?]
  ): Option[BokProfileResolutionFailure] =
    result match {
      case Consequence.Failure(conclusion) => BokProfileResolutionFailure.from(conclusion)
      case Consequence.Success(_) => None
    }

  private def _context(contents: Map[String, String]): ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _source(
    generation: String,
    sourceid: String = "simplemodeling",
    datasetid: String = "simplemodeling-bok",
    resourceid: String = "fixture"
  ): BokKnowledgeSource =
    BokKnowledgeSource(
      BokSourceId(sourceid),
      BokDatasetId(datasetid),
      BokSourceGeneration(generation),
      BokResourceReference(s"urn:textus:bok:$resourceid")
    )

  private def _profile_source(
    profileid: String,
    generation: String,
    sourceid: String,
    datasetid: String
  ): BokKnowledgeSource =
    _source(generation, sourceid, datasetid, profileid)

  private def _profile_configuration(source: BokKnowledgeSource): ResolvedConfiguration =
    _profile_configuration(Vector(("official", None, source)))

  private def _profile_configuration(
    bindings: Vector[(String, Option[String], BokKnowledgeSource)]
  ): ResolvedConfiguration = {
    val configured = bindings.map { case (profile, projectid, source) =>
      val sourcevalue = ConfigurationValue.ObjectValue(Map(
        "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value),
        "datasetId" -> ConfigurationValue.StringValue(source.datasetId.value),
        "generation" -> ConfigurationValue.StringValue(source.generation.value),
        "resource" -> ConfigurationValue.StringValue(source.resource.value)
      ))
      val evidence = BokEvidence(
        BokEvidenceUri(s"https://evidence.example/${source.sourceId.value}"),
        source.sourceId,
        None,
        None,
        None
      )
      val evidencevalue = ConfigurationValue.ObjectValue(Map(
        "uri" -> ConfigurationValue.StringValue(evidence.uri.value),
        "sourceId" -> ConfigurationValue.StringValue(evidence.sourceId.value)
      ))
      ConfigurationValue.ObjectValue(
        Map(
          "profile" -> ConfigurationValue.StringValue(profile),
          "source" -> sourcevalue,
          "evidence" -> evidencevalue
        ) ++ projectid.map(value => "projectId" -> ConfigurationValue.StringValue(value))
      )
    }
    val registry = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(configured.toList)
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

  private val _profile_manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind":"glossary-terms","href":"metadata/glossary/terms.json"},
      |    {"kind":"rdf-graph-summary","href":"metadata/rdf/graph.json"}
      |  ]
      |}""".stripMargin

  private def _profile_resources: Map[String, String] =
    Vector(
      ("official", "official-marker", "Official Profile Marker", "Official representative generation.", "official-source"),
      ("development", "development-marker", "Development Profile Marker", "Development representative generation.", "development-source"),
      ("project-alpha", "project-alpha-marker", "Project Alpha Marker", "Project Alpha representative generation.", "project-alpha-source"),
      ("project-beta", "project-beta-marker", "Project Beta Marker", "Project Beta representative generation.", "project-beta-source")
    ).flatMap { case (profileid, termid, marker, definition, sourceid) =>
      Vector(
        s"$profileid/metadata/cncf/knowledge-source.json" -> _profile_manifest,
        s"$profileid/metadata/glossary/terms.json" -> _profile_terms(termid, marker, definition),
        s"$profileid/metadata/rdf/graph.json" -> _profile_graph(profileid, termid, marker, sourceid)
      )
    }.toMap

  private def _profile_terms(
    termid: String,
    marker: String,
    definition: String
  ): String =
    s"""{"terms":[{"id":"$termid","title":"$marker","definition_text":"$definition","category":"architecture","term_type":"concept"}]}"""

  private def _profile_graph(
    profileid: String,
    termid: String,
    marker: String,
    sourceid: String
  ): String =
    s"""{
       |  "schemaVersion":"cozy.rdf-graph-summary.v1",
       |  "kind":"rdf-graph-summary",
       |  "sourceRef":{"kind":"bok-site","value":"${sourceid}-ref","uri":"https://evidence.example/$sourceid/source"},
       |  "nodes":[{"id":"${profileid}-node","label":"$marker","node_type":"term","category":"architecture","terms":["$termid"]}],
       |  "edges":[],
       |  "truncated":false
       |}""".stripMargin

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

  private final case class ProfileReadFixture(
    assembly: Assembly,
    context: ExecutionContext,
    official: BokKnowledgeSource,
    development: BokKnowledgeSource,
    projectalpha: BokKnowledgeSource,
    projectbeta: BokKnowledgeSource
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
