package org.simplemodeling.textus.bok.impl

import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.simplemodeling.textus.bok.BokComponent
import org.goldenport.cncf.component.{Component, ComponentActivation, ComponentActivationContext, ComponentCreate, ComponentId, ComponentInit}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.spi.ComponentSelector
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.runtime.{BokFederationPublication, BokFederationRetriever, BokKnowledgeBackend, BokKnowledgeBackendConfiguration, BokKnowledgeCatalog, BokKnowledgeSourceReplacementService, BokProfileAdmission, BokProfileAuthorization, BokProfileCompatibilityFilter, BokProfileKey, BokProfileRegistry, BokProfileRegistryConfiguration, BokProfileResolutionFailure, BokProfileSelection, BokSemanticAccess, BokSourceReader, BokStartupBootstrapConfiguration, BokStartupBootstrapCoordinator, BokStartupBootstrapDiagnostic, NormalizedBokSource, ResolvedBokProfile, ResolvedBokSelection}
import org.simplemodeling.textus.bok.value.*
import org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi

/*
 * @since   Jul. 21, 2026
 *  version Jul. 23, 2026
 * @version Sep. 11, 2026
 * @author  ASAMI, Tomoharu
 */

final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    BokPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class BokParticipantFactoryBase extends BokComponent.Factory {
  protected val catalog = new BokKnowledgeCatalog()
  protected val replacement_service = new BokKnowledgeSourceReplacementService(catalog)

  override protected def initialize_component_c(
    component: Component,
    params: ComponentInit
  ): Consequence[Component] =
    super.initialize_component_c(component, params).flatMap {
      case bok: BokPrimaryComponent =>
        bok.subsystem match {
          case Some(subsystem) =>
            // Bootstrap wiring must inspect the resolved subsystem configuration before ActionCalls exist.
            BokKnowledgeBackendConfiguration.fromConfiguration(
              subsystem.configuration // cncf-car-lint: ignore -- component initialization boundary
            ).flatMap { backend =>
              bok.configureKnowledgeBackend(backend).flatMap { _ =>
                BokProfileRegistryConfiguration.fromConfiguration(
                  subsystem.configuration // cncf-car-lint: ignore -- component initialization boundary
                ).flatMap { configuration =>
                  bok.configureProfileRegistry(configuration).map(_ => bok)
                }
              }
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
    componentId: ComponentId
  ): Component.Core =
    spec_create(name, componentId, Vector(BokComponent.BokRetrievalService))

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

    override def createSearchSemanticKnowledgeActionCall(
      core: ActionCall.Core,
      action: SemanticKnowledgeSearchRequest
    ): SearchSemanticKnowledgeActionCall =
      SearchSemanticKnowledgeActionCallImpl(core, action)

    override def createGetSemanticManifestActionCall(
      core: ActionCall.Core,
      action: SemanticManifestRequest
    ): GetSemanticManifestActionCall =
      GetSemanticManifestActionCallImpl(core, action)

    override def createGetSemanticResourceActionCall(
      core: ActionCall.Core,
      action: SemanticResourceRequest
    ): GetSemanticResourceActionCall =
      GetSemanticResourceActionCallImpl(core, action)

    override def createGetSemanticSectionActionCall(
      core: ActionCall.Core,
      action: SemanticSectionRequest
    ): GetSemanticSectionActionCall =
      GetSemanticSectionActionCallImpl(core, action)

    override def createDiscoverSemanticKnowledgeActionCall(
      core: ActionCall.Core,
      action: SemanticKnowledgeDiscoveryRequest
    ): DiscoverSemanticKnowledgeActionCall =
      DiscoverSemanticKnowledgeActionCallImpl(core, action)
  }

  private final case class SearchSemanticKnowledgeActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.SemanticKnowledgeSearchRequest
  ) extends BokComponent.BokRetrievalService.SearchSemanticKnowledgeActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      for {
        query <- _required_string(action.record, "query")
        resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
        response <- catalog.searchSemanticKnowledge(
          BokSemanticAccess(resolved, BokSemanticAccess.CallerPrivilege.Public),
          query,
          action.record.getInt("limit").getOrElse(10)
        )(_ => Consequence.success(Map.empty))
      } yield OperationResponse(response.toRecord())
    }
  }

  private final case class GetSemanticManifestActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.SemanticManifestRequest
  ) extends BokComponent.BokRetrievalService.GetSemanticManifestActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      for {
        identity <- _required_string(action.record, "identity")
        resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
      } yield OperationResponse(catalog.getSemanticManifest(BokSemanticAccess(resolved, BokSemanticAccess.CallerPrivilege.Public), identity).toRecord())
    }
  }

  private final case class GetSemanticResourceActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.SemanticResourceRequest
  ) extends BokComponent.BokRetrievalService.GetSemanticResourceActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      for {
        identity <- _required_string(action.record, "identity")
        resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
      } yield OperationResponse(catalog.getSemanticResource(BokSemanticAccess(resolved, BokSemanticAccess.CallerPrivilege.Public), identity).toRecord())
    }
  }

  private final case class GetSemanticSectionActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.SemanticSectionRequest
  ) extends BokComponent.BokRetrievalService.GetSemanticSectionActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      for {
        documentid <- _required_string(action.record, "documentId")
        sectionid <- _required_string(action.record, "sectionId")
        resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
      } yield OperationResponse(catalog.getSemanticSection(BokSemanticAccess(resolved, BokSemanticAccess.CallerPrivilege.Public), documentid, sectionid).toRecord())
    }
  }

  private final case class DiscoverSemanticKnowledgeActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.SemanticKnowledgeDiscoveryRequest
  ) extends BokComponent.BokRetrievalService.DiscoverSemanticKnowledgeActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = exec_from {
      for {
        query <- _required_string(action.record, "query")
        resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
      } yield OperationResponse(catalog.discoverSemanticKnowledge(
        BokSemanticAccess(resolved, BokSemanticAccess.CallerPrivilege.Public),
        query,
        action.record.getInt("limit").getOrElse(10)
      ).toRecord())
    }
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
          replacement <- component.replacementService.replaceManual(component, executionContext, source)
          normalized = replacement.normalized
          publication = replacement.publication
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
          resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
          response <- catalog.searchTerms(resolved, query, category, limit) { accepted =>
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
        for {
          query <- _required_string(action.record, "term")
          resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
        } yield OperationResponse(catalog.explainTerm(resolved, query).toRecord())
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
          resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
          response <- catalog.searchComponentReferences(resolved, query, kind, limit) { accepted =>
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
        for {
          name <- _required_string(action.record, "name")
          resolved <- _resolve_selection(core, action.record, BokProfileCompatibilityFilter())
        } yield OperationResponse(catalog.getComponentReference(
            resolved,
            name,
            _optional_string(action.record, "version"),
            _optional_string(action.record, "kind"),
            _optional_string(action.record, "organization")
          ).toRecord())
      }
  }

  private final case class GetKnowledgeMapActionCallImpl(
    core: ActionCall.Core,
    override val action: BokComponent.BokRetrievalService.GetKnowledgeMapRequest
  ) extends BokComponent.BokRetrievalService.GetKnowledgeMapActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      exec_from {
        for {
          resolved <- _resolve_selection(
            core,
            action.record,
            BokProfileCompatibilityFilter(
              _optional_string(action.record, "datasetId"),
              _optional_string(action.record, "sourceId")
            )
          )
        } yield OperationResponse(catalog.getKnowledgeMap(
          resolved,
          _optional_string(action.record, "category"),
          _optional_string(action.record, "termType"),
          _optional_string(action.record, "focus"),
          action.record.getInt("nodeLimit"),
          action.record.getInt("relationshipLimit")
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
      case value: BokProfile => Some(value.value)
      case value: BokProjectId => Some(value.value)
      case value: BokDatasetId => Some(value.value)
      case value: BokSourceId => Some(value.value)
      case value: BokTermType => Some(value.value)
      case value: BokKnowledgeMapFocus => Some(value.value)
      case value: ComponentKind => Some(value.value)
      case value: ComponentOrganization => Some(value.value)
      case value: ComponentVersion => Some(value.value)
      case _ => None
    }

  private def _resolve_selection(
    core: ActionCall.Core,
    record: Record,
    filter: BokProfileCompatibilityFilter
  ): Consequence[ResolvedBokSelection] =
    for {
      selection <- _profile_selection(record)
      authorization <- _selection_authorization(selection)
      component <- _component(core)
      resolved <- component.resolveSelection(
        selection,
        filter,
        authorization
      )
    } yield resolved

  private def _profile_selection(record: Record): Consequence[BokProfileSelection] =
    for {
      profile <- _selection_value(record, "profile")
      projectid <- _selection_value(record, "projectId")
      catalogid <- _selection_value(record, "catalogId")
    } yield BokProfileSelection(profile, projectid, catalogid)

  private def _selection_authorization(
    selection: BokProfileSelection
  ): Consequence[BokProfileAuthorization] =
    if (selection.isCatalog)
      selection.catalogIdC.map(BokProfileAuthorization.allowCatalog)
    else
      selection.normalize.map(key => BokProfileAuthorization.allow(key))

  private def _selection_value(record: Record, name: String): Consequence[Option[String]] =
    record.getAny(name) match {
      case None => Consequence.success(None)
      case Some(value: String) => Consequence.success(Some(value))
      case Some(value: BokProfile) => Consequence.success(Some(value.value))
      case Some(value: BokProjectId) => Consequence.success(Some(value.value))
      case Some(value: BokCatalogId) => Consequence.success(Some(value.value))
      case Some(_) => BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.InvalidSelection,
        None,
        s"The BoK $name selection value is invalid"
      )
    }
}

final class BokPrimaryComponent(
  private val _catalog: BokKnowledgeCatalog = new BokKnowledgeCatalog(),
  injectedReplacementService: Option[BokKnowledgeSourceReplacementService] = None
) extends BokComponent with ComponentActivation {
  private var _knowledge_backend: BokKnowledgeBackend = BokKnowledgeBackend.InMemory
  private var _profile_registry: Option[BokProfileRegistry] = None
  private val _replacement_service = injectedReplacementService.getOrElse(
    new BokKnowledgeSourceReplacementService(_catalog)
  )
  private var _startup_bootstrap_diagnostics = Vector.empty[BokStartupBootstrapDiagnostic]

  private[bok] def replacementService: BokKnowledgeSourceReplacementService =
    _replacement_service

  private[bok] def configureKnowledgeBackend(
    backend: BokKnowledgeBackend
  ): Consequence[Unit] = {
    _knowledge_backend = backend
    Consequence.unit
  }

  private[bok] def completeReplacement(
    normalized: NormalizedBokSource
  )(using context: ExecutionContext): Consequence[BokFederationPublication] =
    _knowledge_backend.complete(this, normalized)

  private[bok] def hasSelectedTopology(datasetId: String): Boolean =
    _catalog.selectedTopology(datasetId).nonEmpty

  private[bok] def startupBootstrapDiagnostics: Vector[BokStartupBootstrapDiagnostic] = synchronized {
    _startup_bootstrap_diagnostics
  }

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

  private[bok] def loadConfiguredReplacementSource(
    context: ExecutionContext,
    key: BokProfileKey
  ): Consequence[NormalizedBokSource] =
    _profile_registry match {
      case Some(registry) => registry.loadConfiguredSource(context, key)
      case None => BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Unregistered,
        Some(key),
        "BoK startup bootstrap requires a private registered profile"
      )
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

  private[bok] def admitReplacementWithAttribution(
    key: Option[BokProfileKey],
    publication: BokFederationPublication,
    normalized: NormalizedBokSource
  ): Consequence[BokProfileAdmission] =
    (key, _profile_registry) match {
      case (Some(profilekey), Some(registry)) =>
        registry.admitWithAttribution(profilekey, publication, normalized)
      case (None, None) => Consequence.success(BokProfileAdmission(true, None))
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

  private[bok] def resolveSelection(
    selection: BokProfileSelection,
    filter: BokProfileCompatibilityFilter,
    authorization: BokProfileAuthorization
  ): Consequence[ResolvedBokSelection] =
    _profile_registry match {
      case Some(registry) => registry.resolveSelection(selection, filter, authorization)
      case None => BokProfileRegistry.create(BokProfileRegistryConfiguration(Vector.empty)).flatMap(
        _.resolveSelection(selection, filter, authorization)
      )
    }

  private[bok] def semanticIntegrationFederation(
    selector: ComponentSelector = ComponentSelector()
  )(using ExecutionContext): Consequence[SemanticIntegrationFederationApi] =
    semantic_integration_federation(selector)

  override def activateC(context: ComponentActivationContext): Consequence[Unit] =
    if (context.runMode != RunMode.Server)
      Consequence.unit
    else
      BokStartupBootstrapConfiguration.fromConfiguration(context.configuration) match {
        case Consequence.Success(None) =>
          _set_startup_bootstrap_diagnostics(Vector.empty)
          Consequence.unit
        case Consequence.Success(Some(configuration)) =>
          val coordinator = new BokStartupBootstrapCoordinator(_replacement_service)
          val result = try {
            coordinator.runC(
              logic.executionContext(),
              this,
              configuration.profiles,
              context.deadline,
              context.cancellation
            )
          } catch {
            case _: Throwable =>
              Consequence.stateInvalid("BoK startup bootstrap failed: unexpected-callback-failure")
          }
          val diagnostics = coordinator.diagnostics
          _set_startup_bootstrap_diagnostics(
            if (diagnostics.nonEmpty) diagnostics
            else result match {
              case Consequence.Failure(_) => Vector(_startup_failure_diagnostic("unexpected-callback-failure"))
              case Consequence.Success(_) => Vector.empty
            }
          )
          result
        case Consequence.Failure(_) =>
          _set_startup_bootstrap_diagnostics(Vector(_configuration_failure_diagnostic))
          Consequence.stateInvalid("BoK startup bootstrap failed: configuration")
      }

  private def _set_startup_bootstrap_diagnostics(
    diagnostics: Vector[BokStartupBootstrapDiagnostic]
  ): Unit = synchronized {
    _startup_bootstrap_diagnostics = diagnostics
  }

  private def _configuration_failure_diagnostic: BokStartupBootstrapDiagnostic =
    _startup_failure_diagnostic("configuration")

  private def _startup_failure_diagnostic(category: String): BokStartupBootstrapDiagnostic =
    BokStartupBootstrapDiagnostic(
      profile = "startup",
      projectId = None,
      datasetId = None,
      sourceId = None,
      generation = None,
      status = "failed",
      category = category,
      termCount = 0,
      componentCount = 0,
      topologyNodeCount = 0,
      topologyRelationshipCount = 0,
      warnings = Vector.empty,
      attribution = None
    )

  override def mcpReadyOperations: Set[String] = Set(
    "BokRetrieval.searchTerms",
    "BokRetrieval.explainTerm",
    "BokRetrieval.searchComponentReferences",
    "BokRetrieval.getComponentReference",
    "BokRetrieval.getKnowledgeMap",
    "BokRetrieval.searchSemanticKnowledge",
    "BokRetrieval.getSemanticManifest",
    "BokRetrieval.getSemanticResource",
    "BokRetrieval.getSemanticSection",
    "BokRetrieval.discoverSemanticKnowledge"
  )
}

object BokPrimaryFactory extends BokParticipantFactoryBase with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component =
    new BokPrimaryComponent(catalog, Some(replacement_service))

  override protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core =
    component_core(BokComponent.name, BokComponent.componentId)
}
