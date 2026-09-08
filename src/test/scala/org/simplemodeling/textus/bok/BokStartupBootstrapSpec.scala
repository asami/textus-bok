package org.simplemodeling.textus.bok

import org.goldenport.Consequence
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.runtime.*

/**
 * Executable specification for the private Phase 8 bootstrap selector
 * boundary.  Runtime activation and publication remain pending until their
 * separately owned Phase 8 slices.
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
    "activate eligible components in final admitted order exactly once" in {
      Given("a complete managed server assembly with multiple opted-in components")
      When("the post-assembly activation coordinator is introduced")
      Then("callbacks run sequentially in admitted order once per assembly (P8-A activation-ordering/once-only acceptance)")
      pending
    }

    "publish configured profiles only when explicitly enabled" in {
      Given("an absent configuration and an explicitly ordered configured profile list")
      When("managed startup reaches the future bootstrap hook")
      Then("absence remains default-off and configured profiles publish through the shared authority (P8-A configured-publication/default-off acceptance)")
      pending
    }

    "fail readiness for invalid degraded or failed startup publication" in {
      Given("invalid configuration and complete, degraded, or failed publication outcomes")
      When("the future bootstrap startup gate evaluates required results")
      Then("invalid or non-complete required work prevents readiness with a structured failure (P8-A invalid/degraded/failed-startup acceptance)")
      pending
    }

    "share publication semantics with protected manual replacement" in {
      Given("a configured bootstrap publication and the protected replaceKnowledgeSource operation")
      When("both future paths publish a complete generation")
      Then("one replacement authority preserves atomicity and manual mutation remains outside MCP (P8-A shared-publication acceptance)")
      pending
    }

    "replace an unchanged generation deterministically across restart" in {
      Given("the same complete profile input presented to two managed server assemblies")
      When("the future bootstrap runs after restart")
      Then("the replacement is deterministic and creates no duplicate records (P8-A restart-replacement acceptance)")
      pending
    }

    "emit bounded safe startup diagnostics" in {
      Given("a bootstrap result containing safe identity and count fields")
      When("the future activation or publication path reports its outcome")
      Then("diagnostics allow safe profile and generation attribution but redact locators credentials and raw exceptions (P8-A diagnostics acceptance)")
      pending
    }

    "exclude mutation from the MCP projection" in {
      Given("the protected replaceKnowledgeSource operation and the existing qualified MCP read tools")
      When("the future bootstrap and MCP projections are assembled")
      Then("bootstrap does not add an MCP mutation surface and replaceKnowledgeSource remains absent (P8-A MCP mutation-exclusion acceptance)")
      pending
    }

    "attribute managed readiness and MCP reads to the selected profile" in {
      Given("a managed server that becomes ready only after complete development publication")
      When("a future MCP read selects profile=development")
      Then("readiness follows completion and the response carries selected profile attribution (P8-A readiness/MCP attribution acceptance)")
      pending
    }
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
