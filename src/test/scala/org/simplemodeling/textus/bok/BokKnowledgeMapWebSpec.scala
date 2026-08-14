package org.simplemodeling.textus.bok

import java.nio.file.Files
import java.nio.file.Path

import org.goldenport.cncf.http.{WebApplicationEntryPolicy, WebDescriptor}
import org.goldenport.cncf.subsystem.GenericSubsystemDescriptor
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 23, 2026
 *  version Jul. 24, 2026
 * @version Aug. 14, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokKnowledgeMapWebSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK Knowledge Map Web application" should {
    "package an authorized read-only Static Form map with a complete no-JavaScript fallback" in {
      Given("the authored form descriptor and map page")
      val form = _read("src/main/web-inf/form.yaml")
      val page = _read("src/main/web/textus-bok/map.html")
      val overview = _read("src/main/web/textus-bok/index.html")

      When("the declared Web boundary is inspected")

      Then("the one public bounded operation supplies filters, graph data, and accessible tables")
      form should include("bok.bok-retrieval.get-knowledge-map: public")
      form should include("bok.bok-retrieval.get-knowledge-map:\n    enabled: true")
      page should include("<textus:operation-result component=\"bok\" service=\"bok-retrieval\" operation=\"get-knowledge-map\">")
      page should include("method=\"get\"")
      page should include("action=\"/web/bok/textus-bok/map\"")
      page should include("/web/bok/textus-bok/assets/knowledge-map.js")
      overview should include("/web/bok/textus-bok/map")
      page should include("id=\"bok-knowledge-map-data\"")
      page should include("id=\"bok-knowledge-map-summary\"")
      page should not include "result.body.nodes.size"
      page should not include "result.body.relationships.size"
      page should include("component_references:CBD handoff")
      page should include("This complete table remains available when JavaScript is disabled.")
      page should include("aria-live=\"polite\"")
      overview should include("existence-only identities")

      And("the authored pages expose no mutation control")
      Vector(page, overview).mkString should not include "replace-knowledge-source"
    }

    "use local progressive enhancement without unsafe HTML sinks or non-HTTP evidence navigation" in {
      Given("the authored local JavaScript and CSS assets")
      val script = _read("src/main/web/textus-bok/assets/knowledge-map.js")
      val css = _read("src/main/web/textus-bok/assets/knowledge-map.css")

      When("their rendering and navigation boundary is inspected")

      Then("the graph is keyboard reachable, data stays local, and evidence links are scheme-gated")
      script should include("document.createElementNS")
      script should include("tabindex")
      script should include("event.key === \"Enter\"")
      script should include("textContent")
      script should include("node_id")
      script should include("component_references")
      script should include("Nodes: ${nodes.length} · Relationships: ${relationships.length}")
      script should include("url.protocol === \"https:\" || url.protocol === \"http:\"")
      script should include("noopener noreferrer")
      script should not include "fetch("
      script should not include "innerHTML"
      css should include("@media")
      css should include(".bok-skip-link:focus")
    }

    "declare textus-bok as the configuration-free sole public application entry" in {
      Given("the authored Web descriptor source")

      When("the shared entry policy resolves the sole application for canonical component segment bok")
      val webdescriptor = WebDescriptor.load(Path.of("src", "main", "web-inf", "web.yaml")).fold(_fail_conclusion, identity)
      val entry = WebApplicationEntryPolicy.resolve(
        webdescriptor,
        Some(GenericSubsystemDescriptor(Path.of("textus-bok.sar"), "textus-bok", implicitRootComponentName = Some("bok")))
      )

      Then("the descriptor needs no entry flag while /web is the public entry and the canonical route remains configured")
      webdescriptor.componentEntryApps shouldBe Vector.empty
      entry shouldBe WebApplicationEntryPolicy.Selected(webdescriptor.apps.head, "bok", "/web")
      webdescriptor.apps.head.completedFor(Some("bok")).effectiveRoute shouldBe "/web/bok/textus-bok"
    }
  }

  private def _read(path: String): String =
    Files.readString(Path.of(path))

  private def _fail_conclusion(error: org.goldenport.Conclusion): Nothing = fail(error.toString)
}
