package org.simplemodeling.textus.bok.impl

import org.simplemodeling.textus.bok.BokComponent
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}

final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    BokPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class BokParticipantFactoryBase extends BokComponent.Factory {
  protected final def component_core(
    name: String,
    componentid: ComponentId
  ): Component.Core =
    spec_create(name, componentid, Vector(BokComponent.BokRetrievalService))
}

final class BokPrimaryComponent extends BokComponent {
  override def mcpReadyServices: Set[String] =
    Set.empty
}

object BokPrimaryFactory extends BokParticipantFactoryBase with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component =
    new BokPrimaryComponent()

  override protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core =
    component_core(BokComponent.name, BokComponent.componentId)
}
