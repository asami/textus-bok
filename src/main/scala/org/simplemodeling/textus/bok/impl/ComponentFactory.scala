package org.simplemodeling.textus.bok.impl

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.simplemodeling.textus.bok.BokComponent
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.datatype.BokQueryStatus
import org.simplemodeling.textus.bok.runtime.{BokFederationPublisher, BokSourceReader}
import org.simplemodeling.textus.bok.value.{BokKnowledgeSource, ReplaceKnowledgeSourceResponse}

/*
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */

final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    BokPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class BokParticipantFactoryBase extends BokComponent.Factory {
  override val BokRetrieval: BokComponent.BokRetrievalServiceFactory =
    new BokRetrievalServiceFactoryImpl()

  protected final def component_core(
    name: String,
    componentid: ComponentId
  ): Component.Core =
    spec_create(name, componentid, Vector(BokComponent.BokRetrievalService))

  private final class BokRetrievalServiceFactoryImpl
    extends BokComponent.BokRetrievalServiceFactory {
    import BokComponent.BokRetrievalService.*

    override def createReplaceKnowledgeSourceActionCall(
      core: ActionCall.Core,
      action: ReplaceKnowledgeSourceRequest
    ): ReplaceKnowledgeSourceActionCall =
      ReplaceKnowledgeSourceActionCallImpl(core, action)
  }

  private final case class ReplaceKnowledgeSourceActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.ReplaceKnowledgeSourceRequest
  ) extends BokComponent.BokRetrievalService.ReplaceKnowledgeSourceActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        for {
          source <- _source(action.record)
          normalized <- BokSourceReader.read(executionContext, source)
          publication <- BokFederationPublisher.replace(core, normalized)
        } yield OperationResponse(ReplaceKnowledgeSourceResponse(
          status = BokQueryStatus(publication.state),
          sourceId = source.sourceId,
          datasetId = source.datasetId,
          generation = source.generation,
          termCount = normalized.terms.size,
          componentCount = normalized.components.size,
          warnings = normalized.warnings
        ).toRecord())
      }
  }

  private def _source(record: Record): Consequence[BokKnowledgeSource] =
    record.getAny("source") match {
      case Some(source: BokKnowledgeSource) => Consequence.success(source)
      case Some(source: Record) => BokKnowledgeSource.createC(source)
      case Some(value) => Consequence.valueInvalid(value, org.goldenport.schema.XString)
      case None => Consequence.successOrPropertyNotFound("source", Option.empty[BokKnowledgeSource])
    }
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
