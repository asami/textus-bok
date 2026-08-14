package org.simplemodeling.textus.bok

import java.nio.file.Files
import java.nio.file.Path

import org.goldenport.cncf.http.{StaticFormAppRenderer, WebApplicationEntryPolicy, WebDescriptor}
import org.goldenport.cncf.subsystem.GenericSubsystemDescriptor
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Jul. 23, 2026
 *  version Jul. 24, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokKnowledgeMapWebSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "BoK Knowledge Map Web application" should {
    "provide a complete Static Form and no-JavaScript result" which {
      "package an authorized read-only Static Form map with a complete no-JavaScript fallback" in {
      Given("the authored form descriptor and map page")
      val form = _read("src/main/web-inf/form.yaml")
      val page = _read("src/main/web/textus-bok/map.html")
      val overview = _read("src/main/web/textus-bok/index.html")

      When("the declared Web boundary is inspected")

      Then("the one public bounded operation supplies filters, graph data, and accessible tables")
      form should include("bok.bok-retrieval.get-knowledge-map: public")
      form should include("bok.bok-retrieval.get-knowledge-map:\n    enabled: true")
      form should include("bok.bok-retrieval.replace-knowledge-source: protected")
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
      page should include("name=\"profile\"")
      page should include("<option value=\"official\">Official (default)</option>")
      page should include("<option value=\"development\">Development</option>")
      page should include("<option value=\"project\">Project</option>")
      page should include("name=\"projectId\"")
      page should include("data-profile-dependent=\"project\"")
      page should include("<noscript>")
      form should include("values: [official, development, project]")
      form should include("defaultValue: official")
      form should include("stayOnError: true")
      page should include("<textus-error-panel source=\"error\"></textus-error-panel>")
      page should include("${result.body.selection.resolved_profile}")
      page should include("${result.body.selection.project_id}")
      page should include("${result.body.selection.dataset_id}")
      page should include("${result.body.selection.source_id}")
      page should include("${result.body.selection.generation}")
      page should include("${result.body.selection.evidence.uri}")
      page should include("source=\"result.body.sources\"")
      overview should include("existence-only identities")

      And("the authored pages expose no mutation control")
      Vector(page, overview).mkString should not include "replace-knowledge-source"
      }
    }

    "use safe progressive enhancement and evidence navigation" which {
      "use local progressive enhancement without unsafe HTML sinks or non-HTTP evidence navigation" in {
      Given("the authored local JavaScript and CSS assets")
      val script = _read("src/main/web/textus-bok/assets/knowledge-map.js")
      val css = _read("src/main/web/textus-bok/assets/knowledge-map.css")
      val page = _read("src/main/web/textus-bok/map.html")

      When("their rendering and navigation boundary is inspected")

      Then("the graph is keyboard reachable, data stays local, and evidence links are scheme-gated")
      script should include("document.createElementNS")
      script should include("tabindex")
      script should include("event.key === \"Enter\"")
      script should include("textContent")
      script should include("node_id")
      script should include("component_references")
      script should include("URLSearchParams")
      script should include("window.location.search")
      script should include("const hasprofile = query.has(\"profile\");")
      script should include("const hasprojectid = query.has(\"projectId\");")
      script should include("let conflictingselection = hasprojectid && requestedprofile !== \"project\";")
      script should include("let initialinvalidorconflicting = invalidprofile || conflictingselection;")
      script should include("profile.value = \"\";")
      script should include("profile.selectedIndex = -1;")
      script should include("profile.value = hasprofile ? requestedprofile : \"official\";")
      script should include("projectid.value = hasprojectid ? requestedprojectid : \"\";")
      script should not include "requestedprofile || \"official\""
      script should not include "query.get(\"projectId\") || \"\""
      script should include("projectid.disabled = selectedprofile !== \"project\"")
      script should include("projectid.required = selectedprofile === \"project\"")
      script should include("requestedprojectid")
      script should include("conflicts with this profile; the operation will report a structured failure.")
      script should include("if (profile.value !== \"project\") projectid.value = \"\";")
      script should include("let invalidprofile = hasprofile && !knownprofiles.includes(requestedprofile);")
      script should include("form.addEventListener(\"submit\", (event) => {")
      script should include("if (initialinvalidorconflicting) event.preventDefault();")
      script should include("initialinvalidorconflicting = false;")
      script should include("conflictingselection = false;")
      script should include("projectid.addEventListener(\"input\", () => {")
      script should include("data-node-kind")
      script should include("Nodes: ${nodes.length} · Relationships: ${relationships.length}")
      script should include("url.protocol === \"https:\" || url.protocol === \"http:\"")
      script should include("noopener noreferrer")
      script should not include "fetch("
      script should not include "innerHTML"
      script should not include "localStorage"
      script should not include "sessionStorage"
      script should not include "history."
      script should not include "XMLHttpRequest"
      script should not include "resourceRoot"
      script should not include "credential"
      page should not include "metadata/rdf/graph.json"
      css should include("@media")
      css should include(".bok-skip-link:focus")
      css should include(".bok-map-node-kind")
      page should include("method=\"get\"")
      page should include("action=\"/web/bok/textus-bok/map\"")
      }
    }

    "render the authored Static Form result safely" which {
      "render a successful project result with attribution and complete tables" in {
        Given("the authored map result template and a successful project response containing hostile text")
        val page = _read("src/main/web/textus-bok/map.html")
        val template = _map_result_template(page)
        val body = """{
          |  "status": "matched",
          |  "selection": {
          |    "resolvedProfile": "project",
          |    "projectId": "project-alpha",
          |    "datasetId": "dataset-<alpha>",
          |    "sourceId": "source-&-alpha",
          |    "generation": "generation-1",
          |    "evidence": {"uri": "https://example.test/evidence?q=1&label=<x>", "sourceId": "source-&-alpha"}
          |  },
          |  "sources": [{"datasetId": "dataset-<alpha>", "sourceId": "source-&-alpha", "generation": "generation-1", "sourceReference": {"kind": "bok-site", "value": "site-<alpha>", "uri": "https://example.test/source"}, "sourceTruncated": false, "warnings": []}],
          |  "nodes": [{"datasetId": "dataset-<alpha>", "sourceId": "source-&-alpha", "nodeId": "node-1", "label": "<script>alert('x')</script>", "kind": "term", "category": "guide", "termIds": ["term-1"], "terms": [{"termId": "term-1", "title": "Hostile <term>", "definition": "Definition & detail"}], "componentReferences": [], "tags": [], "evidence": {"uri": "https://example.test/node"}}],
          |  "relationships": [],
          |  "nodeLimit": 128,
          |  "relationshipLimit": 256,
          |  "truncated": false,
          |  "warnings": [{"value": "warning <detail>"}]
          |}""".stripMargin
        val properties = StaticFormAppRenderer.FormResultProperties(
          StaticFormAppRenderer.FormPageProperties("bok", "bok-retrieval", "get-knowledge-map"),
          200,
          "application/json",
          body
        )

        When("the renderer applies the authored template to the successful response")
        val html = StaticFormAppRenderer().renderFormResultTemplate(properties, template).body

        Then("the rendered output exposes attributed escaped data and resolves the no-JavaScript result widgets")
        html should include("project")
        html should include("project-alpha")
        html should include("dataset-&lt;alpha&gt;")
        html should include("source-&amp;-alpha")
        html should include("https://example.test/evidence?q=1&amp;label=&lt;x&gt;")
        html should include("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")
        html should include("Hostile &lt;term&gt;")
        html should include("<table")
        html should not include("${result.body")
        html should not include("<textus:table")
        html should not include("<textus-error-panel")
      }

      "render a structured failure without inventing a fallback selection" in {
        Given("the authored map result template and a conflicting-selection response containing hostile failure text")
        val page = _read("src/main/web/textus-bok/map.html")
        val template = _map_result_template(page)
        val body = """{"status":"conflicting-selection","message":"projectId <alpha> conflicts with the official profile","failure":{"code":"conflicting-selection","message":"Do not use <path>"}}"""
        val properties = StaticFormAppRenderer.FormResultProperties(
          StaticFormAppRenderer.FormPageProperties("bok", "bok-retrieval", "get-knowledge-map"),
          409,
          "application/json",
          body
        )

        When("the renderer applies the authored template to the structured failure")
        val html = StaticFormAppRenderer().renderFormResultTemplate(properties, template).body

        Then("the rendered error surface preserves the failure and does not invent result attribution")
        html should include("409")
        html should include("conflicting-selection")
        html should include("projectId &lt;alpha&gt; conflicts with the official profile")
        html should include("Do not use &lt;path&gt;")
        html should not include("<dd>official</dd>")
        html should not include("${result.body.selection")
        html should not include("<textus-error-panel")
      }
    }

    "declare the Web entry policy explicitly" which {
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
  }

  private def _read(path: String): String =
    Files.readString(Path.of(path))

  private def _map_result_template(page: String): String = {
    val start = page.indexOf("<textus:operation-result")
    val end = page.lastIndexOf("</textus:operation-result>")
    if (start < 0 || end <= start)
      fail("authored Knowledge Map page is missing its operation-result template")
    page.substring(page.indexOf(">", start) + 1, end)
  }

  private def _fail_conclusion(error: org.goldenport.Conclusion): Nothing = fail(error.toString)
}
