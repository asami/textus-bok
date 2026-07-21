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
  "BoK terminology MCP projection" should {
    "publish only typed term search and explanation tools" in {
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
      val outputs = component.operationDefinitions.map(x => x.name -> x.outputType).toMap

      Then("only the two terminology reads are discoverable")
      names shouldBe Set(
        "Bok.BokRetrieval.searchTerms",
        "Bok.BokRetrieval.explainTerm"
      )
      names.exists(_.endsWith(".replaceKnowledgeSource")) shouldBe false
      names.exists(_.endsWith(".searchComponentReferences")) shouldBe false
      names.exists(_.endsWith(".getComponentReference")) shouldBe false

      And("the tool inputs and operation outputs retain their typed BoK contracts")
      searchschema.hcursor.downField("properties").keys.get.toSet shouldBe Set(
        "query",
        "category",
        "limit"
      )
      searchschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("query"))
      explainschema.hcursor.downField("properties").keys.get.toSet shouldBe Set("term")
      explainschema.hcursor.get[Vector[String]]("required") shouldBe Right(Vector("term"))
      outputs.get("searchTerms") shouldBe Some("SearchTermsResponse")
      outputs.get("explainTerm") shouldBe Some("ExplainTermResponse")
    }
  }
}
