package org.simplemodeling.textus.bok

import java.nio.file.Path

import org.goldenport.cncf.subsystem.GenericSubsystemDescriptor
import org.goldenport.configuration.{ConfigurationTrace, ResolvedConfiguration}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.bok.runtime.*

final class BokCodexSarProfileBindingSpec
  extends AnyWordSpec
    with Matchers
    with GivenWhenThen {
  "The canonical BoK SAR profile binding" should {
    "decode its official binding from the real descriptor configuration" in {
      Given("the canonical BoK SAR descriptor")
      val descriptorpath = Path.of(
        "examples",
        "bok-codex-sar",
        "subsystem-descriptor.yaml"
      ).toAbsolutePath.normalize
      val descriptor = GenericSubsystemDescriptor.load(descriptorpath).toOption
        .getOrElse(fail(s"canonical SAR descriptor did not load: $descriptorpath"))
      val configuration = ResolvedConfiguration(descriptor.configuration, ConfigurationTrace.empty)

      When("the BoK profile registry decodes the descriptor's public typed configuration")
      val registry = BokProfileRegistryConfiguration.fromConfiguration(configuration).toOption.flatten
        .getOrElse(fail("canonical SAR profile registry did not decode"))

      Then("exactly one official binding retains the stipulated source and evidence identities")
      registry.bindings should have size 1
      val official = registry.bindings.head
      official.key shouldBe BokProfileKey.official
      official.source.sourceId.value shouldBe "urn:textus:bok:packaged-demo"
      official.source.datasetId.value shouldBe "urn:textus:bok:packaged-demo"
      official.source.generation.value shouldBe "packaged-car-sar-v1"
      official.source.resource.value shouldBe "file:///workspace/bok-fixture/"
      official.evidence.uri.value shouldBe "urn:textus:bok:packaged-demo:evidence:packaged-car-sar-v1"
      official.evidence.sourceId shouldBe official.source.sourceId
    }
  }
}
