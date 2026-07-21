package org.simplemodeling.textus.bok.impl

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.simplemodeling.textus.bok.BokComponent
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.{BokFederationPublisher, BokFederationRetriever, BokKnowledgeCatalog, BokSourceReader}
import org.simplemodeling.textus.bok.value.*

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
  private val _catalog = new BokKnowledgeCatalog()

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

    override def createSearchTermsActionCall(
      core: ActionCall.Core,
      action: SearchTermsRequest
    ): SearchTermsActionCall =
      SearchTermsActionCallImpl(core, action)

    override def createExplainTermActionCall(
      core: ActionCall.Core,
      action: ExplainTermRequest
    ): ExplainTermActionCall =
      ExplainTermActionCallImpl(core, action)

    override def createSearchComponentReferencesActionCall(
      core: ActionCall.Core,
      action: ComponentReferenceSearchRequest
    ): SearchComponentReferencesActionCall =
      SearchComponentReferencesActionCallImpl(core, action)

    override def createGetComponentReferenceActionCall(
      core: ActionCall.Core,
      action: ComponentReferenceLookupRequest
    ): GetComponentReferenceActionCall =
      GetComponentReferenceActionCallImpl(core, action)
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
          _ = _catalog.commit(publication, normalized)
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

  private final case class SearchTermsActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.SearchTermsRequest
  ) extends BokComponent.BokRetrievalService.SearchTermsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        for {
          query <- _required_string(action.record, "query")
          category = _optional_string(action.record, "category")
          limit = action.record.getInt("limit").getOrElse(10)
          response <- _catalog.searchTerms(query, category, limit) { accepted =>
            BokFederationRetriever.candidateScores(core, query, accepted, limit)
          }
        } yield OperationResponse(response.toRecord())
      }
  }

  private final case class ExplainTermActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.ExplainTermRequest
  ) extends BokComponent.BokRetrievalService.ExplainTermActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        _required_string(action.record, "term").map { query =>
          OperationResponse(_catalog.explainTerm(query).toRecord())
        }
      }
  }

  private final case class SearchComponentReferencesActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.ComponentReferenceSearchRequest
  ) extends BokComponent.BokRetrievalService.SearchComponentReferencesActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        for {
          query <- _required_string(action.record, "query")
          kind = _optional_string(action.record, "kind")
          limit = action.record.getInt("limit").getOrElse(10)
          response <- _catalog.searchComponentReferences(query, kind, limit) { accepted =>
            BokFederationRetriever.candidateScores(core, query, accepted, limit)
          }
        } yield OperationResponse(response.toRecord())
      }
  }

  private final case class GetComponentReferenceActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.ComponentReferenceLookupRequest
  ) extends BokComponent.BokRetrievalService.GetComponentReferenceActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        _required_string(action.record, "name").map { name =>
          OperationResponse(_catalog.getComponentReference(
            name,
            _optional_string(action.record, "version"),
            _optional_string(action.record, "kind")
          ).toRecord())
        }
      }
  }

  private def _source(record: Record): Consequence[BokKnowledgeSource] =
    record.getAny("source") match {
      case Some(source: BokKnowledgeSource) => Consequence.success(source)
      case Some(source: Record) => BokKnowledgeSource.createC(source)
      case Some(value) => Consequence.valueInvalid(value, org.goldenport.schema.XString)
      case None => Consequence.successOrPropertyNotFound("source", Option.empty[BokKnowledgeSource])
    }

  private def _required_string(record: Record, name: String): Consequence[String] =
    record.getAny(name) match {
      case Some(value: String) => Consequence.success(value)
      case Some(value: BokTermSearchText) => Consequence.success(value.value)
      case Some(value: ComponentSearchText) => Consequence.success(value.value)
      case Some(value: ComponentName) => Consequence.success(value.value)
      case Some(value) => Consequence.valueInvalid(value, org.goldenport.schema.XString)
      case None => Consequence.successOrPropertyNotFound(name, Option.empty[String])
    }

  private def _optional_string(record: Record, name: String): Option[String] =
    record.getAny(name).flatMap {
      case value: String => Some(value)
      case value: BokTermCategory => Some(value.value)
      case value: ComponentKind => Some(value.value)
      case value: ComponentVersion => Some(value.value)
      case _ => None
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
