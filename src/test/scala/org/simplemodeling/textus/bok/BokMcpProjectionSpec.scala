package org.simplemodeling.textus.bok

import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.mcp.{McpJsonRpcAdapter, McpProtocolRevision, McpToolCatalog}
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.goldenport.configuration.{Configuration, ConfigurationValue}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.impl

/*
 * @since   Jul. 21, 2026
 *  version Jul. 23, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokMcpProjectionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK read MCP projection" should {
    "publish typed terminology and existence-only component-reference tools" in {
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
      val semanticsearchschema = tools.find(_.name.endsWith(".searchSemanticKnowledge")).map(_.inputSchema).get
      val manifestchema = tools.find(_.name.endsWith(".getSemanticManifest")).map(_.inputSchema).get
      val resourceschema = tools.find(_.name.endsWith(".getSemanticResource")).map(_.inputSchema).get
      val sectionschema = tools.find(_.name.endsWith(".getSemanticSection")).map(_.inputSchema).get
      val discoverschema = tools.find(_.name.endsWith(".discoverSemanticKnowledge")).map(_.inputSchema).get
      val outputs = component.operationDefinitions.map(x => x.name -> x.outputType).toMap

      Then("all four existing and exactly five new semantic reads are discoverable while source replacement is absent")
      names shouldBe Set(
        "org.simplemodeling.textus.Bok.BokRetrieval.searchTerms",
        "org.simplemodeling.textus.Bok.BokRetrieval.explainTerm",
        "org.simplemodeling.textus.Bok.BokRetrieval.searchComponentReferences",
        "org.simplemodeling.textus.Bok.BokRetrieval.getComponentReference",
        "org.simplemodeling.textus.Bok.BokRetrieval.searchSemanticKnowledge",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticManifest",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticResource",
        "org.simplemodeling.textus.Bok.BokRetrieval.getSemanticSection",
        "org.simplemodeling.textus.Bok.BokRetrieval.discoverSemanticKnowledge"
      )
      names.exists(_.endsWith(".replaceKnowledgeSource")) shouldBe false
      names.exists(_.endsWith(".getKnowledgeMap")) shouldBe false

      And("the tool inputs and operation outputs retain their typed BoK contracts")
      searchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "category",
        "limit",
        "profile",
        "projectId"
      )
      searchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      explainschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "term",
        "profile",
        "projectId"
      )
      explainschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("term"))
      componentsearchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "kind",
        "limit",
        "profile",
        "projectId"
      )
      componentsearchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      componentlookupschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "name",
        "version",
        "kind",
        "organization",
        "profile",
        "projectId"
      )
      componentlookupschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("name"))
      semanticsearchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "limit",
        "profile",
        "projectId"
      )
      semanticsearchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      manifestchema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "identity",
        "profile",
        "projectId"
      )
      manifestchema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("identity"))
      resourceschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "identity",
        "profile",
        "projectId"
      )
      resourceschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("identity"))
      sectionschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "documentId",
        "sectionId",
        "profile",
        "projectId"
      )
      sectionschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("documentId", "sectionId"))
      discoverschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "limit",
        "profile",
        "projectId"
      )
      discoverschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))

      And("the closed logical selector remains optional on every MCP-ready read")
      Vector(
        searchschema,
        explainschema,
        componentsearchschema,
        componentlookupschema,
        semanticsearchschema,
        manifestchema,
        resourceschema,
        sectionschema,
        discoverschema
      ).foreach { schema =>
        schema.hcursor.downField("properties").downField("profile").get[String]("type") shouldBe Right("string")
        schema.hcursor.downField("properties").downField("projectId").get[String]("type") shouldBe Right("string")
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
}
