package org.simplemodeling.textus.bok

import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.mcp.McpToolCatalog
import org.goldenport.cncf.subsystem.DefaultSubsystemFactory
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.impl

/*
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
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
      val outputs = component.operationDefinitions.map(x => x.name -> x.outputType).toMap

      Then("all four BoK reads are discoverable while source replacement is absent")
      names shouldBe Set(
        "Bok.BokRetrieval.searchTerms",
        "Bok.BokRetrieval.explainTerm",
        "Bok.BokRetrieval.searchComponentReferences",
        "Bok.BokRetrieval.getComponentReference"
      )
      names.exists(_.endsWith(".replaceKnowledgeSource")) shouldBe false

      And("the tool inputs and operation outputs retain their typed BoK contracts")
      searchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "category",
        "limit"
      )
      searchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      explainschema.hcursor.downField("properties").keys.get.toSet shouldBe Set("term")
      explainschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("term"))
      componentsearchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "kind",
        "limit"
      )
      componentsearchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      componentlookupschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "name",
        "version",
        "kind"
      )
      componentlookupschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("name"))
      outputs.get("searchTerms") shouldBe Some("SearchTermsResponse")
      outputs.get("explainTerm") shouldBe Some("ExplainTermResponse")
      outputs.get("searchComponentReferences") shouldBe Some("ComponentReferenceSearchResponse")
      outputs.get("getComponentReference") shouldBe Some("ComponentReferenceLookupResponse")
    }
  }
}
