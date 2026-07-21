package org.simplemodeling.textus.bok

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ComponentFactorySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "ComponentFactory" should {
    "expose a primary component factory" in {
      Given("a newly constructed generated component bundle factory")
      val factory = new impl.ComponentFactory()

      When("the primary factory is requested")
      val primary = factory.primaryFactory

      Then("the generated primary component boundary is available")
      primary should not be null
    }
  }
}
