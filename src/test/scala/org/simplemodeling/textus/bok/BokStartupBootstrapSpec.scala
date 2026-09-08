package org.simplemodeling.textus.bok

import java.time.Instant
import org.goldenport.Consequence
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.component.{ComponentActivation, ComponentActivationCancellation, ComponentActivationContext, ComponentCreate, ComponentOrigin}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.resource.{InMemoryTextusUrnResourceProvider, ResourceAccessTestProfile}
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.testutil.RuntimeBindingAdmissionFixture
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.datatype.{BokDatasetId, BokResourceReference, BokSourceGeneration, BokSourceId}
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent
import org.simplemodeling.textus.bok.value.BokKnowledgeSource
import org.simplemodeling.textus.bok.runtime.*

/**
 * Executable specification for the private Phase 8 bootstrap selector,
 * replacement, and activation boundary. Managed readiness/MCP attribution
 * remains separately pending for P8-C.
 *
 * @since   Sep. 8, 2026
 * @version Sep. 8, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokStartupBootstrapSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "The private BoK startup bootstrap configuration" should {
    "remain disabled when the optional configuration is absent" in {
      Given("a resolved configuration with no textus.bok.bootstrap value")
      val configuration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)

      When("the startup bootstrap configuration is decoded")
      val result = BokStartupBootstrapConfiguration.fromConfiguration(configuration)

      Then("decoding succeeds with no bootstrap selection rather than selecting official implicitly")
      result match {
        case Consequence.Success(value) => value shouldBe None
        case Consequence.Failure(conclusion) =>
          fail(s"Expected disabled bootstrap, got ${conclusion.display}")
      }
    }

    "decode official development and project selectors in declaration order" in {
      Given("a nested bootstrap object containing three exact logical profile selectors")
      val configuration = _nested_configuration(Vector(
        "official",
        "project:alpha",
        "development"
      ))

      When("the startup bootstrap configuration is decoded")
      val result = BokStartupBootstrapConfiguration.fromConfiguration(configuration)

      Then("the normalized registry keys retain the declared order and exact project identity")
      result match {
        case Consequence.Success(Some(value)) =>
          value.profiles.map(_.render) shouldBe Vector("official", "project:alpha", "development")
        case Consequence.Success(None) => fail("Expected an enabled bootstrap configuration")
        case Consequence.Failure(conclusion) =>
          fail(s"Expected valid bootstrap selectors, got ${conclusion.display}")
      }
    }

    "reject blank unknown malformed and empty profile declarations structurally" in {
      Given("bootstrap objects with blank, unknown, non-string, missing-list, extra-field, or empty declarations")
      val invalidconfigurations = Vector(
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue(" ")))
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("preview")))
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.NumberValue(1)))
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map.empty)),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(Nil)
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official"))),
          "mode" -> ConfigurationValue.StringValue("server")
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official"))),
          "mode" -> ConfigurationValue.NullValue
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official"))),
          "mode" -> ConfigurationValue.ListValue(Nil)
        ))),
        _configuration(ConfigurationValue.ObjectValue(Map(
          "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official"))),
          "mode" -> ConfigurationValue.ObjectValue(Map.empty)
        )))
      )

      When("each malformed declaration is decoded")
      val failures = invalidconfigurations.map(BokStartupBootstrapConfiguration.fromConfiguration)

      Then("every malformed shape is rejected with the existing invalid-selection appStatus")
      failures.map(_failure_projection) shouldBe Vector.fill(invalidconfigurations.size)(
        Some(BokProfileResolutionFailure.InvalidSelection -> Some(BokProfileResolutionFailure.InvalidSelection.code))
      )
    }

    "reject duplicate normalized keys and direct-nested ambiguity" in {
      Given("one duplicate ordered selector and one configuration represented at both supported paths")
      val duplicate = _configuration(ConfigurationValue.ObjectValue(Map(
        "profiles" -> ConfigurationValue.ListValue(List(
          ConfigurationValue.StringValue("project:alpha"),
          ConfigurationValue.StringValue("project:alpha")
        ))
      )))
      val direct = ConfigurationValue.ObjectValue(Map(
        "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official")))
      ))
      val nested = _nested_configuration(Vector("development"))
      val ambiguous = ResolvedConfiguration(
        Configuration(Map(
          BokStartupBootstrapConfiguration.configurationKey -> direct,
          "textus" -> nested.configuration.values("textus")
        )),
        ConfigurationTrace.empty
      )

      When("both invalid configurations are decoded")
      val duplicateresult = BokStartupBootstrapConfiguration.fromConfiguration(duplicate)
      val ambiguousresult = BokStartupBootstrapConfiguration.fromConfiguration(ambiguous)

      Then("duplicates use ambiguous while dual path declarations use the same structured ambiguity taxonomy")
      _failure_projection(duplicateresult) shouldBe Some(
        BokProfileResolutionFailure.Ambiguous -> Some(BokProfileResolutionFailure.Ambiguous.code)
      )
      _failure_projection(ambiguousresult) shouldBe Some(
        BokProfileResolutionFailure.Ambiguous -> Some(BokProfileResolutionFailure.Ambiguous.code)
      )
    }

    "preserve the existing project identity failure for an unusable project selector" in {
      Given("a project selector with no usable projectId")
      val configuration = _configuration(ConfigurationValue.ObjectValue(Map(
        "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("project:")))
      )))

      When("the selector is normalized through BokProfileSelection")
      val result = BokStartupBootstrapConfiguration.fromConfiguration(configuration)

      Then("the decoder returns the existing project-identity-required structured failure")
      _failure_projection(result) shouldBe Some(
        BokProfileResolutionFailure.ProjectIdentityRequired ->
          Some(BokProfileResolutionFailure.ProjectIdentityRequired.code)
      )
    }
  }

  "The Phase 8 startup bootstrap acceptance" should {
    "opt the BoK component into activation while remaining inert outside Server mode" in {
      Given("a primary BoK component and a non-Server activation context containing invalid bootstrap configuration")
      val component = new BokPrimaryComponent()
      val context = ComponentActivationContext(
        new Subsystem("bok-bootstrap-non-server", configuration = _invalid_bootstrap_configuration),
        _invalid_bootstrap_configuration,
        RunMode.Command,
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )

      When("the CNCF activation callback is invoked")
      val result = component.activateC(context)

      Then("the component is opted in and non-Server startup is effect-free")
      component shouldBe a[ComponentActivation]
      result should matchPattern { case Consequence.Success(_) => }
      component.startupBootstrapDiagnostics shouldBe Vector.empty
    }

    "reach the shared bootstrap boundary from the admitted Server callback without an ActionCall" in {
      Given("an admitted primary component with an explicit but unregistered Server bootstrap profile")
      val subsystem = RuntimeBindingAdmissionFixture.admit(
        new Subsystem("bok-bootstrap-server-callback", configuration = _server_bootstrap_configuration)
      )
      val component = new org.simplemodeling.textus.bok.impl.ComponentFactory()
        .create(ComponentCreate(subsystem, ComponentOrigin.Main))
        .primary
        .asInstanceOf[BokPrimaryComponent]
      subsystem.add(Vector(component))
      val context = ComponentActivationContext(
        subsystem,
        _server_bootstrap_configuration,
        RunMode.Server,
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )

      When("the post-assembly Server callback obtains its component runtime context and starts bootstrap")
      val result = component.activateC(context)

      Then("the missing private binding is reported as one sanitized BoK diagnostic rather than an Action dispatch")
      result should matchPattern { case Consequence.Failure(_) => }
      component.startupBootstrapDiagnostics.map(_.profile) shouldBe Vector("official")
      component.startupBootstrapDiagnostics.map(_.category) shouldBe Vector("publication-failed")
    }

    "publish explicitly configured profiles in declaration order and remain default-off when absent" in {
      Given("one coordinator with a deterministic injected publication and an absent component bootstrap configuration")
      val component = new BokPrimaryComponent()
      val context = _context
      val configuration = ResolvedConfiguration(Configuration.empty, ConfigurationTrace.empty)
      val calls = scala.collection.mutable.ArrayBuffer.empty[String]
      val publication: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => {
          calls += key.render
          Consequence.success(_result(key))
        }
      val coordinator = new BokStartupBootstrapCoordinator(
        new BokKnowledgeSourceReplacementService(new BokKnowledgeCatalog()),
        Some(publication)
      )

      When("the default-off callback and then an explicit ordered coordinator run")
      val defaultoff = component.activateC(ComponentActivationContext(
        new Subsystem("bok-bootstrap-default-off", configuration = configuration),
        configuration,
        RunMode.Server,
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      ))
      val configured = coordinator.runC(
        context,
        component,
        Vector(BokProfileKey.development, BokProfileKey.official),
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )

      Then("no implicit profile is selected and configured replacement stays sequential")
      defaultoff should matchPattern { case Consequence.Success(_) => }
      configured should matchPattern { case Consequence.Success(_) => }
      calls.toVector shouldBe Vector("development", "official")
      coordinator.diagnostics.map(_.profile) shouldBe Vector("development", "official")
    }

    "stop after the first failed or non-complete configured publication" in {
      Given("one ordered coordinator whose first result is degraded")
      val component = new BokPrimaryComponent()
      val calls = scala.collection.mutable.ArrayBuffer.empty[String]
      val publication: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => {
          calls += key.render
          Consequence.success(_result(key, state = "degraded"))
        }
      val coordinator = new BokStartupBootstrapCoordinator(
        new BokKnowledgeSourceReplacementService(new BokKnowledgeCatalog()),
        Some(publication)
      )
      val rejectedpublication: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => Consequence.success(_result(key, admitted = Some(false)))
      val rejected = new BokStartupBootstrapCoordinator(
        new BokKnowledgeSourceReplacementService(new BokKnowledgeCatalog()),
        Some(rejectedpublication)
      )

      When("two required profiles are evaluated")
      val result = coordinator.runC(
        _context,
        component,
        Vector(BokProfileKey.official, BokProfileKey.development),
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )
      val rejectedresult = rejected.runC(
        _context,
        component,
        Vector(BokProfileKey.official),
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )

      Then("the structured failure prevents the later profile from starting")
      result should matchPattern { case Consequence.Failure(_) => }
      rejectedresult should matchPattern { case Consequence.Failure(_) => }
      calls.toVector shouldBe Vector("official")
      coordinator.diagnostics.map(_.category) shouldBe Vector("publication-not-complete")
      rejected.diagnostics.map(_.category) shouldBe Vector("admission-rejected")
    }

    "reject missing registry bindings and deadline or cancellation before later publication" in {
      Given("an unconfigured primary component plus terminal deadline and cancellation coordinators")
      val component = new BokPrimaryComponent()
      val service = new BokKnowledgeSourceReplacementService(new BokKnowledgeCatalog())
      val cancellation = new ComponentActivationCancellation
      cancellation.cancel()
      val cancelledcalls = scala.collection.mutable.ArrayBuffer.empty[String]
      val cancelledpublication: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => {
          cancelledcalls += key.render
          Consequence.success(_result(key))
        }
      val expiredcalls = scala.collection.mutable.ArrayBuffer.empty[String]
      val expiredpublication: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => {
          expiredcalls += key.render
          Consequence.success(_result(key))
        }
      val postpublicationcancellation = new ComponentActivationCancellation
      val postpublicationcalls = scala.collection.mutable.ArrayBuffer.empty[String]
      val postpublicationfunction: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => {
          postpublicationcalls += key.render
          postpublicationcancellation.cancel()
          Consequence.success(_result(key))
        }
      val cancelledcoordinator = new BokStartupBootstrapCoordinator(
        service,
        Some(cancelledpublication)
      )
      val expiredcoordinator = new BokStartupBootstrapCoordinator(
        service,
        Some(expiredpublication)
      )
      val postpublicationcoordinator = new BokStartupBootstrapCoordinator(
        service,
        Some(postpublicationfunction)
      )

      When("startup attempts use an absent registry or terminal activation state")
      val missing = service.replaceBootstrap(component, _context, BokProfileKey.official)
      val cancelled = cancelledcoordinator.runC(
        _context,
        component,
        Vector(BokProfileKey.official, BokProfileKey.development),
        Instant.now.plusSeconds(5),
        cancellation
      )
      val expired = expiredcoordinator.runC(
        _context,
        component,
        Vector(BokProfileKey.official, BokProfileKey.development),
        Instant.now.minusSeconds(1),
        new ComponentActivationCancellation
      )
      val postpublication = postpublicationcoordinator.runC(
        _context,
        component,
        Vector(BokProfileKey.official, BokProfileKey.development),
        Instant.now.plusSeconds(5),
        postpublicationcancellation
      )

      Then("each condition is a safe failure before a later profile can start")
      missing should matchPattern { case Consequence.Failure(_) => }
      cancelled should matchPattern { case Consequence.Failure(_) => }
      expired should matchPattern { case Consequence.Failure(_) => }
      postpublication should matchPattern { case Consequence.Failure(_) => }
      cancelledcalls.toVector shouldBe Vector.empty
      expiredcalls.toVector shouldBe Vector.empty
      postpublicationcalls.toVector shouldBe Vector("official")
    }

    "retain bounded safe diagnostics for deterministic repeated replacement" in {
      Given("two identical complete publication outcomes backed by a private-looking source locator")
      val component = new BokPrimaryComponent()
      val calls = scala.collection.mutable.ArrayBuffer.empty[String]
      val publication: (ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult] =
        (_, _, key) => {
          calls += key.render
          Consequence.success(_result(key))
        }
      val coordinator = new BokStartupBootstrapCoordinator(
        new BokKnowledgeSourceReplacementService(new BokKnowledgeCatalog()),
        Some(publication)
      )

      When("the same configured key is replaced twice")
      val first = coordinator.runC(
        _context,
        component,
        Vector(BokProfileKey.official),
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )
      val second = coordinator.runC(
        _context,
        component,
        Vector(BokProfileKey.official),
        Instant.now.plusSeconds(5),
        new ComponentActivationCancellation
      )

      Then("both outcomes are complete and diagnostics contain identities and counts but not the locator")
      first should matchPattern { case Consequence.Success(_) => }
      second should matchPattern { case Consequence.Success(_) => }
      calls.toVector shouldBe Vector("official", "official")
      coordinator.diagnostics.foreach { diagnostic =>
        diagnostic.status shouldBe "complete"
        diagnostic.datasetId shouldBe Some("bootstrap-dataset")
        diagnostic.sourceId shouldBe Some("bootstrap-source")
        diagnostic.generation shouldBe Some("bootstrap-generation")
        diagnostic.toString should not include "private-resource"
        diagnostic.toString should not include "credential"
      }
    }

    "exclude protected mutation from the MCP-ready operation set" in {
      Given("the activated primary component")
      val component = new BokPrimaryComponent()

      When("its MCP operation inventory is projected")
      val operations = component.mcpReadyOperations

      Then("bootstrap has not added replaceKnowledgeSource to MCP")
      operations should not contain "BokRetrieval.replaceKnowledgeSource"
      operations should contain ("BokRetrieval.searchTerms")
    }

    "attribute managed readiness and MCP reads to the selected profile" in {
      Given("a managed server that becomes ready only after complete development publication")
      When("a future MCP read selects profile=development")
      Then("readiness follows completion and the response carries selected profile attribution (P8-C managed acceptance)")
      pending
    }
  }

  private def _context: ExecutionContext = {
    val provider = new InMemoryTextusUrnResourceProvider("bok", _resources)
    val resources = ResourceAccessTestProfile(textusUrnProviders = Vector(provider)).resourceAccess
    ExecutionContext.withResourceAccess(ExecutionContext.create(), resources)
  }

  private def _result(
    key: BokProfileKey,
    state: String = "complete",
    admitted: Option[Boolean] = None
  ): BokKnowledgeSourceReplacementResult = {
    val normalized = BokSourceReader.read(_context, _source).TAKE
    BokKnowledgeSourceReplacementResult(
      Some(key),
      normalized,
      BokFederationPublication(state, Record.empty),
      BokProfileAdmission(admitted = admitted.getOrElse(state == "complete"), attribution = None)
    )
  }

  private def _source: BokKnowledgeSource =
    BokKnowledgeSource(
      BokSourceId("bootstrap-source"),
      BokDatasetId("bootstrap-dataset"),
      BokSourceGeneration("bootstrap-generation"),
      BokResourceReference("urn:textus:bok:private-resource")
    )

  private val _resources = Map(
    "private-resource/metadata/cncf/knowledge-source.json" ->
      """{"schemaVersion":"cncf.knowledge-source.v1","resources":[{"kind":"glossary-terms","href":"metadata/glossary/terms.json"}]}""",
    "private-resource/metadata/glossary/terms.json" ->
      """{"terms":[{"id":"bootstrap:term","title":"Bootstrap","definition_text":"Bounded startup term.","term_type":"concept"}]}"""
  )

  private val _invalid_bootstrap_configuration: ResolvedConfiguration =
    _configuration(ConfigurationValue.StringValue("not-an-object"))

  private val _server_bootstrap_configuration: ResolvedConfiguration = {
    val bootstrap = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(List(ConfigurationValue.StringValue("official")))
    ))
    val bok = ConfigurationValue.ObjectValue(Map(
      "bootstrap" -> bootstrap
    ))
    ResolvedConfiguration(
      Configuration(Map("textus" -> ConfigurationValue.ObjectValue(Map("bok" -> bok)))),
      ConfigurationTrace.empty
    )
  }

  private def _configuration(value: ConfigurationValue): ResolvedConfiguration =
    ResolvedConfiguration(
      Configuration(Map(BokStartupBootstrapConfiguration.configurationKey -> value)),
      ConfigurationTrace.empty
    )

  private def _nested_configuration(selectors: Vector[String]): ResolvedConfiguration = {
    val bootstrap = ConfigurationValue.ObjectValue(Map(
      "profiles" -> ConfigurationValue.ListValue(selectors.toList.map(ConfigurationValue.StringValue.apply))
    ))
    val textus = ConfigurationValue.ObjectValue(Map(
      "bok" -> ConfigurationValue.ObjectValue(Map("bootstrap" -> bootstrap))
    ))
    ResolvedConfiguration(Configuration(Map("textus" -> textus)), ConfigurationTrace.empty)
  }

  private def _failure_projection(
    result: Consequence[?]
  ): Option[(BokProfileResolutionFailure, Option[String])] =
    result match {
      case Consequence.Failure(conclusion) =>
        BokProfileResolutionFailure.from(conclusion).map(_ -> conclusion.status.appStatus)
      case Consequence.Success(_) => None
    }
}
