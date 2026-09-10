package org.simplemodeling.textus.bok.runtime

import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.configuration.{ConfigurationValue, ResolvedConfiguration}
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent

/*
 * Private completion boundary for the selected BoK knowledge backend.
 *
 * @since   Sep. 10, 2026
 * @version Sep. 10, 2026
 * @author  ASAMI, Tomoharu
 */
private[bok] sealed trait BokKnowledgeBackend {
  def complete(
    component: BokPrimaryComponent,
    normalized: NormalizedBokSource
  )(using context: ExecutionContext): Consequence[BokFederationPublication]
}

private[bok] object BokKnowledgeBackend {
  case object InMemory extends BokKnowledgeBackend {
    override def complete(
      component: BokPrimaryComponent,
      normalized: NormalizedBokSource
    )(using context: ExecutionContext): Consequence[BokFederationPublication] =
      Consequence.success(BokFederationPublication("complete", Record.empty))
  }

  case object Sie extends BokKnowledgeBackend {
    override def complete(
      component: BokPrimaryComponent,
      normalized: NormalizedBokSource
    )(using context: ExecutionContext): Consequence[BokFederationPublication] = {
      val publication = try {
        BokFederationPublisher.replace(component, normalized)
      } catch {
        case _: Throwable => _unavailable[BokFederationPublication]
      }
      publication.recoverWith(_ => _unavailable[BokFederationPublication])
    }

    private def _unavailable[A]: Consequence[A] =
      BokProfileResolutionFailure.failure(
        BokProfileResolutionFailure.Unavailable,
        None,
        "The selected SIE BoK backend is unavailable"
      )
  }
}

private[bok] object BokKnowledgeBackendConfiguration {
  val configurationKey = "textus.bok.knowledge-backend"

  private val _backend_key = "backend"

  def fromConfiguration(
    configuration: ResolvedConfiguration
  ): Consequence[BokKnowledgeBackend] =
    _configuration_value(configuration) match {
      case Consequence.Success(None) => Consequence.success(BokKnowledgeBackend.InMemory)
      case Consequence.Success(Some(ConfigurationValue.ObjectValue(values))) =>
        if (values.keySet != Set(_backend_key))
          _invalid_configuration(
            s"The $configurationKey configuration must contain only '$_backend_key'"
          )
        else
          values.get(_backend_key) match {
            case Some(ConfigurationValue.StringValue("memory")) =>
              Consequence.success(BokKnowledgeBackend.InMemory)
            case Some(ConfigurationValue.StringValue("sie")) =>
              Consequence.success(BokKnowledgeBackend.Sie)
            case Some(ConfigurationValue.StringValue(value)) =>
              _invalid_configuration(s"Unknown BoK knowledge backend '$value'")
            case Some(_) =>
              _invalid_configuration("The BoK knowledge backend must be a string")
            case None =>
              _invalid_configuration("The BoK knowledge backend is required")
          }
      case Consequence.Success(Some(_)) =>
        _invalid_configuration(s"The $configurationKey configuration must be an object")
      case Consequence.Failure(conclusion) => Consequence.Failure(conclusion)
    }

  private def _configuration_value(
    configuration: ResolvedConfiguration
  ): Consequence[Option[ConfigurationValue]] = {
    val values = configuration.configuration.values
    val direct = values.get(configurationKey)
    val nested = _lookup(values, configurationKey.split('.').toList)
    (direct, nested) match {
      case (Some(_), Some(_)) =>
        BokProfileResolutionFailure.failure(
          BokProfileResolutionFailure.InvalidSelection,
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

  private def _invalid_configuration[A](message: String): Consequence[A] =
    BokProfileResolutionFailure.failure(
      BokProfileResolutionFailure.InvalidSelection,
      None,
      message
    )
}
