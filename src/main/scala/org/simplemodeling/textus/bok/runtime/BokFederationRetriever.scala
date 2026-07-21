package org.simplemodeling.textus.bok.runtime

import io.circe.parser
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.component.Component
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.semanticintegration.SemanticIntegrationEngineComponent
import org.simplemodeling.textus.semanticintegration.SemanticIntegrationEngineComponent.SemanticRetrievalService

/*
 * Provider-neutral semantic candidate lookup through the generated SIE API.
 *
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
object BokFederationRetriever {
  val MaximumProviderResultLimit = 100

  private val _service_name = "SemanticRetrieval"
  private val _operation_name = "query"

  def candidateScores(
    core: ActionCall.Core,
    query: String,
    accepted: Set[BokCandidateKey],
    limit: Int
  ): Consequence[Map[BokCandidateKey, Double]] =
    for {
      target <- _target(core)
      groups <- _sequence(accepted.groupBy(_.sourceId).toVector.sortBy(_._1).map { case (sourceid, keys) =>
        _query_until(target, core, query, sourceid, keys, limit.max(1).min(keys.size), 10)
      })
    } yield groups.flatten.groupMapReduce(_._1)(_._2)(_.max(_))

  private def _target(core: ActionCall.Core): Consequence[Component] =
    core.component.flatMap(_.subsystem).flatMap { subsystem =>
      subsystem.components.find(_.name.equalsIgnoreCase(SemanticIntegrationEngineComponent.name))
    } match {
      case Some(component) => Consequence.success(component)
      case None => Consequence.serviceUnavailable(
        s"${SemanticIntegrationEngineComponent.name} component is not available in the current subsystem"
      )
    }

  private def _query_until(
    target: Component,
    core: ActionCall.Core,
    query: String,
    sourceid: String,
    accepted: Set[BokCandidateKey],
    targetcount: Int,
    providerlimit: Int
  ): Consequence[Vector[(BokCandidateKey, Double)]] =
    for {
      action <- _request(target, query, sourceid, providerlimit)
      response <- target.logic.executeAction(action, core.executionContext)
      page <- _scores(response)
      scores = page.scores.filter(x => accepted.contains(x._1))
      result <-
        if (
          scores.map(_._1).distinct.size >= targetcount ||
          page.resultCount < providerlimit ||
          providerlimit >= BokFederationRetriever.MaximumProviderResultLimit
        )
          Consequence.success(scores)
        else
          _query_until(
            target,
            core,
            query,
            sourceid,
            accepted,
            targetcount,
            (providerlimit * 2).min(BokFederationRetriever.MaximumProviderResultLimit)
          )
    } yield result

  private def _request(
    target: Component,
    query: String,
    sourceid: String,
    limit: Int
  ): Consequence[SemanticRetrievalService.SemanticQueryRequest] = {
    val record = Record.dataAuto(
      "text" -> query,
      "sourceId" -> sourceid,
      "limit" -> limit.max(1).min(100),
      "includeRdf" -> false,
      "includeKnowledgeFrame" -> false,
      "registerKnowledgeSpace" -> false
    )
    val request = Request.of(
      component = target.name,
      service = _service_name,
      operation = _operation_name,
      properties = record.fields.map(x => Property(x.key, x.value.single, None)).toList
    )
    SemanticRetrievalService.SemanticQueryRequest.create(request)
  }

  private def _scores(response: OperationResponse): Consequence[QueryPage] =
    response match {
      case OperationResponse.RecordResponse(record) =>
        record.getAny("results") match {
          case None => Consequence.success(QueryPage(0, Vector.empty))
          case Some(values: Iterable[?]) =>
            _sequence(values.toVector.map(_score)).map(x => QueryPage(values.size, x.flatten))
          case Some(value) => _score(value).map(x => QueryPage(1, x.toVector))
        }
      case other =>
        Consequence.stateInvalid(s"SIE semantic query returned an unsupported response: ${other.getClass.getName}")
    }

  private def _score(value: Any): Consequence[Option[(BokCandidateKey, Double)]] = value match {
    case record: Record =>
      val candidate = for {
        documentid <- record.getString("documentId")
        sourceid <- record.getString("sourceId")
        score <- record.getDouble("score")
        metadata <- record.getString("metadata")
        json <- parser.parse(metadata).toOption
        if json.hcursor.get[String]("domain").toOption.contains("bok")
        datasetid <- json.hcursor.get[String]("datasetId").toOption
      } yield BokCandidateKey(datasetid, sourceid, documentid) -> score
      Consequence.success(candidate)
    case other =>
      Consequence.stateInvalid(s"SIE semantic query returned a non-record result: ${other.getClass.getName}")
  }

  private def _sequence[A](values: Vector[Consequence[A]]): Consequence[Vector[A]] =
    values.foldLeft(Consequence.success(Vector.empty[A])) { (result, value) =>
      for {
        collected <- result
        element <- value
      } yield collected :+ element
    }
}

final case class BokCandidateKey(
  datasetId: String,
  sourceId: String,
  documentId: String
)

private final case class QueryPage(
  resultCount: Int,
  scores: Vector[(BokCandidateKey, Double)]
)
