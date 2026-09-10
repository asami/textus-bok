package org.simplemodeling.textus.bok.runtime

import java.time.Instant
import org.goldenport.Consequence
import org.goldenport.cncf.component.ComponentActivationCancellation
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.configuration.{ConfigurationValue, ResolvedConfiguration}
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent
import org.simplemodeling.textus.bok.value.BokKnowledgeSource

/**
 * The private, logical-profile portion of the Phase 8 startup contract.
 *
 * This decoder selects already registered profile keys.  It deliberately does
 * not load a source, publish a generation, or invoke a component lifecycle
 * hook; those responsibilities belong to later Phase 8 slices.
 *
 * @since   Sep. 8, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
private[bok] final case class BokStartupBootstrapConfiguration(
  profiles: Vector[BokProfileKey]
)

private[bok] object BokStartupBootstrapConfiguration {
  val configurationKey = "textus.bok.bootstrap"

  private val _profiles_key = "profiles"

  /**
   * Decodes the optional private bootstrap configuration.
   *
   * Absence is intentionally different from an empty profile list: absence
   * disables bootstrap, while a present empty list is an invalid selection.
   */
  def fromConfiguration(
    configuration: ResolvedConfiguration
  ): Consequence[Option[BokStartupBootstrapConfiguration]] =
    _configuration_value(configuration) match {
      case Consequence.Success(None) => Consequence.success(None)
      case Consequence.Success(Some(ConfigurationValue.ObjectValue(values))) =>
        if (values.keySet != Set(_profiles_key))
          _invalid_selection(
            s"The BoK startup bootstrap configuration must contain only '$_profiles_key'"
          )
        else
          createC(_record(values)).map(Some(_))
      case Consequence.Success(Some(_)) =>
        _invalid_selection("The BoK startup bootstrap configuration must be an object")
      case Consequence.Failure(conclusion) => Consequence.Failure(conclusion)
    }

  /**
   * Decodes the object body while retaining declaration order in `profiles`.
   */
  def createC(record: Record): Consequence[BokStartupBootstrapConfiguration] =
    if (record.keySet != Set(_profiles_key))
      _invalid_selection(
        s"The BoK startup bootstrap configuration must contain only '$_profiles_key'"
      )
    else
      record.getVector(_profiles_key) match {
        case Some(values) if values.nonEmpty =>
          _sequence(values.map(_profile_key)).flatMap(_unique)
            .map(BokStartupBootstrapConfiguration.apply)
        case Some(_) =>
          _invalid_selection(
            s"The BoK startup bootstrap $_profiles_key collection must not be empty"
          )
        case None =>
          _invalid_selection(
            s"The BoK startup bootstrap $_profiles_key collection is required"
          )
      }

  private def _profile_key(value: Any): Consequence[BokProfileKey] =
    value match {
      case "official" => BokProfileSelection.official.normalize
      case "development" => BokProfileSelection.development.normalize
      case selector: String if selector.startsWith("project:") =>
        BokProfileSelection.project(selector.drop("project:".length)).normalize
      case selector: String if selector.trim.isEmpty =>
        _invalid_selection("A BoK startup bootstrap profile selector must not be blank")
      case selector: String =>
        _invalid_selection(s"Unknown BoK startup bootstrap profile '$selector'")
      case _ =>
        _invalid_selection("A BoK startup bootstrap profile selector must be a string")
    }

  private def _unique(
    keys: Vector[BokProfileKey]
  ): Consequence[Vector[BokProfileKey]] = {
    val duplicate = keys.groupBy(identity).collectFirst {
      case (key, occurrences) if occurrences.size > 1 => key
    }
    duplicate match {
      case Some(key) =>
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.Ambiguous,
          Some(key),
          s"The BoK startup bootstrap profile ${key.render} is declared more than once"
        )
      case None => Consequence.success(keys)
    }
  }

  private def _sequence[A](values: Vector[Consequence[A]]): Consequence[Vector[A]] =
    Consequence.zipN(values).map(_.toVector)

  private def _configuration_value(
    configuration: ResolvedConfiguration
  ): Consequence[Option[ConfigurationValue]] = {
    val values = configuration.configuration.values
    val direct = values.get(configurationKey)
    val nested = _lookup(values, configurationKey.split('.').toList)
    (direct, nested) match {
      case (Some(_), Some(_)) =>
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.Ambiguous,
          None,
          s"The $configurationKey configuration is defined more than once"
        )
      case (Some(value), None) => Consequence.success(Some(value))
      case (None, Some(value)) => Consequence.success(Some(value))
      case (None, None) => Consequence.success(None)
    }
  }

  private def _lookup(
    values: Map[String, ConfigurationValue],
    path: List[String]
  ): Option[ConfigurationValue] =
    path match {
      case Nil => None
      case head :: Nil => values.get(head)
      case head :: tail => values.get(head) match {
        case Some(ConfigurationValue.ObjectValue(children)) => _lookup(children, tail)
        case _ => None
      }
    }

  private def _record(values: Map[String, ConfigurationValue]): Record =
    Record.dataAuto(values.toVector.map { case (name, value) =>
      name -> _record_value(value)
    }*)

  private def _record_value(value: ConfigurationValue): Any =
    value match {
      case ConfigurationValue.StringValue(x) => x
      case ConfigurationValue.NumberValue(x) => x
      case ConfigurationValue.BooleanValue(x) => x
      case ConfigurationValue.ListValue(xs) => xs.toVector.map(_record_value)
      case ConfigurationValue.ObjectValue(xs) => _record(xs)
      case ConfigurationValue.NullValue => null
    }

  private def _invalid_selection[A](message: String): Consequence[A] =
    BokProfileResolutionFailure.failure(
      BokProfileResolutionFailure.InvalidSelection,
      None,
      message
    )
}

/**
 * The single BoK replacement authority shared by the protected manual
 * operation and configured startup.  The two callers differ only in how the
 * configured source is selected; publication, registry admission, and local
 * catalog visibility have exactly one implementation.
 */
private[bok] final class BokKnowledgeSourceReplacementService(
  catalog: BokKnowledgeCatalog
) {
  def replaceManual(
    component: BokPrimaryComponent,
    context: ExecutionContext,
    requested: BokKnowledgeSource
  ): Consequence[BokKnowledgeSourceReplacementResult] =
    component.loadReplacementSource(context, requested).flatMap { case (key, normalized) =>
      _replace(component, context, key, normalized)
    }

  def replaceBootstrap(
    component: BokPrimaryComponent,
    context: ExecutionContext,
    key: BokProfileKey
  ): Consequence[BokKnowledgeSourceReplacementResult] =
    component.loadConfiguredReplacementSource(context, key).flatMap { normalized =>
      _replace(component, context, Some(key), normalized)
    }

  private def _replace(
    component: BokPrimaryComponent,
    context: ExecutionContext,
    key: Option[BokProfileKey],
    normalized: NormalizedBokSource
  ): Consequence[BokKnowledgeSourceReplacementResult] =
    for {
      publication <- component.completeReplacement(normalized)(using context)
      admission <- component.admitReplacementWithAttribution(key, publication, normalized)
      _ = commitIfAdmitted(admission, publication, normalized)
    } yield BokKnowledgeSourceReplacementResult(key, normalized, publication, admission)

  private[bok] def commitIfAdmitted(
    admission: BokProfileAdmission,
    publication: BokFederationPublication,
    normalized: NormalizedBokSource
  ): Boolean =
    admission.admitted && catalog.commit(publication, normalized)
}

private[bok] final case class BokKnowledgeSourceReplacementResult(
  profileKey: Option[BokProfileKey],
  normalized: NormalizedBokSource,
  publication: BokFederationPublication,
  admission: BokProfileAdmission
)

/**
 * Bounded operator-safe startup state.  It intentionally retains neither a
 * resource locator, configuration/source body, Conclusion, nor Throwable.
 */
private[bok] final case class BokStartupBootstrapDiagnostic(
  profile: String,
  projectId: Option[String],
  datasetId: Option[String],
  sourceId: Option[String],
  generation: Option[String],
  status: String,
  category: String,
  termCount: Int,
  componentCount: Int,
  topologyNodeCount: Int,
  topologyRelationshipCount: Int,
  warnings: Vector[String],
  attribution: Option[ResolvedBokProfile]
)

/**
 * Sequential profile coordinator for one activation callback.  CNCF owns
 * once-only lifecycle coordination; this object deliberately has no global or
 * per-process state.
 */
private[bok] final class BokStartupBootstrapCoordinator(
  replacementService: BokKnowledgeSourceReplacementService,
  injectedPublication: Option[(ExecutionContext, BokPrimaryComponent, BokProfileKey) => Consequence[BokKnowledgeSourceReplacementResult]] = None
) {
  private val _maximum_warnings = 16
  private val _maximum_warning_length = 240
  private var _diagnostics = Vector.empty[BokStartupBootstrapDiagnostic]

  def diagnostics: Vector[BokStartupBootstrapDiagnostic] = synchronized(_diagnostics)

  def runC(
    context: ExecutionContext,
    component: BokPrimaryComponent,
    profiles: Vector[BokProfileKey],
    deadline: Instant,
    cancellation: ComponentActivationCancellation
  ): Consequence[Unit] =
    _run(context, component, profiles, deadline, cancellation, 0)

  private def _run(
    context: ExecutionContext,
    component: BokPrimaryComponent,
    profiles: Vector[BokProfileKey],
    deadline: Instant,
    cancellation: ComponentActivationCancellation,
    index: Int
  ): Consequence[Unit] =
    if (index >= profiles.size)
      Consequence.unit
    else {
      val key = profiles(index)
      _terminal_category(deadline, cancellation) match {
        case Some(category) =>
          _record(_failure_diagnostic(key, category))
          _failure(category)
        case None =>
          val publisher = injectedPublication.getOrElse(
            (ctx, target, profile) => _publish(ctx, target, profile)
          )
          val publication = try {
            Some(publisher(context, component, key))
          } catch {
            case _: Throwable =>
              _record(_failure_diagnostic(key, "unexpected-callback-failure"))
              None
          }
          publication match {
            case None =>
              _failure("unexpected-callback-failure")
            case Some(Consequence.Failure(_)) =>
              _record(_failure_diagnostic(key, "publication-failed"))
              _failure("publication-failed")
            case Some(Consequence.Success(result)) =>
              _terminal_category(deadline, cancellation) match {
                case Some(category) =>
                  _record(_diagnostic(result, category, category))
                  _failure(category)
                case None if _normalize(result.publication.state) != "complete" =>
                  _record(_diagnostic(result, _normalize(result.publication.state), "publication-not-complete"))
                  _failure("publication-not-complete")
                case None if !result.admission.admitted =>
                  _record(_diagnostic(result, "complete", "admission-rejected"))
                  _failure("admission-rejected")
                case None =>
                  _record(_diagnostic(result, "complete", "complete"))
                  _run(context, component, profiles, deadline, cancellation, index + 1)
              }
          }
      }
    }

  private def _publish(
    context: ExecutionContext,
    component: BokPrimaryComponent,
    key: BokProfileKey
  ): Consequence[BokKnowledgeSourceReplacementResult] =
    replacementService.replaceBootstrap(component, context, key)

  private def _terminal_category(
    deadline: Instant,
    cancellation: ComponentActivationCancellation
  ): Option[String] =
    if (cancellation.isCancelled)
      Some("cancelled")
    else if (!Instant.now.isBefore(deadline))
      Some("deadline-exceeded")
    else
      None

  private def _record(diagnostic: BokStartupBootstrapDiagnostic): Unit = synchronized {
    _diagnostics = _diagnostics :+ diagnostic
  }

  private def _diagnostic(
    result: BokKnowledgeSourceReplacementResult,
    status: String,
    category: String
  ): BokStartupBootstrapDiagnostic = {
    val normalized = result.normalized
    val key = result.profileKey
    BokStartupBootstrapDiagnostic(
      profile = key.map(_.kind.selectionName).getOrElse("manual"),
      projectId = key.flatMap(_.projectId.map(_.value)),
      datasetId = Some(normalized.source.datasetId.value),
      sourceId = Some(normalized.source.sourceId.value),
      generation = Some(normalized.source.generation.value),
      status = status,
      category = category,
      termCount = normalized.terms.size,
      componentCount = normalized.components.size,
      topologyNodeCount = normalized.topology.nodes.size,
      topologyRelationshipCount = normalized.topology.relationships.size,
      warnings = normalized.warnings.take(_maximum_warnings).map(x => _bounded(x.value)),
      attribution = result.admission.attribution
    )
  }

  private def _failure_diagnostic(
    key: BokProfileKey,
    category: String
  ): BokStartupBootstrapDiagnostic =
    BokStartupBootstrapDiagnostic(
      profile = key.kind.selectionName,
      projectId = key.projectId.map(_.value),
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

  private def _bounded(value: String): String =
    value.take(_maximum_warning_length)

  private def _normalize(value: String): String =
    value.trim.toLowerCase

  private def _failure[A](category: String): Consequence[A] =
    Consequence.stateInvalid(s"BoK startup bootstrap failed: $category")
}
