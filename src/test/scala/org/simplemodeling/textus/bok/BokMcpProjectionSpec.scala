package org.simplemodeling.textus.bok

import io.circe.Json
import io.circe.parser.parse
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.mcp.{McpJsonRpcAdapter, McpProtocolRevision, McpToolCatalog}
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.cncf.subsystem.{DefaultSubsystemFactory, Subsystem}
import org.goldenport.cncf.testutil.RuntimeBindingAdmissionFixture
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.impl
import org.simplemodeling.textus.bok.value.BokKnowledgeSource

/*
 * @since   Jul. 21, 2026
 *  version Jul. 23, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokMcpProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK read MCP projection" should {
    "publish typed terminology, bounded Knowledge Map, and existence-only component-reference tools" in {
      Given("an initialized BoK component with operation-level MCP readiness")
      val subsystem = DefaultSubsystemFactory.default(Some("server"))
      val component = new impl.ComponentFactory().create(
        ComponentCreate(subsystem, ComponentOrigin.Repository("bok-mcp-spec"))
      ).primary

      When("CNCF projects the component MCP catalog")
      val tools = McpToolCatalog.toolsForComponent(component)
      val names = tools.map(_.name).toSet
      val searchschema = tools.find(_.name.endsWith(".searchTerms")).map(_.inputSchema).get
      val explainschema = tools.find(_.name.endsWith(".explainTerm")).map(_.inputSchema).get
      val componentsearchschema = tools.find(_.name.endsWith(".searchComponentReferences")).map(_.inputSchema).get
      val componentlookupschema = tools.find(_.name.endsWith(".getComponentReference")).map(_.inputSchema).get
      val mapschema = tools.find(_.name.endsWith(".getKnowledgeMap")).map(_.inputSchema).get
      val semanticsearchschema = tools.find(_.name.endsWith(".searchSemanticKnowledge")).map(_.inputSchema).get
      val manifestchema = tools.find(_.name.endsWith(".getSemanticManifest")).map(_.inputSchema).get
      val resourceschema = tools.find(_.name.endsWith(".getSemanticResource")).map(_.inputSchema).get
      val sectionschema = tools.find(_.name.endsWith(".getSemanticSection")).map(_.inputSchema).get
      val discoverschema = tools.find(_.name.endsWith(".discoverSemanticKnowledge")).map(_.inputSchema).get
      val outputs = component.operationDefinitions.map(x => x.name -> x.outputType).toMap

      Then("exactly ten bounded read tools are discoverable while source replacement is absent")
      names shouldBe Set(
        "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms",
        "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm",
        "org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences",
        "org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference",
        "org.simplemodeling.textus.Bok.BokRetrieval.getKnowledgeMap",
        "org.simplemodeling.textus.Bok.BokRetrieval.searchSemanticKnowledge",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticManifest",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticResource",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticSection",
        "org.simplemodeling.textus.Bok.BokRetrieval.discoverSemanticKnowledge"
      )
      names.exists(_.endsWith(".replaceKnowledgeSource")) shouldBe false
      names.exists(_.endsWith(".getKnowledgeMap")) shouldBe true

      And("the tool inputs and operation outputs retain their typed BoK contracts")
      searchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "category",
        "limit",
        "profile",
        "projectId",
        "catalogId"
      )
      searchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      explainschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "term",
        "profile",
        "projectId",
        "catalogId"
      )
      explainschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("term"))
      componentsearchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "kind",
        "limit",
        "profile",
        "projectId",
        "catalogId"
      )
      componentsearchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      componentlookupschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "name",
        "version",
        "kind",
        "organization",
        "profile",
        "projectId",
        "catalogId"
      )
      componentlookupschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("name"))
      mapschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "datasetId",
        "sourceId",
        "category",
        "termType",
        "focus",
        "nodeLimit",
        "relationshipLimit",
        "profile",
        "projectId",
        "catalogId"
      )
      mapschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector.empty)
      semanticsearchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "limit",
        "profile",
        "projectId",
        "catalogId"
      )
      semanticsearchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      manifestchema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "identity",
        "profile",
        "projectId",
        "catalogId"
      )
      manifestchema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("identity"))
      resourceschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "identity",
        "profile",
        "projectId",
        "catalogId"
      )
      resourceschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("identity"))
      sectionschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "documentId",
        "sectionId",
        "profile",
        "projectId",
        "catalogId"
      )
      sectionschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("documentId", "sectionId"))
      discoverschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "limit",
        "profile",
        "projectId",
        "catalogId"
      )
      discoverschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))

      And("the closed logical selector remains optional on every MCP-ready read")
      Vector(
        searchschema,
        explainschema,
        componentsearchschema,
        componentlookupschema,
        mapschema,
        semanticsearchschema,
        manifestchema,
        resourceschema,
        sectionschema,
        discoverschema
      ).foreach { schema =>
        schema.hcursor.downField("properties").downField("profile").get[String]("type") shouldBe Right("string")
        schema.hcursor.downField("properties").downField("projectId").get[String]("type") shouldBe Right("string")
        schema.hcursor.downField("properties").downField("catalogId").get[String]("type") shouldBe Right("string")
      }
      outputs.get("searchTerms") shouldBe Some("SearchTermsResponse")
      outputs.get("explainTerm") shouldBe Some("ExplainTermResponse")
      outputs.get("searchComponentReferences") shouldBe Some("ComponentReferenceSearchResponse")
      outputs.get("getComponentReference") shouldBe Some("ComponentReferenceLookupResponse")
      outputs.get("getKnowledgeMap") shouldBe Some("GetKnowledgeMapResponse")
      outputs.get("searchSemanticKnowledge") shouldBe Some("SemanticKnowledgeSearchResponse")
      outputs.get("getSemanticManifest") shouldBe Some("SemanticManifestResponse")
      outputs.get("getSemanticResource") shouldBe Some("SemanticResourceResponse")
      outputs.get("getSemanticSection") shouldBe Some("SemanticSectionResponse")
      outputs.get("discoverSemanticKnowledge") shouldBe Some("SemanticKnowledgeDiscoveryResponse")
    }

    "read a selected resident catalog through MCP" which {
      "return the same official terminology and attribution as the ordinary searchTerms operation" in {
        Given("one configured official source, an in-memory resource access, and a server subsystem containing only BoK")
        val source = _official_source
        val subsystem = RuntimeBindingAdmissionFixture.admit(
          new Subsystem(
            name = "textus-bok-mcp-resident-catalog-spec",
            configuration = _official_profile_configuration(source)
          )
        )
        val bundle = new impl.ComponentFactory().create(
          ComponentCreate(subsystem, ComponentOrigin.Repository("bok-mcp-resident-catalog-spec"))
        )
        val component = bundle.primary
        val context = _official_context
        subsystem.add(bundle)

        When("the existing non-MCP replacement boundary admits the complete official generation and both read boundaries search it")
        val replacement = _replace(component, context, source)
        val ordinary = _search_terms(component, context, "P10 MCP Marker")
        val mcpresponse = _mcp_search_terms(new McpJsonRpcAdapter(subsystem), "P10 MCP Marker")

        Then("the resident catalog has one complete official selection without a SemanticIntegrationEngine component")
        replacement.getString("status") shouldBe Some("complete")
        replacement.getString("sourceId") shouldBe Some(source.sourceId.value)
        replacement.getString("datasetId") shouldBe Some(source.datasetId.value)
        replacement.getString("generation") shouldBe Some(source.generation.value)
        subsystem.components.toVector shouldBe Vector(component)

        And("ordinary searchTerms and MCP tools/call resolve the same full selection and source evidence")
        mcpresponse.hcursor.downField("error").focus shouldBe None
        mcpresponse.hcursor.downField("result").downField("isError").focus shouldBe None
        val mcptext = mcpresponse.hcursor.downField("result").downField("content").downN(0).get[String]("text")
          .getOrElse(fail("MCP search outcome has no JSON text content"))
        val mcp = parse(mcptext).fold(error => fail(s"MCP search outcome has malformed JSON: ${error.getMessage}"), identity)
        val ordinaryselection = ordinary.getRecord("selection").getOrElse(fail("Ordinary search selection is missing"))
        val ordinaryevidence = ordinaryselection.getRecord("evidence").getOrElse(fail("Ordinary selection evidence is missing"))
        val ordinarymatches = ordinary.getVector("results").getOrElse(Vector.empty).collect { case value: Record => value }
        ordinarymatches should have size 1
        val ordinarymatch = ordinarymatches.head
        val ordinaryterm = ordinarymatch.getRecord("term").getOrElse(fail("Ordinary terminology match is missing"))
        val ordinarytermevidence = ordinaryterm.getRecord("evidence").getOrElse(fail("Ordinary terminology evidence is missing"))
        val mcpdata = mcp.hcursor
        val mcpselection = mcpdata.downField("selection")
        val mcpmatch = mcpdata.downField("results").downN(0)
        val mcpterm = mcpmatch.downField("term")

        ordinary.getString("status") shouldBe Some("matched")
        mcpdata.get[String]("status") shouldBe Right(ordinary.getString("status").get)
        mcpselection.get[String]("resolved_profile") shouldBe Right(ordinaryselection.getString("resolvedProfile").get)
        mcpselection.get[Option[String]]("project_id") shouldBe Right(ordinaryselection.getString("projectId"))
        mcpselection.get[String]("dataset_id") shouldBe Right(ordinaryselection.getString("datasetId").get)
        mcpselection.get[String]("source_id") shouldBe Right(ordinaryselection.getString("sourceId").get)
        mcpselection.get[String]("generation") shouldBe Right(ordinaryselection.getString("generation").get)
        mcpselection.downField("evidence").get[String]("uri") shouldBe Right(ordinaryevidence.getString("uri").get)
        mcpselection.downField("evidence").get[String]("source_id") shouldBe Right(ordinaryevidence.getString("sourceId").get)
        mcpmatch.get[String]("match_kind") shouldBe Right(ordinarymatch.getString("matchKind").get)
        mcpmatch.get[Double]("score") shouldBe Right(ordinarymatch.getDouble("score").get)
        mcpterm.get[String]("term_id") shouldBe Right(ordinaryterm.getString("termId").get)
        mcpterm.get[String]("title") shouldBe Right(ordinaryterm.getString("title").get)
        mcpterm.get[String]("definition") shouldBe Right(ordinaryterm.getString("definition").get)
        mcpterm.get[String]("dataset_id") shouldBe Right(ordinaryterm.getString("datasetId").get)
        mcpterm.downField("evidence").get[String]("uri") shouldBe Right(ordinarytermevidence.getString("uri").get)
        mcpterm.downField("evidence").get[String]("source_id") shouldBe Right(ordinarytermevidence.getString("sourceId").get)
      }

      "read two complete direct generations through one source-qualified documentation catalog without publishing mutation" in {
        Given("a development and KnowledgeHub profile bound to separate in-memory generated sources")
        val developmentsource = _catalog_source(
          "p10-catalog-simplemodeling",
          "p10-catalog-simplemodeling-dataset",
          "p10-catalog-simplemodeling-generation",
          "p10-catalog-simplemodeling"
        )
        val knowledgehubsource = _catalog_source(
          "p10-catalog-knowledgehub",
          "p10-catalog-knowledgehub-dataset",
          "p10-catalog-knowledgehub-generation",
          "p10-catalog-knowledgehub"
        )
        val subsystem = RuntimeBindingAdmissionFixture.admit(
          new Subsystem(
            name = "textus-bok-mcp-two-source-catalog-spec",
            configuration = _two_source_catalog_configuration(developmentsource, knowledgehubsource)
          )
        )
        val bundle = new impl.ComponentFactory().create(
          ComponentCreate(subsystem, ComponentOrigin.Repository("bok-mcp-two-source-catalog-spec"))
        )
        val component = bundle.primary
        val context = _two_source_catalog_context
        subsystem.add(bundle)

        When("the protected replacement boundary admits both direct generations and MCP reads select documentation")
        val developmentreplacement = _replace(component, context, developmentsource)
        val knowledgehubreplacement = _replace(component, context, knowledgehubsource)
        val adapter = new McpJsonRpcAdapter(subsystem)
        val tools = McpToolCatalog.toolsForComponent(component).map(_.name).toSet
        val story = _mcp_explain_term(adapter, "p10-catalog:story")
        val embedding = _mcp_explain_term(adapter, "p10-catalog:embedding")

        Then("both exact terms retain their direct source attribution while the catalog selection exposes both complete members")
        developmentreplacement.getString("status") shouldBe Some("complete")
        knowledgehubreplacement.getString("status") shouldBe Some("complete")
        _catalog_selection_members(story) shouldBe Vector(
          (
            "p10-catalog-knowledgehub",
            "p10-catalog-knowledgehub-dataset",
            "p10-catalog-knowledgehub-generation",
            "https://evidence.example/p10-catalog-knowledgehub",
            "p10-catalog-knowledgehub"
          ),
          (
            "p10-catalog-simplemodeling",
            "p10-catalog-simplemodeling-dataset",
            "p10-catalog-simplemodeling-generation",
            "https://evidence.example/p10-catalog-simplemodeling",
            "p10-catalog-simplemodeling"
          )
        )
        _catalog_selection_members(embedding) shouldBe _catalog_selection_members(story)
        story.hcursor.downField("result").downField("term").get[String]("term_id") shouldBe Right("p10-catalog:story")
        story.hcursor.downField("result").downField("term").get[String]("dataset_id") shouldBe Right(developmentsource.datasetId.value)
        story.hcursor.downField("result").downField("term").downField("evidence").get[String]("source_id") shouldBe Right(developmentsource.sourceId.value)
        embedding.hcursor.downField("result").downField("term").get[String]("term_id") shouldBe Right("p10-catalog:embedding")
        embedding.hcursor.downField("result").downField("term").get[String]("dataset_id") shouldBe Right(knowledgehubsource.datasetId.value)
        embedding.hcursor.downField("result").downField("term").downField("evidence").get[String]("source_id") shouldBe Right(knowledgehubsource.sourceId.value)

        And("MCP keeps the ten declared reads and excludes protected source replacement")
        tools should have size 10
        tools should not contain "org.simplemodeling.textus.Bok.BokRetrieval.replaceKnowledgeSource"
      }
    }

    "allow deployment policy to narrow reads without publishing source mutation" in {
      Given("a BoK component with hostile enable keys and one read disabled")
      val subsystem = DefaultSubsystemFactory.default(Some("server"))
      val bundle = new impl.ComponentFactory().create(
        ComponentCreate(subsystem, ComponentOrigin.Repository("bok-mcp-policy-spec"))
      )
      val component = bundle.primary
      component.withApplicationConfig(Component.ApplicationConfig(config = Some(Configuration(Map(
        "cncf.mcp.enabled" -> ConfigurationValue.StringValue("true"),
        "cncf.mcp.enabled-services" -> ConfigurationValue.StringValue("BokRetrieval"),
        "cncf.mcp.enabled-operations" -> ConfigurationValue.StringValue(
          "BokRetrieval.replaceKnowledgeSource"
        ),
        "cncf.mcp.disabled-operations" -> ConfigurationValue.StringValue(
          "BokRetrieval.searchTerms"
        )
      )))))
      subsystem.add(bundle)
      val adapter = new McpJsonRpcAdapter(subsystem)

      When("the configured catalog is listed and source replacement is called")
      val listed = adapter.handle(
        """{"jsonrpc":"2.0","id":"policy-list","method":"tools/list","params":{}}""",
        Some(McpProtocolRevision.PREFERRED.print)
      ).responseBody.getOrElse(fail("MCP list outcome has no JSON response body"))
      val narrowednames = listed.hcursor.downField("result").downField("tools").focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)
        .flatMap(_.hcursor.get[String]("name").toOption)
        .filter(_.startsWith("org.simplemodeling.textus.Bok.BokRetrieval."))
        .toSet
      val called = adapter.handle(
        """{"jsonrpc":"2.0","id":"mutation-call","method":"tools/call","params":{"name":"org.simplemodeling.textus.Bok.BokRetrieval.replaceKnowledgeSource","arguments":{}}}""",
        Some(McpProtocolRevision.PREFERRED.print)
      ).responseBody.getOrElse(fail("MCP call outcome has no JSON response body"))

      Then("configuration removes declared reads but cannot add source mutation")
      narrowednames shouldBe Set(
        "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm",
        "org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences",
        "org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference",
        "org.simplemodeling.textus.Bok.BokRetrieval.getKnowledgeMap",
        "org.simplemodeling.textus.Bok.BokRetrieval.searchSemanticKnowledge",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticManifest",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticResource",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticSection",
        "org.simplemodeling.textus.Bok.BokRetrieval.discoverSemanticKnowledge"
      )
      called.hcursor.downField("error").get[Int]("code") shouldBe Right(-32602)

      When("service and global disable policies are applied")
      component.withApplicationConfig(Component.ApplicationConfig(config = Some(Configuration(Map(
        "cncf.mcp.disabled-services" -> ConfigurationValue.StringValue("BokRetrieval")
      )))))
      val servicedisabled = McpToolCatalog.toolsForComponent(component)
      component.withApplicationConfig(Component.ApplicationConfig(config = Some(Configuration(Map(
        "cncf.mcp.enabled" -> ConfigurationValue.StringValue("false")
      )))))
      val globallydisabled = McpToolCatalog.toolsForComponent(component)

      Then("both policies can only reduce the published catalog")
      servicedisabled shouldBe empty
      globallydisabled shouldBe empty
    }
  }

  private def _official_source: BokKnowledgeSource =
    BokKnowledgeSource(
      BokSourceId("p10-mcp-source"),
      BokDatasetId("p10-mcp-dataset"),
      BokSourceGeneration("p10-mcp-generation"),
      BokResourceReference("urn:textus:bok:p10-mcp")
    )

  private def _official_context: ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", Map(
      "p10-mcp/metadata/cncf/knowledge-source.json" ->
        """{
          |  "schemaVersion": "cncf.knowledge-source.v1",
          |  "resources": [
          |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
          |  ]
          |}""".stripMargin,
      "p10-mcp/metadata/glossary/terms.json" ->
        """{
          |  "terms": [
          |    {
          |      "id": "phase-10:mcp-marker",
          |      "title": "P10 MCP Marker",
          |      "definition_text": "A selected in-memory official terminology fixture.",
          |      "term_type": "concept"
          |    }
          |  ]
          |}""".stripMargin
    ))
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _official_profile_configuration(source: BokKnowledgeSource): ResolvedConfiguration = {
    val binding = ConfigurationValue.ObjectValue(Map(
      "profile" -> ConfigurationValue.StringValue("official"),
      "source" -> ConfigurationValue.ObjectValue(Map(
        "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value),
        "datasetId" -> ConfigurationValue.StringValue(source.datasetId.value),
        "generation" -> ConfigurationValue.StringValue(source.generation.value),
        "resource" -> ConfigurationValue.StringValue(source.resource.value)
      )),
      "evidence" -> ConfigurationValue.ObjectValue(Map(
        "uri" -> ConfigurationValue.StringValue("https://evidence.example/p10-mcp-source"),
        "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value)
      ))
    ))
    val registry = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(List(binding))
    ))
    val textus = ConfigurationValue.ObjectValue(Map(
      "bok" -> ConfigurationValue.ObjectValue(Map("profile-registry" -> registry))
    ))
    ResolvedConfiguration(Configuration(Map("textus" -> textus)), ConfigurationTrace.empty)
  }

  private def _catalog_source(
    sourceid: String,
    datasetid: String,
    generation: String,
    resourceid: String
  ): BokKnowledgeSource =
    BokKnowledgeSource(
      BokSourceId(sourceid),
      BokDatasetId(datasetid),
      BokSourceGeneration(generation),
      BokResourceReference(s"urn:textus:bok:$resourceid")
    )

  private def _two_source_catalog_context: ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", Map(
      "p10-catalog-simplemodeling/metadata/cncf/knowledge-source.json" ->
        """{
          |  "schemaVersion": "cncf.knowledge-source.v1",
          |  "resources": [
          |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
          |  ]
          |}""".stripMargin,
      "p10-catalog-simplemodeling/metadata/glossary/terms.json" ->
        """{
          |  "terms": [
          |    {
          |      "id": "p10-catalog:story",
          |      "title": "P10 Catalog Story",
          |      "definition_text": "A source-qualified SimpleModeling catalog term.",
          |      "term_type": "concept"
          |    }
          |  ]
          |}""".stripMargin,
      "p10-catalog-knowledgehub/metadata/cncf/knowledge-source.json" ->
        """{
          |  "schemaVersion": "cncf.knowledge-source.v1",
          |  "resources": [
          |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
          |  ]
          |}""".stripMargin,
      "p10-catalog-knowledgehub/metadata/glossary/terms.json" ->
        """{
          |  "terms": [
          |    {
          |      "id": "p10-catalog:embedding",
          |      "title": "P10 Catalog Embedding",
          |      "definition_text": "A source-qualified KnowledgeHub catalog term.",
          |      "term_type": "technology"
          |    }
          |  ]
          |}""".stripMargin
    ))
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _two_source_catalog_configuration(
    developmentsource: BokKnowledgeSource,
    knowledgehubsource: BokKnowledgeSource
  ): ResolvedConfiguration = {
    def _binding_(profile: String, projectid: Option[String], source: BokKnowledgeSource): ConfigurationValue = {
      val project = projectid.map(value => Map("projectId" -> ConfigurationValue.StringValue(value))).getOrElse(Map.empty)
      ConfigurationValue.ObjectValue(Map(
        "profile" -> ConfigurationValue.StringValue(profile),
        "source" -> ConfigurationValue.ObjectValue(Map(
          "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value),
          "datasetId" -> ConfigurationValue.StringValue(source.datasetId.value),
          "generation" -> ConfigurationValue.StringValue(source.generation.value),
          "resource" -> ConfigurationValue.StringValue(source.resource.value)
        )),
        "evidence" -> ConfigurationValue.ObjectValue(Map(
          "uri" -> ConfigurationValue.StringValue(s"https://evidence.example/${source.sourceId.value}"),
          "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value)
        ))
      ) ++ project)
    }
    val registry = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(List(
        _binding_("development", None, developmentsource),
        _binding_("project", Some("knowledgehub"), knowledgehubsource)
      )),
      "catalogs" -> ConfigurationValue.ListValue(List(ConfigurationValue.ObjectValue(Map(
        "catalogId" -> ConfigurationValue.StringValue("documentation"),
        "members" -> ConfigurationValue.ListValue(List(
          ConfigurationValue.StringValue("development"),
          ConfigurationValue.StringValue("project:knowledgehub")
        ))
      ))))
    ))
    val textus = ConfigurationValue.ObjectValue(Map(
      "bok" -> ConfigurationValue.ObjectValue(Map(
        "profile-registry" -> registry,
        "knowledge-backend" -> ConfigurationValue.ObjectValue(Map(
          "backend" -> ConfigurationValue.StringValue("memory")
        ))
      ))
    ))
    ResolvedConfiguration(Configuration(Map("textus" -> textus)), ConfigurationTrace.empty)
  }

  private def _replace(
    component: Component,
    context: ExecutionContext,
    source: BokKnowledgeSource
  ): Record = {
    val action = BokComponent.BokRetrievalService.ReplaceKnowledgeSourceRequest.unsafeForTest(
      null,
      Record.dataAuto("source" -> source.toRecord())
    )
    _record(action.createCall(ActionCall.Core(action, context, Some(component), None)).execute().TAKE)
  }

  private def _search_terms(
    component: Component,
    context: ExecutionContext,
    query: String
  ): Record = {
    val action = BokComponent.BokRetrievalService.SearchTermsRequest.unsafeForTest(
      null,
      Record.dataAuto("query" -> query, "limit" -> 1)
    )
    _record(action.createCall(ActionCall.Core(action, context, Some(component), None)).execute().TAKE)
  }

  private def _mcp_search_terms(adapter: McpJsonRpcAdapter, query: String): Json = {
    adapter.handle(
      s"""{"jsonrpc":"2.0","id":"resident-catalog-search","method":"tools/call","params":{"name":"org.simplemodeling.textus.Bok.BokRetrieval.searchTerms","arguments":{"query":"$query","limit":1}}}""",
      Some(McpProtocolRevision.PREFERRED.print)
    ).responseBody.getOrElse(fail("MCP search outcome has no JSON response body"))
  }

  private def _mcp_explain_term(adapter: McpJsonRpcAdapter, term: String): Json = {
    val response = adapter.handle(
      s"""{"jsonrpc":"2.0","id":"catalog-$term","method":"tools/call","params":{"name":"org.simplemodeling.textus.Bok.BokRetrieval.explainTerm","arguments":{"term":"$term","profile":"catalog","catalogId":"documentation"}}}""",
      Some(McpProtocolRevision.PREFERRED.print)
    ).responseBody.getOrElse(fail("MCP catalog explanation has no JSON response body"))
    val text = response.hcursor.downField("result").downField("content").downN(0).get[String]("text")
      .getOrElse(fail("MCP catalog explanation has no JSON text content"))
    parse(text).fold(error => fail(s"MCP catalog explanation has malformed JSON: ${error.getMessage}"), identity)
  }

  private def _catalog_selection_members(response: Json): Vector[(String, String, String, String, String)] = {
    val selection = response.hcursor.downField("selection")
    selection.downField("datasetId").focus shouldBe None
    selection.downField("dataset_id").focus shouldBe None
    selection.downField("sourceId").focus shouldBe None
    selection.downField("source_id").focus shouldBe None
    selection.downField("generation").focus shouldBe None
    selection.downField("evidence").focus shouldBe None
    selection.downField("members").focus.flatMap(_.asArray).getOrElse(Vector.empty).map { member =>
      val cursor = member.hcursor
      val evidence = cursor.downField("evidence")
      (
        cursor.get[String]("source_id").getOrElse(fail("Catalog member has no source_id")),
        cursor.get[String]("dataset_id").getOrElse(fail("Catalog member has no dataset_id")),
        cursor.get[String]("generation").getOrElse(fail("Catalog member has no generation")),
        evidence.get[String]("uri").getOrElse(fail("Catalog member has no evidence uri")),
        evidence.get[String]("source_id").getOrElse(fail("Catalog member has no evidence source_id"))
      )
    }
  }

  private def _record(response: OperationResponse): Record =
    response match {
      case OperationResponse.RecordResponse(record) => record
      case other => fail(s"Expected RecordResponse but got ${other.getClass.getName}")
    }
}
