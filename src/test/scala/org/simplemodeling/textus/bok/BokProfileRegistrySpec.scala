package org.simplemodeling.textus.bok

import org.goldenport.Consequence
import org.goldenport.cncf.component.{ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent
import org.simplemodeling.textus.bok.runtime.*
import org.simplemodeling.textus.bok.value.{BokEvidence, BokKnowledgeSource}

/*
 * @since   Aug. 14, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokProfileRegistrySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The private BoK profile registry" should {
    "validate private configuration and its bindings" which {
      "reject duplicate keys and cross-profile dataset claims before registry mutation" in {
        Given("duplicate official bindings and a development binding that claims the official dataset")
        val officialsource = _source("official-source", "official-dataset", "g1", "official")
        val developmentsource = _source("development-source", "official-dataset", "g1", "development")
        val official = BokProfileBinding.official(officialsource, _evidence(officialsource)).TAKE
        val development = BokProfileBinding.development(developmentsource, _evidence(developmentsource)).TAKE

        When("each invalid private configuration is validated")
        val duplicate = BokProfileRegistry.create(
          BokProfileRegistryConfiguration(Vector(official, official))
        )
        val shadow = BokProfileRegistry.create(
          BokProfileRegistryConfiguration(Vector(official, development))
        )

        Then("duplicate and official-shadowing attempts remain distinct structured failures")
        _failure_projection(duplicate) shouldBe Some(
          BokProfileResolutionFailure.Ambiguous -> Some(BokProfileResolutionFailure.Ambiguous.code)
        )
        _failure_projection(shadow) shouldBe Some(
          BokProfileResolutionFailure.ConflictingSelection -> Some(BokProfileResolutionFailure.ConflictingSelection.code)
        )
      }

      "reject malformed component configuration through the closed structured failure vocabulary" in {
        Given("profile-registry configuration roots with a missing collection, malformed binding, or invalid source descriptor")
        val missingprofiles = _configuration(ConfigurationValue.ObjectValue(Map.empty))
        val malformedbinding = _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official")))
        )))
        val invalidsource = _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.ObjectValue(Map(
            "profile" -> ConfigurationValue.StringValue("official"),
            "source" -> ConfigurationValue.ObjectValue(Map(
              "sourceId" -> ConfigurationValue.StringValue("official-source")
            )),
            "evidence" -> ConfigurationValue.ObjectValue(Map(
              "uri" -> ConfigurationValue.StringValue("https://evidence.example/official-source"),
              "sourceId" -> ConfigurationValue.StringValue("official-source")
            ))
          ))))
        )))

        When("each configuration is decoded at the private component bootstrap boundary")
        val results = Vector(missingprofiles, malformedbinding, invalidsource).map(
          BokProfileRegistryConfiguration.fromConfiguration
        )

        Then("every parser failure carries invalid-selection instead of an unclassified argument failure")
        results.map(_failure_projection) shouldBe Vector.fill(3)(Some(
          BokProfileResolutionFailure.InvalidSelection -> Some(BokProfileResolutionFailure.InvalidSelection.code)
        ))
      }

      "fail production component initialization for malformed private configuration" in {
        Given("a subsystem whose private profile registry contains a malformed binding")
        val configuration = _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official")))
        )))
        val subsystem = new Subsystem(
          name = "textus-bok-malformed-profile-registry-spec",
          configuration = configuration
        )

        When("the production component factory initializes the BoK primary component")
        val result = new impl.ComponentFactory().createC(
          ComponentCreate(subsystem, ComponentOrigin.Main)
        )

        Then("initialization fails before an ActionCall can observe the malformed registry")
        _failure_projection(result) shouldBe Some(
          BokProfileResolutionFailure.InvalidSelection -> Some(BokProfileResolutionFailure.InvalidSelection.code)
        )
      }
    }

    "load configured sources and admit complete generations" which {
      "load configured logical resources and resolve one attributable profile deterministically" in {
        Given("official, development, and project bindings delivered as private component/SAR configuration")
        val officialsource = _source("official-source", "official-dataset", "official-g1", "official")
        val developmentsource = _source("development-source", "development-dataset", "development-g1", "development")
        val projectsource = _source("project-alpha-source", "project-alpha-dataset", "project-alpha-g1", "project-alpha")
        val registry = BokProfileRegistry.createC(Record.dataAuto(
          "profiles" -> Vector(
            _binding_record("official", None, officialsource),
            _binding_record("development", None, developmentsource),
            _binding_record("project", Some("alpha"), projectsource)
          )
        )).TAKE
        val officialkey = BokProfileKey.official
        val developmentkey = BokProfileKey.development
        val projectkey = BokProfileKey.project("alpha").TAKE
        val authorization = BokProfileAuthorization.allow(officialkey, developmentkey, projectkey)
        val context = _context(Vector(officialsource, developmentsource, projectsource))

        When("each configured binding is read through CNCF ResourceAccess and admitted as a complete generation")
        Vector(officialkey, developmentkey, projectkey).foreach { key =>
          val normalized = registry.loadConfiguredSource(context, key).TAKE
          registry.admit(key, _publication("complete"), normalized).TAKE shouldBe true
        }
        val official = registry.resolve(
          BokProfileSelection(),
          BokProfileCompatibilityFilter(),
          authorization
        ).TAKE
        val development = registry.resolve(
          BokProfileSelection.development,
          BokProfileCompatibilityFilter(
            Some(developmentsource.datasetId.value),
            Some(developmentsource.sourceId.value)
          ),
          authorization
        ).TAKE
        val project = registry.resolve(
          BokProfileSelection.project("alpha"),
          BokProfileCompatibilityFilter(),
          authorization
        ).TAKE

        Then("omission selects only official while every result preserves its exact identity and evidence")
        official.resolvedProfile shouldBe "official"
        official.projectId shouldBe empty
        official.datasetId shouldBe officialsource.datasetId
        official.sourceId shouldBe officialsource.sourceId
        official.generation shouldBe officialsource.generation
        official.evidence.uri.value shouldBe "https://evidence.example/official-source"
        development.resolvedProfile shouldBe "development"
        development.datasetId shouldBe developmentsource.datasetId
        project.resolvedProfile shouldBe "project"
        project.projectId shouldBe Some("alpha")
        project.datasetId shouldBe projectsource.datasetId
        project.productElementNames.toVector should not contain "resource"
      }

      "bootstrap production component state and ignore an administrative request resource" in {
        Given("an official binding in resolved subsystem configuration and a request with the same identity but another resource")
        val source = _source("official-source", "official-dataset", "official-g1", "official")
        val subsystem = new Subsystem(
          name = "textus-bok-profile-registry-spec",
          configuration = _profile_configuration(source)
        )
        val component = new impl.ComponentFactory().create(
          ComponentCreate(subsystem, ComponentOrigin.Main)
        ).primary.asInstanceOf[BokPrimaryComponent]
        val requested = source.copy(resource = BokResourceReference("urn:textus:bok:request-must-not-be-read"))
        val context = _context(Vector(source))

        When("the component replacement boundary loads and admits the configured complete generation")
        val (key, normalized) = component.loadReplacementSource(context, requested).TAKE
        component.admitReplacement(key, _publication("complete"), normalized).TAKE shouldBe true
        val resolved = component.resolveProfile(
          BokProfileSelection.official,
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(BokProfileKey.official)
        ).TAKE

        Then("bootstrap owns the registry and the installed binding remains the sole resource supplier")
        key shouldBe Some(BokProfileKey.official)
        normalized.source shouldBe source
        normalized.source.resource should not be requested.resource
        resolved.generation shouldBe source.generation
        resolved.evidence.sourceId shouldBe source.sourceId
      }

      "classify configured resource-load failure without leaking its private binding" in {
        Given("an official binding whose configured CNCF resource is unavailable")
        val source = _source("official-source", "official-dataset", "official-g1", "private-missing")
        val binding = BokProfileBinding.official(source, _evidence(source)).TAKE
        val registry = BokProfileRegistry.create(
          BokProfileRegistryConfiguration(Vector(binding))
        ).TAKE
        val context = _context(Vector.empty)

        When("the registry attempts to load that configured logical source")
        val result = registry.loadConfiguredSource(context, BokProfileKey.official)

        Then("the failure is unavailable and does not expose the private resource value")
        _failure_projection(result) shouldBe Some(
          BokProfileResolutionFailure.Unavailable -> Some(BokProfileResolutionFailure.Unavailable.code)
        )
        val display = result match {
          case Consequence.Failure(conclusion) => conclusion.display
          case Consequence.Success(_) => fail("Expected the configured resource load to fail")
        }
        display should not include source.resource.value
      }
    }

    "resolve profiles and preserve closed failures" which {
      "return the closed failure taxonomy without fallback or ambient project inference" in {
        Given("one unavailable official binding and one available explicitly keyed project binding")
        val officialsource = _source("official-source", "official-dataset", "g1", "official")
        val projectsource = _source("project-alpha-source", "project-alpha-dataset", "g1", "project-alpha")
        val official = BokProfileBinding.official(officialsource, _evidence(officialsource)).TAKE
        val project = BokProfileBinding.project("alpha", projectsource, _evidence(projectsource)).TAKE
        val registry = BokProfileRegistry.create(
          BokProfileRegistryConfiguration(Vector(official, project))
        ).TAKE
        val officialkey = BokProfileKey.official
        val projectkey = BokProfileKey.project("alpha").TAKE
        val projectbetakey = BokProfileKey.project("beta").TAKE
        registry.admit(projectkey, _publication("complete"), _normalized(projectsource)).TAKE shouldBe true

        When("invalid, incomplete, unauthorized, absent, and conflicting selections are resolved")
        val invalid = registry.resolve(
          BokProfileSelection(Some("preview"), None),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(officialkey)
        )
        val projectidentityrequired = registry.resolve(
          BokProfileSelection(Some("project"), None),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(projectkey)
        )
        val unavailable = registry.resolve(
          BokProfileSelection(),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(officialkey)
        )
        val unauthorized = registry.resolve(
          BokProfileSelection.project("alpha"),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(officialkey)
        )
        val unauthorizedunregistered = registry.resolve(
          BokProfileSelection.project("beta"),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(officialkey)
        )
        val unregistered = registry.resolve(
          BokProfileSelection.project("beta"),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(projectbetakey)
        )
        val conflicting = registry.resolve(
          BokProfileSelection.project("alpha"),
          BokProfileCompatibilityFilter(datasetId = Some("official-dataset")),
          BokProfileAuthorization.allow(projectkey)
        )

        Then("the requested key fails explicitly and neither another profile nor process context is consulted")
        _failure_projection(invalid) shouldBe Some(
          BokProfileResolutionFailure.InvalidSelection -> Some(BokProfileResolutionFailure.InvalidSelection.code)
        )
        _failure_projection(projectidentityrequired) shouldBe Some(
          BokProfileResolutionFailure.ProjectIdentityRequired -> Some(BokProfileResolutionFailure.ProjectIdentityRequired.code)
        )
        _failure_projection(unavailable) shouldBe Some(
          BokProfileResolutionFailure.Unavailable -> Some(BokProfileResolutionFailure.Unavailable.code)
        )
        _failure_projection(unauthorized) shouldBe Some(
          BokProfileResolutionFailure.Unauthorized -> Some(BokProfileResolutionFailure.Unauthorized.code)
        )
        _failure_projection(unauthorizedunregistered) shouldBe Some(
          BokProfileResolutionFailure.Unauthorized -> Some(BokProfileResolutionFailure.Unauthorized.code)
        )
        _failure_projection(unregistered) shouldBe Some(
          BokProfileResolutionFailure.Unregistered -> Some(BokProfileResolutionFailure.Unregistered.code)
        )
        _failure_projection(conflicting) shouldBe Some(
          BokProfileResolutionFailure.ConflictingSelection -> Some(BokProfileResolutionFailure.ConflictingSelection.code)
        )
        registry.resolve(
          BokProfileSelection(),
          BokProfileCompatibilityFilter(),
          BokProfileAuthorization.allow(officialkey, projectkey)
        ) should matchPattern { case Consequence.Failure(_) => }
      }
    }

    "retain complete generations with explicit freshness" which {
      "retain the previous complete generation after degradation and enforce explicit freshness" in {
        Given("an admitted official generation and a later configuration for the same logical source")
        val firstsource = _source("official-source", "official-dataset", "g1", "official")
        val secondsource = _source("official-source", "official-dataset", "g2", "official")
        val firstbinding = BokProfileBinding.official(firstsource, _evidence(firstsource)).TAKE
        val secondbinding = BokProfileBinding.official(secondsource, _evidence(secondsource)).TAKE
        val registry = BokProfileRegistry.create(
          BokProfileRegistryConfiguration(Vector(firstbinding))
        ).TAKE
        val authorization = BokProfileAuthorization.allow(BokProfileKey.official)
        registry.admit(
          BokProfileKey.official,
          _publication("complete"),
          _normalized(firstsource)
        ).TAKE shouldBe true

        When("the configured replacement degrades")
        registry.configure(BokProfileRegistryConfiguration(Vector(secondbinding))).TAKE
        registry.admit(
          BokProfileKey.official,
          _publication("degraded"),
          _normalized(secondsource)
        ).TAKE shouldBe false
        val retained = registry.resolve(
          BokProfileSelection(),
          BokProfileCompatibilityFilter(),
          authorization
        ).TAKE

        Then("the preceding complete generation remains the atomic readable result")
        retained.generation.value shouldBe "g1"
        retained.evidence.uri.value shouldBe "https://evidence.example/official-source"

        When("configuration requires the degraded replacement generation explicitly")
        val freshbinding = BokProfileBinding.official(
          secondsource,
          _evidence(secondsource),
          BokProfileFreshnessPolicy.ExactGeneration(secondsource.generation)
        ).TAKE
        registry.configure(BokProfileRegistryConfiguration(Vector(freshbinding))).TAKE
        val stale = registry.resolve(
          BokProfileSelection(),
          BokProfileCompatibilityFilter(),
          authorization
        )

        Then("retention does not make the older generation current under that freshness policy")
        _failure_projection(stale) shouldBe Some(
          BokProfileResolutionFailure.Stale -> Some(BokProfileResolutionFailure.Stale.code)
        )

        When("the replacement is later admitted as complete")
        registry.admit(
          BokProfileKey.official,
          _publication("complete"),
          _normalized(secondsource)
        ).TAKE shouldBe true
        val current = registry.resolve(
          BokProfileSelection(),
          BokProfileCompatibilityFilter(),
          authorization
        ).TAKE

        Then("all resolved identities switch to the new complete generation together")
        current.generation.value shouldBe "g2"
        current.datasetId shouldBe secondsource.datasetId
        current.sourceId shouldBe secondsource.sourceId
      }
    }
  }

  private def _binding_record(
    profile: String,
    projectid: Option[String],
    source: BokKnowledgeSource
  ): Record =
    Record.dataAuto(
      "profile" -> profile,
      "projectId" -> projectid,
      "source" -> source.toRecord(),
      "evidence" -> _evidence(source).toRecord()
    )

  private def _source(
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

  private def _evidence(source: BokKnowledgeSource): BokEvidence =
    BokEvidence(
      BokEvidenceUri(s"https://evidence.example/${source.sourceId.value}"),
      source.sourceId,
      None,
      None,
      None
    )

  private def _normalized(source: BokKnowledgeSource): NormalizedBokSource =
    NormalizedBokSource(
      source,
      Vector.empty,
      Vector.empty,
      Vector.empty,
      BokKnowledgeTopology.empty
    )

  private def _publication(state: String): BokFederationPublication =
    BokFederationPublication(state, Record.empty)

  private def _context(sources: Vector[BokKnowledgeSource]): ExecutionContext = {
    val contents = sources.flatMap { source =>
      val root = source.resource.value.stripPrefix("urn:textus:bok:")
      Vector(
        s"$root/metadata/cncf/knowledge-source.json" -> _manifest,
        s"$root/metadata/glossary/terms.json" -> _terms(source)
      )
    }.toMap
    val provider = new InMemoryTextusUrnResourceProvider("bok", contents)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _terms(source: BokKnowledgeSource): String =
    s"""{
       |  "terms": [{
       |    "id": "architecture:${source.sourceId.value}",
       |    "title": "${source.sourceId.value}",
       |    "definition_text": "Configured knowledge.",
       |    "term_type": "concept"
       |  }]
       |}""".stripMargin

  private def _failure_projection(
    result: Consequence[?]
  ): Option[(BokProfileResolutionFailure, Option[String])] =
    result match {
      case Consequence.Failure(conclusion) =>
        BokProfileResolutionFailure.from(conclusion).map(_ -> conclusion.status.appStatus)
      case Consequence.Success(_) => None
    }

  private def _configuration(value: ConfigurationValue): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(BokProfileRegistryConfiguration.configurationKey -> value)),
      ConfigurationTrace.empty
    )

  private def _profile_configuration(source: BokKnowledgeSource): ResolvedConfiguration = {
    val binding = ConfigurationValue.ObjectValue(Map(
      "profile" -> ConfigurationValue.StringValue("official"),
      "source" -> ConfigurationValue.ObjectValue(Map(
        "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value),
        "datasetId" -> ConfigurationValue.StringValue(source.datasetId.value),
        "generation" -> ConfigurationValue.StringValue(source.generation.value),
        "resource" -> ConfigurationValue.StringValue(source.resource.value)
      )),
      "evidence" -> ConfigurationValue.ObjectValue(Map(
        "uri" -> ConfigurationValue.StringValue(_evidence(source).uri.value),
        "sourceId" -> ConfigurationValue.StringValue(source.sourceId.value)
      ))
    ))
    val registry = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(List(binding))
    ))
    val textus = ConfigurationValue.ObjectValue(Map(
      "bok" -> ConfigurationValue.ObjectValue(Map("profile-registry" -> registry))
    ))
    ResolvedConfiguration(
      Configuration(Map("textus" -> textus)),
      ConfigurationTrace.empty
    )
  }

  private val _manifest =
    """{
      |  "schemaVersion": "cncf.knowledge-source.v1",
      |  "resources": [
      |    {"kind": "glossary-terms", "href": "metadata/glossary/terms.json"}
      |  ]
      |}""".stripMargin
}
