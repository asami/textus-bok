package org.simplemodeling.textus.bok.impl

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.simplemodeling.textus.bok.BokComponent
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId, ComponentInit}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.ComponentSelector
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.{BokFederationPublication, BokFederationPublisher, BokFederationRetriever, BokKnowledgeCatalog, BokProfileAuthorization, BokProfileCompatibilityFilter, BokProfileKey, BokProfileRegistry, BokProfileRegistryConfiguration, BokProfileSelection, BokSourceReader, NormalizedBokSource, ResolvedBokProfile}
import org.simplemodeling.textus.bok.value.*
import org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi

/*
 * @since   Jul. 21, 2026
 *  version Jul. 23, 2026
 * @version Aug. 14, 2026
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

  override protected def initialize_component_c(
    component: Component,
    params: ComponentInit
  ): Consequence[Component] =
    super.initialize_component_c(component, params).flatMap {
      case bok: BokPrimaryComponent =>
        bok.subsystem match {
          case Some(subsystem) =>
            // Bootstrap wiring must inspect the resolved subsystem configuration before ActionCalls exist.
            BokProfileRegistryConfiguration.fromConfiguration(
              subsystem.configuration // cncf-car-lint: ignore -- component initialization boundary
            ).flatMap { configuration =>
              bok.configureProfileRegistry(configuration).map(_ => bok)
            }
          case None => Consequence.stateInvalid("The BoK component subsystem is unavailable during initialization")
        }
      case value => Consequence.componentInvalid(
        new IllegalStateException(s"The BoK factory initialized an incompatible component: ${value.getClass.getName}")
      )
    }

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

    override def createGetKnowledgeMapActionCall(
      core: ActionCall.Core,
      action: GetKnowledgeMapRequest
    ): GetKnowledgeMapActionCall =
      GetKnowledgeMapActionCallImpl(core, action)
  }

  private final case class ReplaceKnowledgeSourceActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.ReplaceKnowledgeSourceRequest
  ) extends BokComponent.BokRetrievalService.ReplaceKnowledgeSourceActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        for {
          source <- _source(action.record)
          component <- _component(core)
          configured <- component.loadReplacementSource(executionContext, source)
          (profilekey, normalized) = configured
          publication <- BokFederationPublisher.replace(core, normalized)
          admitted <- component.admitReplacement(profilekey, publication, normalized)
          _ = if (admitted) _catalog.commit(publication, normalized)
        } yield OperationResponse(ReplaceKnowledgeSourceResponse(
          status = BokQueryStatus(publication.state),
          sourceId = normalized.source.sourceId,
          datasetId = normalized.source.datasetId,
          generation = normalized.source.generation,
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
            _optional_string(action.record, "kind"),
            _optional_string(action.record, "organization")
          ).toRecord())
        }
      }
  }

  private final case class GetKnowledgeMapActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.GetKnowledgeMapRequest
  ) extends BokComponent.BokRetrievalService.GetKnowledgeMapActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        Consequence.success(OperationResponse(_catalog.getKnowledgeMap(
          _optional_string(action.record, "datasetId"),
          _optional_string(action.record, "sourceId"),
          _optional_string(action.record, "category"),
          _optional_string(action.record, "termType"),
          _optional_string(action.record, "focus"),
          action.record.getInt("nodeLimit"),
          action.record.getInt("relationshipLimit")
        ).toRecord()))
      }
  }

  private def _source(record: Record): Consequence[BokKnowledgeSource] =
    record.getAny("source") match {
      case Some(source: BokKnowledgeSource) => Consequence.success(source)
      case Some(source: Record) => BokKnowledgeSource.createC(source)
      case Some(value) => Consequence.valueInvalid(value, org.goldenport.schema.XString)
      case None => Consequence.successOrPropertyNotFound("source", Option.empty[BokKnowledgeSource])
    }

  private def _component(core: ActionCall.Core): Consequence[BokPrimaryComponent] =
    core.component match {
      case Some(component: BokPrimaryComponent) => Consequence.success(component)
      case Some(value) => Consequence.stateInvalid(
        s"The BoK action is bound to an unsupported component: ${value.getClass.getName}"
      )
      case None => Consequence.serviceUnavailable("The BoK component is unavailable")
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
      case value: BokDatasetId => Some(value.value)
      case value: BokSourceId => Some(value.value)
      case value: BokTermType => Some(value.value)
      case value: BokKnowledgeMapFocus => Some(value.value)
      case value: ComponentKind => Some(value.value)
      case value: ComponentOrganization => Some(value.value)
      case value: ComponentVersion => Some(value.value)
      case _ => None
    }
}

final class BokPrimaryComponent extends BokComponent {
  private var _profile_registry: Option[BokProfileRegistry] = None

  private[bok] def configureProfileRegistry(
    configuration: Option[BokProfileRegistryConfiguration]
  ): Consequence[Unit] =
    configuration match {
      case Some(value) => BokProfileRegistry.create(value).map { registry =>
        _profile_registry = Some(registry)
      }
      case None =>
        _profile_registry = None
        Consequence.unit
    }

  private[bok] def loadReplacementSource(
    context: ExecutionContext,
    requested: BokKnowledgeSource
  ): Consequence[(Option[BokProfileKey], NormalizedBokSource)] =
    _profile_registry match {
      case Some(registry) =>
        for {
          key <- registry.configuredKey(requested)
          normalized <- registry.loadConfiguredSource(context, key)
        } yield Some(key) -> normalized
      case None => BokSourceReader.read(context, requested).map(None -> _)
    }

  private[bok] def admitReplacement(
    key: Option[BokProfileKey],
    publication: BokFederationPublication,
    normalized: NormalizedBokSource
  ): Consequence[Boolean] =
    (key, _profile_registry) match {
      case (Some(profilekey), Some(registry)) => registry.admit(profilekey, publication, normalized)
      case (None, None) => Consequence.success(true)
      case _ => Consequence.stateInvalid("The BoK profile registry changed during source replacement")
    }

  private[bok] def resolveProfile(
    selection: BokProfileSelection,
    filter: BokProfileCompatibilityFilter,
    authorization: BokProfileAuthorization
  ): Consequence[ResolvedBokProfile] =
    _profile_registry match {
      case Some(registry) => registry.resolve(selection, filter, authorization)
      case None => BokProfileRegistry.create(BokProfileRegistryConfiguration(Vector.empty)).flatMap(
        _.resolve(selection, filter, authorization)
      )
    }

  private[bok] def semanticIntegrationFederation(
    selector: ComponentSelector = ComponentSelector()
  )(using ExecutionContext): Consequence[SemanticIntegrationFederationApi] =
    semantic_integration_federation(selector)

  override def mcpReadyOperations: Set[String] = Set(
    "BokRetrieval.searchTerms",
    "BokRetrieval.explainTerm",
    "BokRetrieval.searchComponentReferences",
    "BokRetrieval.getComponentReference"
  )
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
