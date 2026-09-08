package org.simplemodeling.textus.bok.runtime

import org.goldenport.Consequence
import org.goldenport.configuration.{ConfigurationValue, ResolvedConfiguration}
import org.goldenport.record.Record

/**
 * The private, logical-profile portion of the Phase 8 startup contract.
 *
 * This decoder selects already registered profile keys.  It deliberately does
 * not load a source, publish a generation, or invoke a component lifecycle
 * hook; those responsibilities belong to later Phase 8 slices.
 *
 * @since   Sep. 8, 2026
 * @version Sep. 8, 2026
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
