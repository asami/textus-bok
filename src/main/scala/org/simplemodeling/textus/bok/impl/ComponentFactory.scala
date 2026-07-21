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
  protected final val shared_services =
    Vector(
      BokComponent.BokRetrievalService,
      BokComponent.AggregateService,
      BokComponent.ViewService,
      BokComponent.EntityService
    )

  protected final def component_core(
    name: String,
    componentid: ComponentId
  ): Component.Core =
    spec_create(name, componentid, shared_services)

  override val BokRetrieval: BokComponent.BokRetrievalServiceFactory = DefaultBokRetrievalServiceFactory()
  override val aggregate: BokComponent.AggregateServiceFactory = AggregateServiceFactoryImpl()
  override val view: BokComponent.ViewServiceFactory = ViewServiceFactoryImpl()
  override val entity: BokComponent.EntityServiceFactory = DefaultEntityServiceFactory()
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

final class DefaultBokRetrievalServiceFactory extends BokComponent.BokRetrievalServiceFactory {
  import BokComponent.BokRetrievalService.*

  override def createReplaceKnowledgeSourceActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: ReplaceKnowledgeSource
  ): ReplaceKnowledgeSourceActionCall =
    ReplaceKnowledgeSourceActionCall(core, action)

  override def createSearchTermsActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: SearchTerms
  ): SearchTermsActionCall =
    SearchTermsActionCall(core, action)
  }

object DefaultBokRetrievalServiceFactory {
  def apply(): DefaultBokRetrievalServiceFactory = new DefaultBokRetrievalServiceFactory()
  }

final class DefaultEntityServiceFactory extends BokComponent.EntityServiceFactory

object DefaultEntityServiceFactory {
  def apply(): DefaultEntityServiceFactory = new DefaultEntityServiceFactory()
  }

final class AggregateServiceFactoryImpl extends BokComponent.AggregateServiceFactory

object AggregateServiceFactoryImpl {
  def apply(): AggregateServiceFactoryImpl = new AggregateServiceFactoryImpl()
}

final class ViewServiceFactoryImpl extends BokComponent.ViewServiceFactory

object ViewServiceFactoryImpl {
  def apply(): ViewServiceFactoryImpl = new ViewServiceFactoryImpl()
}
