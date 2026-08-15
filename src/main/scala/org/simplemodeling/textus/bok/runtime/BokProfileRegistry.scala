package org.simplemodeling.textus.bok.runtime

import org.goldenport.{Conclusion, Consequence}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.configuration.{ConfigurationValue, ResolvedConfiguration}
import org.goldenport.observation.{Descriptor, Taxonomy}
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.value.{BokEvidence, BokKnowledgeSource}

/*
 * Private deterministic bindings from BoK profile identities to admitted generations.
 *
 * @since   Aug. 14, 2026
 * @version Aug. 15, 2026
 * @author  ASAMI, Tomoharu
 */
private[bok] enum BokProfileKind(val selectionName: String) {
  case Official extends BokProfileKind("official")
  case Development extends BokProfileKind("development")
  case Project extends BokProfileKind("project")
}

private[bok] final case class BokProjectId(value: String)

private[bok] final case class BokProfileKey(
  kind: BokProfileKind,
  projectId: Option[BokProjectId]
) {
  def render: String =
    projectId.fold(kind.selectionName)(x => s"${kind.selectionName}:${x.value}")
}

private[bok] object BokProfileKey {
  val official: BokProfileKey = BokProfileKey(BokProfileKind.Official, None)
  val development: BokProfileKey = BokProfileKey(BokProfileKind.Development, None)

  def project(projectId: String): Consequence[BokProfileKey] =
    BokProfileSelection.project(projectId).normalize
}

private[bok] final case class BokProfileSelection(
  profile: Option[String] = None,
  projectId: Option[String] = None
) {
  def normalize: Consequence[BokProfileKey] =
    BokProfileSelection.normalize(this)
}

private[bok] object BokProfileSelection {
  private val _project_id_pattern = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}".r

  val official: BokProfileSelection = BokProfileSelection(Some("official"), None)
  val development: BokProfileSelection = BokProfileSelection(Some("development"), None)

  def project(projectId: String): BokProfileSelection =
    BokProfileSelection(Some("project"), Some(projectId))

  def normalize(selection: BokProfileSelection): Consequence[BokProfileKey] = {
    val profile = selection.profile.getOrElse("official")
    profile match {
      case "official" =>
        _without_project_identity(BokProfileKey.official, selection.projectId)
      case "development" =>
        _without_project_identity(BokProfileKey.development, selection.projectId)
      case "project" =>
        selection.projectId match {
          case Some(projectid) if _valid_project_id(projectid) =>
            Consequence.success(BokProfileKey(BokProfileKind.Project, Some(BokProjectId(projectid))))
          case _ =>
            BokProfileResolutionFailure.failure(
              BokProfileResolutionFailure.ProjectIdentityRequired,
              None,
              "The project profile requires one explicit logical projectId"
            )
        }
      case _ =>
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.InvalidSelection,
          None,
          s"Unknown BoK profile '$profile'"
        )
    }
  }

  private def _without_project_identity(
    key: BokProfileKey,
    projectid: Option[String]
  ): Consequence[BokProfileKey] =
    projectid match {
      case None => Consequence.success(key)
      case Some(_) =>
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.ConflictingSelection,
          Some(key),
          s"The ${key.kind.selectionName} profile does not accept projectId"
        )
    }

  private def _valid_project_id(projectid: String): Boolean =
    projectid == projectid.trim && _project_id_pattern.matches(projectid)
}

private[bok] final case class BokProfileCompatibilityFilter(
  datasetId: Option[String] = None,
  sourceId: Option[String] = None
)

private[bok] enum BokProfileFreshnessPolicy {
  case AnyComplete
  case ExactGeneration(generation: BokSourceGeneration)

  def accepts(generation: BokSourceGeneration): Boolean = this match {
    case AnyComplete => true
    case ExactGeneration(expected) => expected == generation
  }
}

private[bok] final class BokProfileBinding private (
  val key: BokProfileKey,
  val source: BokKnowledgeSource,
  val evidence: BokEvidence,
  val freshnessPolicy: BokProfileFreshnessPolicy
)

private[bok] object BokProfileBinding {
  def official(
    source: BokKnowledgeSource,
    evidence: BokEvidence,
    freshnessPolicy: BokProfileFreshnessPolicy = BokProfileFreshnessPolicy.AnyComplete
  ): Consequence[BokProfileBinding] =
    _create(BokProfileKey.official, source, evidence, freshnessPolicy)

  def development(
    source: BokKnowledgeSource,
    evidence: BokEvidence,
    freshnessPolicy: BokProfileFreshnessPolicy = BokProfileFreshnessPolicy.AnyComplete
  ): Consequence[BokProfileBinding] =
    _create(BokProfileKey.development, source, evidence, freshnessPolicy)

  def project(
    projectId: String,
    source: BokKnowledgeSource,
    evidence: BokEvidence,
    freshnessPolicy: BokProfileFreshnessPolicy = BokProfileFreshnessPolicy.AnyComplete
  ): Consequence[BokProfileBinding] =
    for {
      key <- BokProfileKey.project(projectId)
      binding <- _create(key, source, evidence, freshnessPolicy)
    } yield binding

  def createC(record: Record): Consequence[BokProfileBinding] = {
    for {
      selection <- _selection(record)
      key <- selection.normalize
      source <- _source(record)
      evidence <- _evidence(record)
      freshness <- _freshness(record)
      binding <- _create(key, source, evidence, freshness)
    } yield binding
  }

  private def _create(
    key: BokProfileKey,
    source: BokKnowledgeSource,
    evidence: BokEvidence,
    freshnesspolicy: BokProfileFreshnessPolicy
  ): Consequence[BokProfileBinding] =
    if (evidence.sourceId == source.sourceId)
      Consequence.success(new BokProfileBinding(key, source, evidence, freshnesspolicy))
    else
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.ConflictingSelection,
        Some(key),
        "Profile evidence sourceId must equal the configured logical sourceId"
      )

  private def _selection(record: Record): Consequence[BokProfileSelection] =
    for {
      profile <- _optional_string(record, "profile")
      projectid <- _optional_string(record, "projectId")
    } yield BokProfileSelection(profile, projectid)

  private def _source(record: Record): Consequence[BokKnowledgeSource] =
    record.getAny("source") match {
      case Some(source: BokKnowledgeSource) => Consequence.success(source)
      case Some(source: Record) => BokKnowledgeSource.createC(source).recoverWith { _ =>
        _invalid_configuration("The BoK profile source descriptor is invalid")
      }
      case Some(_) => _invalid_configuration("The BoK profile source descriptor is invalid")
      case None => _invalid_configuration("The BoK profile source descriptor is required")
    }

  private def _evidence(record: Record): Consequence[BokEvidence] =
    record.getAny("evidence") match {
      case Some(evidence: BokEvidence) => Consequence.success(evidence)
      case Some(evidence: Record) => BokEvidence.createC(evidence).recoverWith { _ =>
        _invalid_configuration("The BoK profile evidence descriptor is invalid")
      }
      case Some(_) => _invalid_configuration("The BoK profile evidence descriptor is invalid")
      case None => _invalid_configuration("The BoK profile evidence descriptor is required")
    }

  private def _freshness(record: Record): Consequence[BokProfileFreshnessPolicy] =
    record.getAny("freshnessGeneration") match {
      case None => Consequence.success(BokProfileFreshnessPolicy.AnyComplete)
      case Some(value: String) if value.nonEmpty =>
        Consequence.success(BokProfileFreshnessPolicy.ExactGeneration(BokSourceGeneration(value)))
      case Some(value: BokSourceGeneration) =>
        Consequence.success(BokProfileFreshnessPolicy.ExactGeneration(value))
      case Some(_) => _invalid_configuration("The BoK profile freshnessGeneration is invalid")
    }

  private def _optional_string(record: Record, name: String): Consequence[Option[String]] =
    record.getAny(name) match {
      case None => Consequence.success(None)
      case Some(value: String) => Consequence.success(Some(value))
      case Some(_) => _invalid_configuration(s"The BoK profile $name is invalid")
    }

  private def _invalid_configuration[A](message: String): Consequence[A] =
    BokProfileResolutionFailure.failure(
      BokProfileResolutionFailure.InvalidSelection,
      None,
      message
    )
}

private[bok] final case class BokProfileRegistryConfiguration(
  bindings: Vector[BokProfileBinding]
)

private[bok] object BokProfileRegistryConfiguration {
  val configurationKey = "textus.bok.profile-registry"

  def createC(record: Record): Consequence[BokProfileRegistryConfiguration] =
    record.getVector("profiles") match {
      case Some(values) =>
        _sequence(values.map {
          case binding: Record => BokProfileBinding.createC(binding)
          case _ => _invalid_configuration[BokProfileBinding]("A BoK profile binding is invalid")
        }).map(BokProfileRegistryConfiguration.apply)
      case None => _invalid_configuration("The BoK profile-registry profiles collection is required")
    }

  def fromConfiguration(
    configuration: ResolvedConfiguration
  ): Consequence[Option[BokProfileRegistryConfiguration]] =
    _configuration_value(configuration) match {
      case Consequence.Success(None) => Consequence.success(None)
      case Consequence.Success(Some(ConfigurationValue.ObjectValue(values))) =>
        createC(_record(values)).map(Some(_))
      case Consequence.Success(Some(_)) =>
        _invalid_configuration("The BoK profile-registry configuration must be an object")
      case Consequence.Failure(conclusion) => Consequence.Failure(conclusion)
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

  private def _invalid_configuration[A](message: String): Consequence[A] =
    BokProfileResolutionFailure.failure(
      BokProfileResolutionFailure.InvalidSelection,
      None,
      message
    )
}

private[bok] final case class BokProfileAuthorization(
  allowedKeys: Set[BokProfileKey]
) {
  def allows(key: BokProfileKey): Boolean =
    allowedKeys.contains(key)
}

private[bok] object BokProfileAuthorization {
  def allow(keys: BokProfileKey*): BokProfileAuthorization =
    BokProfileAuthorization(keys.toSet)
}

private[bok] final case class ResolvedBokProfile(
  resolvedProfile: String,
  projectId: Option[String],
  datasetId: BokDatasetId,
  sourceId: BokSourceId,
  generation: BokSourceGeneration,
  evidence: BokEvidence
)

private[bok] enum BokProfileResolutionFailure(
  val code: String,
  val taxonomy: Taxonomy
) {
  case InvalidSelection extends BokProfileResolutionFailure(
    "invalid-selection",
    Taxonomy(Taxonomy.Category.Argument, Taxonomy.Symptom.Invalid)
  )
  case ProjectIdentityRequired extends BokProfileResolutionFailure(
    "project-identity-required",
    Taxonomy(Taxonomy.Category.Argument, Taxonomy.Symptom.Missing)
  )
  case Unregistered extends BokProfileResolutionFailure(
    "unregistered",
    Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.NotFound)
  )
  case Unavailable extends BokProfileResolutionFailure(
    "unavailable",
    Taxonomy(Taxonomy.Category.Resource, Taxonomy.Symptom.Unavailable)
  )
  case Stale extends BokProfileResolutionFailure(
    "stale",
    Taxonomy(Taxonomy.Category.Resource, Taxonomy.Symptom.Invalid)
  )
  case Ambiguous extends BokProfileResolutionFailure(
    "ambiguous",
    Taxonomy(Taxonomy.Category.Configuration, Taxonomy.Symptom.Duplicate)
  )
  case Unauthorized extends BokProfileResolutionFailure(
    "unauthorized",
    Taxonomy(Taxonomy.Category.Security, Taxonomy.Symptom.PermissionDenied)
  )
  case ConflictingSelection extends BokProfileResolutionFailure(
    "conflicting-selection",
    Taxonomy(Taxonomy.Category.Argument, Taxonomy.Symptom.Conflict)
  )
}

private[bok] object BokProfileResolutionFailure {
  private val _codes = BokProfileResolutionFailure.values.map(x => x.code -> x).toMap

  def failure[A](
    failure: BokProfileResolutionFailure,
    key: Option[BokProfileKey],
    message: String
  ): Consequence[A] = {
    val keyfacets = key.toVector.map(x => Descriptor.Facet.Key(x.render))
    val conclusion = Consequence.fail(
      failure.taxonomy,
      message,
      Descriptor.Facet.Reason(failure.code) +: keyfacets
    ).conclusion
    Consequence.Failure(
      conclusion.copy(status = conclusion.status.copy(appStatus = Some(failure.code)))
    )
  }

  def from(conclusion: Conclusion): Option[BokProfileResolutionFailure] =
    conclusion.observation.cause.descriptor.facets.collectFirst {
      case Descriptor.Facet.Reason(code) if _codes.contains(code) => _codes(code)
    }
}

private[bok] final class BokProfileRegistry private (
  initialBindings: Map[BokProfileKey, BokProfileBinding]
) {
  private final case class AdmittedGeneration(
    datasetid: BokDatasetId,
    sourceid: BokSourceId,
    generation: BokSourceGeneration,
    evidence: BokEvidence
  )

  private final case class Entry(
    binding: BokProfileBinding,
    admitted: Option[AdmittedGeneration]
  )

  private var _entries = initialBindings.view.mapValues(x => Entry(x, None)).toMap

  def configure(configuration: BokProfileRegistryConfiguration): Consequence[Unit] = synchronized {
    BokProfileRegistry.validatedBindings(configuration).map { bindings =>
      _entries = bindings.map { case (key, binding) =>
        val admitted = _entries.get(key).flatMap { current =>
          Option.when(_same_logical_source(current.binding, binding))(current.admitted).flatten
        }
        key -> Entry(binding, admitted)
      }
    }
  }

  def loadConfiguredSource(
    context: ExecutionContext,
    key: BokProfileKey
  ): Consequence[NormalizedBokSource] =
    _binding(key).flatMap { binding =>
      BokSourceReader.read(context, binding.source).recoverWith { _ =>
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.Unavailable,
          Some(key),
          s"The configured resource for BoK profile ${key.render} is unavailable"
        )
      }
    }

  def configuredKey(source: BokKnowledgeSource): Consequence[BokProfileKey] = synchronized {
    val matches = _entries.toVector.collect {
      case (key, entry) if _same_requested_source(entry.binding.source, source) => key
    }.sortBy(_.render)
    matches match {
      case Vector(key) => Consequence.success(key)
      case Vector() => BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Unregistered,
        None,
        "The requested BoK source identity has no private profile binding"
      )
      case _ => BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Ambiguous,
        matches.headOption,
        "The requested BoK source identity matches more than one private profile binding"
      )
    }
  }

  def admit(
    key: BokProfileKey,
    publication: BokFederationPublication,
    normalized: NormalizedBokSource
  ): Consequence[Boolean] = synchronized {
    _entry(key).flatMap { entry =>
      if (_normalize(publication.state) != "complete")
        Consequence.success(false)
      else if (entry.binding.source != normalized.source)
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.ConflictingSelection,
          Some(key),
          "The admitted generation does not match the currently configured logical source"
        )
      else {
        val source = normalized.source
        val admitted = AdmittedGeneration(
          source.datasetId,
          source.sourceId,
          source.generation,
          entry.binding.evidence
        )
        _entries = _entries.updated(key, entry.copy(admitted = Some(admitted)))
        Consequence.success(true)
      }
    }
  }

  def resolve(
    selection: BokProfileSelection,
    filter: BokProfileCompatibilityFilter,
    authorization: BokProfileAuthorization
  ): Consequence[ResolvedBokProfile] =
    for {
      key <- selection.normalize
      _ <- _authorize(key, authorization)
      entry <- _entry(key)
      admitted <- _admitted(key, entry)
      _ <- _fresh(key, entry.binding, admitted)
      _ <- _compatible(key, filter, admitted)
    } yield ResolvedBokProfile(
      key.kind.selectionName,
      key.projectId.map(_.value),
      admitted.datasetid,
      admitted.sourceid,
      admitted.generation,
      admitted.evidence
    )

  private def _binding(key: BokProfileKey): Consequence[BokProfileBinding] =
    _entry(key).map(_.binding)

  private def _entry(key: BokProfileKey): Consequence[Entry] = synchronized {
    _entries.get(key) match {
      case Some(entry) => Consequence.success(entry)
      case None => BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Unregistered,
        Some(key),
        s"No private BoK profile binding is registered for ${key.render}"
      )
    }
  }

  private def _authorize(
    key: BokProfileKey,
    authorization: BokProfileAuthorization
  ): Consequence[Unit] =
    if (authorization.allows(key))
      Consequence.unit
    else
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Unauthorized,
        Some(key),
        s"The caller is not authorized for BoK profile ${key.render}"
      )

  private def _admitted(
    key: BokProfileKey,
    entry: Entry
  ): Consequence[AdmittedGeneration] =
    entry.admitted match {
      case Some(admitted) => Consequence.success(admitted)
      case None => BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Unavailable,
        Some(key),
        s"BoK profile ${key.render} has no admitted complete generation"
      )
    }

  private def _fresh(
    key: BokProfileKey,
    binding: BokProfileBinding,
    admitted: AdmittedGeneration
  ): Consequence[Unit] =
    if (binding.freshnessPolicy.accepts(admitted.generation))
      Consequence.unit
    else
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Stale,
        Some(key),
        s"The retained complete generation for ${key.render} does not satisfy its freshness policy"
      )

  private def _compatible(
    key: BokProfileKey,
    filter: BokProfileCompatibilityFilter,
    admitted: AdmittedGeneration
  ): Consequence[Unit] = {
    val datasetmatches = filter.datasetId.forall(_ == admitted.datasetid.value)
    val sourcematches = filter.sourceId.forall(_ == admitted.sourceid.value)
    if (datasetmatches && sourcematches)
      Consequence.unit
    else
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.ConflictingSelection,
        Some(key),
        s"The datasetId or sourceId filter conflicts with BoK profile ${key.render}"
      )
  }

  private def _same_logical_source(
    current: BokProfileBinding,
    replacement: BokProfileBinding
  ): Boolean =
    current.source.datasetId == replacement.source.datasetId &&
      current.source.sourceId == replacement.source.sourceId

  private def _same_requested_source(
    configured: BokKnowledgeSource,
    requested: BokKnowledgeSource
  ): Boolean =
    configured.datasetId == requested.datasetId &&
      configured.sourceId == requested.sourceId &&
      configured.generation == requested.generation

  private def _normalize(value: String): String =
    value.trim.toLowerCase
}

private[bok] object BokProfileRegistry {
  def create(
    configuration: BokProfileRegistryConfiguration
  ): Consequence[BokProfileRegistry] =
    validatedBindings(configuration).map(new BokProfileRegistry(_))

  def createC(record: Record): Consequence[BokProfileRegistry] =
    BokProfileRegistryConfiguration.createC(record).flatMap(create)

  private[runtime] def validatedBindings(
    configuration: BokProfileRegistryConfiguration
  ): Consequence[Map[BokProfileKey, BokProfileBinding]] = {
    val duplicatekeys = configuration.bindings.groupBy(_.key).collect {
      case (key, bindings) if bindings.size > 1 => key
    }.toVector.sortBy(_.render)
    val duplicatedatasets = configuration.bindings.groupBy(_.source.datasetId.value).collect {
      case (datasetid, bindings) if bindings.map(_.key).distinct.size > 1 => datasetid
    }.toVector.sorted
    if (duplicatekeys.nonEmpty)
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Ambiguous,
        duplicatekeys.headOption,
        s"Duplicate private BoK profile bindings: ${duplicatekeys.map(_.render).mkString(", ")}"
      )
    else if (duplicatedatasets.nonEmpty)
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.ConflictingSelection,
        None,
        s"A BoK dataset cannot claim more than one profile: ${duplicatedatasets.mkString(", ")}"
      )
    else
      Consequence.success(configuration.bindings.map(x => x.key -> x).toMap)
  }
}
