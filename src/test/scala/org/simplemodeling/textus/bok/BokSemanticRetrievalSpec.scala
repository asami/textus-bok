package org.simplemodeling.textus.bok

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.knowledge.ComponentKnowledgeManifestConsumerContractCodec
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.{
  BokCandidateKey,
  BokFederationPublication,
  BokKnowledgeCatalog,
  BokSemanticAccess,
  BokSemanticRecord,
  BokSourceReader,
  BokKnowledgeTopology,
  NormalizedBokSource,
  ResolvedBokProfile
}
import org.simplemodeling.textus.bok.value.*

/*
 * Failing-first executable acceptance specification for DOC-07
 * (Phase 59.7 / Step P597-S1 / Slice P597-S1A).
 *
 * The direct semantic vocabulary is intentionally referenced before its
 * production implementation. Structured ResourceAccess input is the only
 * source in these scenarios; no HTML, filesystem, network, raw source, or
 * provider discovery is part of the contract.
 *
 * @since   Aug. 26, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokSemanticRetrievalSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "DOC-07 semantic admission" should {
    "admit digest-bound structured records and preserve every public semantic identity" in {
      Given("a cncf.knowledge-source.v1 manifest with the two declared semantic resources and exact SHA-256 evidence")
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))

      When("CNCF ResourceAccess admits the selected structured source")
      val normalized: NormalizedBokSource = BokSourceReader.read(context, _source).TAKE
      val currentpublic = normalized.semanticRecords.filter(record => record.visibility == "public" && !record.stale)

      Then("NormalizedBokSource.semanticRecords retains thirteen current public kinds, preserves stale index evidence, with restricted metadata withheld, and admits no fabricated consumer records")
      currentpublic should have size 13
      currentpublic.map(_.identity).distinct should have size 13
      currentpublic.map(_.kind).toSet shouldBe _public_kinds
      normalized.semanticRecords should have size 14
      normalized.semanticRecords.map(_.sourceId).distinct shouldBe Vector("source-a")
      normalized.semanticRecords.map(_.datasetId).distinct shouldBe Vector("dataset-a")
      normalized.semanticRecords.map(_.generation).distinct shouldBe Vector("g1")
      normalized.semanticRecords.foreach { record =>
        record.title should not be empty
        record.summary should not include "<html>"
        record.documentId should not be empty
        record.canonicalUrl should startWith("https://")
        record.indexedAt should not be empty
        record.digest should fullyMatch regex "[0-9a-f]{64}"
        record.componentReference.foreach { reference =>
          reference.kind.value shouldBe "car"
          reference.name.value shouldBe "textus-account"
        }
      }
      normalized.semanticRecords.find(_.identity == "urn:cncf:framework:source:1.0.0") shouldBe defined
      normalized.semanticRecords.find(_.identity == "urn:cncf:framework:source:1.0.0").foreach { record =>
        record.title shouldBe "cncf 1.0.0"
        record.summary should include("cncf 1.0.0")
        record.documentId shouldBe "urn:cncf:framework:document:1.0.0"
        record.sectionId shouldBe Some("urn:cncf:framework:section:intro")
        record.canonicalUrl shouldBe "https://example.test/framework/1.0.0"
        record.indexedAt shouldBe "2026-08-26"
        record.digest shouldBe _sha256("framework:cncf:source:1.0.0")
        record.authority shouldBe "framework-publication"
        record.visibility shouldBe "public"
        record.product shouldBe Some("cncf")
        record.version shouldBe Some("1.0.0")
        record.publicationGeneration shouldBe Some("2026-08-26")
        record.publicationDigest shouldBe Some(_sha256("framework:cncf:1.0.0"))
        record.profile shouldBe empty
        record.owner shouldBe empty
        record.license shouldBe empty
        record.logicalPath shouldBe empty
        record.chunkId shouldBe empty
      }
      normalized.semanticRecords.find(_.identity == "urn:cncf:resource:fixture:directive") shouldBe defined
      normalized.semanticRecords.find(_.identity == "urn:cncf:resource:fixture:directive").foreach { record =>
        record.title shouldBe "mounted-directive"
        record.documentId shouldBe "mounted-directive"
        record.sectionId shouldBe Some("public-rule-identity")
        record.canonicalUrl shouldBe "https://example.test/directive/public"
        record.indexedAt shouldBe "g1"
        record.digest shouldBe _sha256("directive:core:public")
        record.authority shouldBe "mounted-directive-remains-authoritative"
        record.visibility shouldBe "public"
        record.product shouldBe empty
        record.version shouldBe Some("1.0.0")
        record.profile shouldBe Some("public-profile")
        record.owner shouldBe empty
        record.license shouldBe Some("Apache-2.0")
        record.logicalPath shouldBe Some("directive/public.yaml")
        record.chunkId shouldBe empty
        record.publicationGeneration shouldBe empty
        record.publicationDigest shouldBe empty
      }
      normalized.semanticRecords.find(_.identity == "urn:cncf:resource:fixture:skill") shouldBe defined
      normalized.semanticRecords.find(_.identity == "urn:cncf:resource:fixture:skill").foreach { record =>
        record.title shouldBe "public-skill-catalog"
        record.summary should include("descriptive public catalog metadata")
        record.summary should include("1.0.0")
        record.documentId shouldBe "public-skill-catalog"
        record.canonicalUrl shouldBe "https://example.test/skills/public"
        record.indexedAt shouldBe "g1"
        record.digest shouldBe _sha256("skill:catalog:public")
        record.authority shouldBe "skill-catalog"
        record.visibility shouldBe "public"
        record.product shouldBe empty
        record.version shouldBe Some("1.0.0")
        record.profile shouldBe empty
        record.owner shouldBe Some("cncf")
        record.license shouldBe Some("Apache-2.0")
        record.logicalPath shouldBe Some("skills/catalog.json")
        record.chunkId shouldBe empty
        record.publicationGeneration shouldBe empty
        record.publicationDigest shouldBe empty
      }
      normalized.semanticRecords.map(_.identity) should contain allOf(
        "component:org.example:account:1.0.0",
        "component:org.example:account:consumer"
      )
      normalized.semanticRecords.find(_.identity == "catalog:framework:stale") shouldBe defined
      normalized.semanticRecords.find(_.identity == "skill:catalog:restricted") shouldBe empty
    }

    "admit semantic-index manifest records without creating direct records for absent optional roots" in {
      Given("a canonical consumer contract whose optional framework, directive, and skill roots are absent")
      val contract = ComponentKnowledgeManifestConsumerContractCodec.encode(
        ComponentKnowledgeManifestConsumerContractCodec.decodeC(_consumer_contract).TAKE.copy(
          frameworkPublication = None,
          publicDirective = None,
          skillCatalog = None
        )
      )
      val context = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest.replace(
          _sha256(_consumer_contract),
          _sha256(contract)
        ),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> contract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))

      When("the selected source is read through ResourceAccess")
      val normalized = BokSourceReader.read(context, _source).TAKE

      Then("only structured semantic-index records remain and component manifest and contract identities are preserved")
      normalized.semanticRecords.map(_.kind).toSet shouldBe _semantic_index_kinds
      normalized.semanticRecords.map(_.identity) should contain allOf(
        "component:org.example:account:1.0.0",
        "component:org.example:account:consumer"
      )
      normalized.semanticRecords.map(_.identity) should not contain "urn:cncf:framework:source:1.0.0"
      normalized.semanticRecords.map(_.identity) should not contain "urn:cncf:resource:fixture:directive"
      normalized.semanticRecords.map(_.identity) should not contain "urn:cncf:resource:fixture:skill"
    }

    "reject digest mismatch, unsafe child href, unknown kind, malformed input, and invalid declared metadata before ambient selection" in {
      Given("one valid structured source and nine independently hostile manifest or resource variants")
      val mismatch = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest.replace(
          _sha256(_semantic_index),
          "0000000000000000000000000000000000000000000000000000000000000000"
        ),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))
      val unsafe = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest.replace(
          "metadata/cncf/semantic-index.json",
          "../outside/semantic-index.json"
        ),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))
      val unknown = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest.replace(
          "semantic-index",
          "ambient-source"
        ),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))
      val malformed = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest,
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> "{ malformed structured index"
      ))
      val publicationgenerationmarkupcontract = _consumer_contract.replace(
        "\"publicationGeneration\": \"2026-08-26\"",
        "\"publicationGeneration\": \"<2026-08-26>\""
      )
      val publicationgenerationmarkup = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _manifest.replace(
          _sha256(_consumer_contract),
          _sha256(publicationgenerationmarkupcontract)
        ),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> publicationgenerationmarkupcontract,
        "fixture/metadata/cncf/semantic-index.json" -> _semantic_index
      ))
      val emptypublicindex = _semantic_index.replace(
        "\"summary\":\"Account component manifest metadata\"",
        "\"summary\":\"Account component manifest metadata\",\"product\":\"\""
      )
      val emptypublic = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _semantic_manifest_for(emptypublicindex),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> emptypublicindex
      ))
      val markupindex = _semantic_index.replace(
        "\"summary\":\"Account component manifest metadata\"",
        "\"summary\":\"Account component manifest metadata\",\"profile\":\"<public>\""
      )
      val markup = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _semantic_manifest_for(markupindex),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> markupindex
      ))
      val unsafepathindex = _semantic_index.replace(
        "\"summary\":\"Account component manifest metadata\"",
        "\"summary\":\"Account component manifest metadata\",\"logicalPath\":\"../outside.md\""
      )
      val unsafepath = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _semantic_manifest_for(unsafepathindex),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> unsafepathindex
      ))
      val invalidpublicationdigestindex = _semantic_index.replace(
        "\"summary\":\"Account component manifest metadata\"",
        "\"summary\":\"Account component manifest metadata\",\"publicationDigest\":\"ABC\""
      )
      val invalidpublicationdigest = _context(Map(
        "fixture/metadata/cncf/knowledge-source.json" -> _semantic_manifest_for(invalidpublicationdigestindex),
        "fixture/metadata/cncf/component-knowledge-consumer-contract.json" -> _consumer_contract,
        "fixture/metadata/cncf/semantic-index.json" -> invalidpublicationdigestindex
      ))

      When("each input is read through the explicitly selected source")
      val publicationgenerationmarkupresult = BokSourceReader.read(publicationgenerationmarkup, _source)
      val results = Vector(
        BokSourceReader.read(mismatch, _source),
        BokSourceReader.read(unsafe, _source),
        BokSourceReader.read(unknown, _source),
        BokSourceReader.read(malformed, _source),
        publicationgenerationmarkupresult,
        BokSourceReader.read(emptypublic, _source),
        BokSourceReader.read(markup, _source),
        BokSourceReader.read(unsafepath, _source),
        BokSourceReader.read(invalidpublicationdigest, _source)
      )

      Then("every unsafe input is rejected before inferred or ambient source selection and does not expose source content")
      results.foreach { result =>
        result should matchPattern { case Consequence.Failure(_) => }
        result match {
          case Consequence.Failure(conclusion) =>
            conclusion.display should not include "ambient-source"
            conclusion.display should not include "outside/semantic-index.json"
            conclusion.display should not include "malformed structured index"
          case Consequence.Success(value) => fail("hostile semantic input was admitted: " + value)
        }
      }
      publicationgenerationmarkupresult match {
        case Consequence.Failure(conclusion) =>
          conclusion.display should include("BoK consumer record.publicationGeneration")
        case Consequence.Success(value) => fail("digest-valid direct consumer publicationGeneration markup was admitted: " + value)
      }
    }
  }

  "DOC-07 semantic catalog" should {
    "expose exact lexical and structural retrieval with bounded attributable responses" in {
      Given("one selected source with current semantic records and an explicit public caller privilege")
      val fixture = _catalog_fixture()
      val access = BokSemanticAccess(fixture.selection, BokSemanticAccess.CallerPrivilege.Public)

      When("the selected catalog performs exact lexical and structural reads")
      val lexical: SemanticKnowledgeSearchResponse =
        fixture.catalog.searchSemanticKnowledge(access, "Introduction", 2)(_ => Consequence.success(Map.empty)).TAKE
      val discovered: SemanticKnowledgeDiscoveryResponse =
        fixture.catalog.discoverSemanticKnowledge(access, 2)
      val allDiscovered: SemanticKnowledgeDiscoveryResponse =
        fixture.catalog.discoverSemanticKnowledge(access, 20)
      val manifest: SemanticManifestResponse =
        fixture.catalog.getSemanticManifest(access, "component:org.example:account:1.0.0")
      val manifestAsResource: SemanticResourceResponse =
        fixture.catalog.getSemanticResource(access, "component:org.example:account:1.0.0")
      val resource: SemanticResourceResponse =
        fixture.catalog.getSemanticResource(access, "rdf:framework:graph")
      val resourceAsManifest: SemanticManifestResponse =
        fixture.catalog.getSemanticManifest(access, "rdf:framework:graph")
      val section: SemanticSectionResponse =
        fixture.catalog.getSemanticSection(access, "smartdox-framework-guide", "intro")

      Then("each typed response is matched, selected, bounded, and attributable")
      lexical.status.value shouldBe "matched"
      lexical.results.map(_.identity) should contain("smartdox:framework:guide:intro")
      lexical.limit shouldBe 2
      lexical.truncated shouldBe false
      lexical.selection.sourceId.value shouldBe "source-a"
      lexical.selection.datasetId.value shouldBe "dataset-a"
      lexical.selection.generation.value shouldBe "g1"
      discovered.status.value shouldBe "matched"
      discovered.results.map(_.identity) shouldBe Vector(
        "component:org.example:account:1.0.0",
        "component:org.example:account:consumer"
      )
      discovered.results should have size 2
      discovered.limit shouldBe 2
      discovered.truncated shouldBe true
      allDiscovered.results.map(_.kind).toSet shouldBe _public_kinds
      allDiscovered.results should have size 13
      allDiscovered.truncated shouldBe false
      manifest.status.value shouldBe "matched"
      manifest.result.map(_.identity) shouldBe Some("component:org.example:account:1.0.0")
      manifest.result.flatMap(_.componentReference) shouldBe Some(_component_reference)
      manifestAsResource.status.value shouldBe "no-match"
      manifestAsResource.result shouldBe empty
      resourceAsManifest.status.value shouldBe "no-match"
      resourceAsManifest.result shouldBe empty
      resource.status.value shouldBe "matched"
      resource.result.map(_.documentId) shouldBe Some("rdf-framework-graph")
      section.status.value shouldBe "matched"
      section.result.flatMap(_.sectionId) shouldBe Some("intro")
      section.result.map(_.evidence.sourceId.value) shouldBe Some("source-a")
    }

    "withhold duplicate exact SmartDox sections as ambiguous" in {
      Given("a selected source with two SmartDox sections for the same exact document and section identity")
      val catalog = new BokKnowledgeCatalog()
      val duplicate = _semantic_record(
        "smartdox-section",
        "smartdox:framework:guide:intro:duplicate",
        "Duplicate Framework Introduction",
        "Duplicate SmartDox section metadata",
        "smartdox-framework-guide",
        Some("intro"),
        "https://example.test/docs/framework#intro-duplicate",
        "public",
        "smartdox"
      )
      val normalized = NormalizedBokSource(
        source = _source,
        terms = Vector.empty,
        components = Vector(_component_reference),
        warnings = Vector.empty,
        topology = BokKnowledgeTopology.empty,
        semanticRecords = _semantic_records :+ duplicate
      )
      catalog.commit(BokFederationPublication("complete", Record.empty), normalized) shouldBe true
      val access = BokSemanticAccess(_resolved, BokSemanticAccess.CallerPrivilege.Public)

      When("the catalog performs the exact SmartDox section read")
      val section = catalog.getSemanticSection(access, "smartdox-framework-guide", "intro")

      Then("the ambiguous outcome discloses no section, summary, authority, or component reference")
      section.status.value shouldBe "ambiguous"
      section.result shouldBe empty
      section.summary shouldBe empty
      section.authority shouldBe empty
      section.componentReference shouldBe empty
    }

    "report stale and forbidden evidence without disclosure and keep provider candidates secondary" in {
      Given("distinct stale and restricted records plus explicit public and privileged caller access")
      val fixture = _catalog_fixture()
      val publicaccess = BokSemanticAccess(fixture.selection, BokSemanticAccess.CallerPrivilege.Public)
      val privilegedaccess = BokSemanticAccess(fixture.selection, BokSemanticAccess.CallerPrivilege.MetadataReader)
      val inventedcandidate = BokCandidateKey("dataset-a", "source-a", "invented-provider-record")
      var providercalls = 0

      When("stale, restricted, exact lexical, and provider fallback queries are issued")
      val stale: SemanticResourceResponse =
        fixture.catalog.getSemanticResource(publicaccess, "catalog:framework:stale")
      val forbidden: SemanticResourceResponse =
        fixture.catalog.getSemanticResource(publicaccess, "skill:catalog:restricted")
      val exact: SemanticKnowledgeSearchResponse =
        fixture.catalog.searchSemanticKnowledge(publicaccess, "Runtime", 1) { _ =>
          providercalls += 1
          Consequence.success(Map.empty)
        }.TAKE
      val noMatch: SemanticKnowledgeSearchResponse =
        fixture.catalog.searchSemanticKnowledge(privilegedaccess, "no lexical match", 1) { _ =>
          providercalls += 1
          Consequence.success(Map(inventedcandidate -> 0.99))
        }.TAKE

      Then("stale is explicit, forbidden withholds result content, and provider fallback is secondary and non-authoritative")
      stale.status.value shouldBe "stale"
      stale.result shouldBe empty
      stale.summary shouldBe empty
      stale.authority shouldBe empty
      stale.componentReference shouldBe empty
      forbidden.status.value shouldBe "forbidden"
      forbidden.result shouldBe empty
      forbidden.summary shouldBe empty
      forbidden.authority shouldBe empty
      forbidden.componentReference shouldBe empty
      exact.status.value shouldBe "matched"
      noMatch.status.value shouldBe "no-match"
      noMatch.results shouldBe empty
      providercalls shouldBe 1
      noMatch.componentReference shouldBe empty
      noMatch.authority shouldBe empty
    }
  }

  private final case class CatalogFixture(
    catalog: BokKnowledgeCatalog,
    selection: ResolvedBokProfile
  )

  private def _catalog_fixture(): CatalogFixture = {
    val catalog = new BokKnowledgeCatalog()
    val normalized = NormalizedBokSource(
      source = _source,
      terms = Vector.empty,
      components = Vector(_component_reference),
      warnings = Vector.empty,
      topology = BokKnowledgeTopology.empty,
      semanticRecords = _semantic_records
    )
    normalized.semanticRecords.map(_.identity).distinct should have size 15
    catalog.commit(BokFederationPublication("complete", Record.empty), normalized) shouldBe true
    CatalogFixture(catalog, _resolved)
  }

  private def _semantic_record_json(
    kind: String,
    identity: String,
    title: String,
    summary: String,
    documentid: String,
    sectionid: String,
    canonicalurl: String,
    visibility: String,
    authority: String,
    stale: Boolean = false
  ): String =
    "{\"kind\":\"" + kind + "\",\"identity\":\"" + identity + "\",\"title\":\"" + title +
      "\",\"summary\":\"" + summary + "\",\"documentId\":\"" + documentid +
      "\",\"sectionId\":" + (if (sectionid.isEmpty) "null" else "\"" + sectionid + "\"") + ",\"canonicalUrl\":\"" + canonicalurl +
      "\",\"indexedAt\":\"2026-08-26T00:00:00Z\",\"visibility\":\"" + visibility +
      "\",\"authority\":\"" + authority + "\",\"stale\":" + stale +
      ",\"sha256\":\"" + _sha256(identity) + "\"}"

  private def _semantic_record(
    kind: String,
    identity: String,
    title: String,
    summary: String,
    documentid: String,
    sectionid: Option[String],
    canonicalurl: String,
    visibility: String,
    authority: String,
    stale: Boolean = false,
    componentreference: Option[ComponentReference] = None
  ): BokSemanticRecord =
    BokSemanticRecord(
      kind,
      identity,
      title,
      summary,
      documentid,
      sectionid,
      canonicalurl,
      "2026-08-26T00:00:00Z",
      visibility,
      authority,
      _source.sourceId.value,
      _source.datasetId.value,
      _source.generation.value,
      _sha256(identity),
      stale,
      componentreference
    )

  private def _context(contents: Map[String, String]): ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map(byte => Integer.toHexString(byte & 0xff).reverse.padTo(2, '0').reverse)
      .mkString

  private def _resolved: ResolvedBokProfile =
    ResolvedBokProfile(
      "official",
      None,
      _source.datasetId,
      _source.sourceId,
      _source.generation,
      BokEvidence(
        BokEvidenceUri("urn:textus:bok:source-a:selection"),
        _source.sourceId,
        None,
        None,
        None
      )
    )

  private val _source = BokKnowledgeSource(
    BokSourceId("source-a"),
    BokDatasetId("dataset-a"),
    BokSourceGeneration("g1"),
    BokResourceReference("urn:textus:bok:fixture")
  )

  private val _component_reference = ComponentReference(
    Some(ComponentSourceId("source-a")),
    Some(ComponentCatalogId("car/org/example/account.yaml")),
    Some(ComponentOrganization("org.example")),
    ComponentName("textus-account"),
    ComponentTitle("Account Component"),
    ComponentKind("car"),
    Some(ComponentVersion("1.0.0")),
    BokEvidence(
      BokEvidenceUri("urn:textus:bok:source-a:component-account"),
      _source.sourceId,
      None,
      None,
      None
    )
  )

  private val _public_kinds = Set(
    "component-manifest",
    "component-knowledge-consumer-contract",
    "semantic-index",
    "framework-publication",
    "directive-metadata",
    "skill-metadata",
    "smartdox-document",
    "smartdox-section",
    "rdf-jsonld",
    "glossary",
    "ontology",
    "schema",
    "catalog"
  )

  private val _semantic_records = Vector(
    _semantic_record("component-manifest", "component:org.example:account:1.0.0", "Account Component", "Account component manifest metadata", "component-account", None, "https://example.test/components/account", "public", "component-manifest", componentreference = Some(_component_reference)),
    _semantic_record("component-knowledge-consumer-contract", "component:org.example:account:consumer", "Account Consumer Contract", "Value-only consumer contract evidence", "component-account-consumer", None, "https://example.test/components/account/consumer", "public", "component-knowledge-consumer"),
    _semantic_record("semantic-index", "semantic-index:source-a:g1", "Semantic Index", "Structured semantic index metadata", "semantic-index", None, "https://example.test/semantic-index", "public", "source-a"),
    _semantic_record("framework-publication", "framework:cncf:1.0.0", "CNCF Framework", "Framework publication identity and hash", "framework-publication", None, "https://example.test/framework/1.0.0", "public", "cncf-publication"),
    _semantic_record("directive-metadata", "directive:core:public", "Public Directive", "Public Directive metadata only", "directive-public", None, "https://example.test/directive/public", "public", "mounted-directive-remains-authoritative"),
    _semantic_record("skill-metadata", "skill:catalog:public", "Public Skill", "Public Skill catalog metadata only", "skill-public", None, "https://example.test/skills/public", "public", "skill-catalog"),
    _semantic_record("smartdox-document", "smartdox:framework:guide", "Framework Guide", "SmartDox document metadata", "smartdox-framework-guide", None, "https://example.test/docs/framework", "public", "smartdox"),
    _semantic_record("smartdox-section", "smartdox:framework:guide:intro", "Framework Introduction", "SmartDox section metadata", "smartdox-framework-guide", Some("intro"), "https://example.test/docs/framework#intro", "public", "smartdox"),
    _semantic_record("rdf-jsonld", "rdf:framework:graph", "Framework RDF", "RDF and JSON-LD graph metadata", "rdf-framework-graph", None, "https://example.test/rdf/framework", "public", "framework-publication"),
    _semantic_record("glossary", "glossary:framework:runtime", "Runtime Glossary", "Glossary entry metadata", "glossary-runtime", None, "https://example.test/glossary/runtime", "public", "framework-publication"),
    _semantic_record("ontology", "ontology:framework:component", "Component Ontology", "Ontology metadata", "ontology-component", None, "https://example.test/ontology/component", "public", "framework-publication"),
    _semantic_record("schema", "schema:framework:component", "Component Schema", "Schema metadata", "schema-component", None, "https://example.test/schema/component", "public", "framework-publication"),
    _semantic_record("catalog", "catalog:framework:components", "Framework Catalog", "Catalog metadata", "catalog-components", None, "https://example.test/catalog/components", "public", "framework-publication"),
    _semantic_record("catalog", "catalog:framework:stale", "Stale Framework Catalog", "Stale catalog metadata", "catalog-stale", None, "https://example.test/catalog/stale", "public", "framework-publication", stale = true),
    _semantic_record("skill-metadata", "skill:catalog:restricted", "Restricted Skill", "Restricted Skill metadata", "skill-restricted", None, "https://example.test/skills/restricted", "restricted", "skill-catalog")
  )

  private val _semantic_index_kinds = Set(
    "component-manifest",
    "component-knowledge-consumer-contract",
    "semantic-index",
    "smartdox-document",
    "smartdox-section",
    "rdf-jsonld",
    "glossary",
    "ontology",
    "schema",
    "catalog"
  )

  private lazy val _semantic_index_records =
    _semantic_records.filter(record => _semantic_index_kinds.contains(record.kind))

  private lazy val _manifest =
    """{
       |  "schemaVersion": "cncf.knowledge-source.v1",
       |  "resources": [
       |    {"kind": "component-knowledge-consumer-contract", "href": "metadata/cncf/component-knowledge-consumer-contract.json", "sha256": """".stripMargin +
      _sha256(_consumer_contract) +
      """"},
       |    {"kind": "semantic-index", "href": "metadata/cncf/semantic-index.json", "sha256": """".stripMargin +
      _sha256(_semantic_index) +
      """"}
       |  ]
       |}""".stripMargin

  private def _semantic_manifest_for(semanticindex: String): String =
    _manifest.replace(_sha256(_semantic_index), _sha256(semanticindex))

  private lazy val _consumer_contract =
    """{
      |  "schema": "cncf.component-knowledge-consumer.v1",
      |  "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |  "logicalRelease": "0.1.0-SNAPSHOT",
      |  "frameworkPublication": {
      |    "product": "cncf",
      |    "version": "1.0.0",
      |    "canonicalUrl": "https://example.test/framework/1.0.0",
      |    "publicationGeneration": "2026-08-26",
      |    "documentId": "urn:cncf:framework:document:1.0.0",
      |    "sectionId": "urn:cncf:framework:section:intro",
      |    "sha256": """".stripMargin +
      _sha256("framework:cncf:1.0.0") +
      """" ,
      |    "availability": "online",
      |    "sourceIdentity": "urn:cncf:framework:source:1.0.0",
      |    "sourceSha256": """".stripMargin +
      _sha256("framework:cncf:source:1.0.0") +
      """"
      |  },
      |  "publicDirective": {
      |    "logicalIdentity": {
      |      "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |      "logicalRelease": "0.1.0-SNAPSHOT",
      |      "parentComponentId": null,
      |      "childRole": "Directive",
      |      "logicalResource": "urn:cncf:resource:fixture:directive"
      |    },
      |    "directiveId": "mounted-directive",
      |    "profileId": "public-profile",
      |    "ruleId": "public-rule-identity",
      |    "origin": "urn:cncf:directive:public",
      |    "version": "1.0.0",
      |    "authority": "mounted-directive-remains-authoritative",
      |    "visibility": "public",
      |    "sourceSha256": """".stripMargin +
      _sha256("directive:core:public") +
      """" ,
      |    "redaction": "source-and-rule-content-withheld",
      |    "guideReference": "https://example.test/directive/public"
      |  },
      |  "skillCatalog": {
      |    "logicalIdentity": {
      |      "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |      "logicalRelease": "0.1.0-SNAPSHOT",
      |      "parentComponentId": null,
      |      "childRole": "SkillCatalog",
      |      "logicalResource": "urn:cncf:resource:fixture:skill"
      |    },
      |    "catalogId": "public-skill-catalog",
      |    "owner": "cncf",
      |    "purpose": "descriptive public catalog metadata",
      |    "trigger": "explicit user request",
      |    "requirements": ["component knowledge manifest"],
      |    "permissions": ["metadata visibility"],
      |    "sideEffects": ["none"],
      |    "mcpRequirements": ["descriptive only"],
      |    "installationReference": "https://example.test/skills/public",
      |    "visibility": "ecosystem",
      |    "version": "1.0.0",
      |    "sourceSha256": """".stripMargin +
      _sha256("skill:catalog:public") +
      """"
      |  },
      |  "resources": [
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |        "logicalRelease": "0.1.0-SNAPSHOT",
      |        "parentComponentId": null,
      |        "childRole": "Documentation",
      |        "logicalResource": "urn:cncf:resource:fixture:component-knowledge-integration"
      |      },
      |      "logicalPath": "documentation/fixture.md",
      |      "kind": "documentation",
      |      "role": "documentation",
      |      "language": "en",
      |      "mediaType": "text/markdown",
      |      "size": 1,
      |      "sha256": """".stripMargin +
      _sha256("component-knowledge-integration:documentation/fixture.md") +
      """",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example.fixture:component-knowledge-integration:0.1.0-SNAPSHOT",
      |        "logicalSource": "component-car:fixture",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": """".stripMargin +
      _sha256("component-knowledge-integration:documentation/fixture.md") +
      """"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |        "logicalRelease": "0.1.0-SNAPSHOT",
      |        "parentComponentId": null,
      |        "childRole": "Directive",
      |        "logicalResource": "urn:cncf:resource:fixture:directive"
      |      },
      |      "logicalPath": "directive/public.yaml",
      |      "kind": "directive",
      |      "role": "directive",
      |      "language": "en",
      |      "mediaType": "application/yaml",
      |      "size": 1,
      |      "sha256": """".stripMargin +
      _sha256("directive:core:public") +
      """",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example.fixture:component-knowledge-integration:0.1.0-SNAPSHOT",
      |        "logicalSource": "component-car:fixture",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": """".stripMargin +
      _sha256("directive:core:public") +
      """"
      |      }
      |    },
      |    {
      |      "logicalIdentity": {
      |        "componentId": "org.example.fixture.ComponentKnowledgeIntegration",
      |        "logicalRelease": "0.1.0-SNAPSHOT",
      |        "parentComponentId": null,
      |        "childRole": "SkillCatalog",
      |        "logicalResource": "urn:cncf:resource:fixture:skill"
      |      },
      |      "logicalPath": "skills/catalog.json",
      |      "kind": "skill-catalog",
      |      "role": "skill-catalog",
      |      "language": "en",
      |      "mediaType": "application/json",
      |      "size": 1,
      |      "sha256": """".stripMargin +
      _sha256("skill:catalog:public") +
      """",
      |      "metadata": {
      |        "authority": "component",
      |        "stability": "stable",
      |        "source": "component-declared",
      |        "license": "Apache-2.0",
      |        "disclosure": "metadata-only"
      |      },
      |      "availability": "available",
      |      "integrity": "verified",
      |      "authorization": "granted",
      |      "provenance": {
      |        "sourceKind": "expanded-car",
      |        "artifactCoordinate": "org.example.fixture:component-knowledge-integration:0.1.0-SNAPSHOT",
      |        "logicalSource": "component-car:fixture",
      |        "resolutionStep": "expanded-car:2",
      |        "externalDeploymentRequired": false,
      |        "matchingDigest": """".stripMargin +
      _sha256("skill:catalog:public") +
      """"
      |      }
      |    }
      |  ]
      |}""".stripMargin

  private lazy val _semantic_index =
    """{
       |  "schemaVersion": "cncf.semantic-index.v1",
       |  "kind": "semantic-index",
       |  "sourceId": "source-a",
       |  "datasetId": "dataset-a",
       |  "generation": "g1",
       |  "records": [""".stripMargin +
      _semantic_index_records.map { record =>
        _semantic_record_json(
          record.kind,
          record.identity,
          record.title,
          record.summary,
          record.documentId,
          record.sectionId.getOrElse(""),
          record.canonicalUrl,
          record.visibility,
          record.authority,
          record.stale
        )
      }.mkString(",") +
      """]
       |}""".stripMargin
}
