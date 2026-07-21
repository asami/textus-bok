package org.simplemodeling.textus.bok.runtime

import io.circe.parser
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent
import org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi
import org.simplemodeling.textus.semanticintegration.value.{FederationDatasetQueryRequest, FederationDatasetQueryResponse, FederationDatasetQueryResult}

/*
 * Provider-neutral semantic candidate lookup through the generated SIE API.
 *
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
object BokFederationRetriever {
  val maximumProviderResultLimit = 100

  def candidateScores(
    core: ActionCall.Core,
    query: String,
    accepted: Set[BokCandidateKey],
    limit: Int
  ): Consequence[Map[BokCandidateKey, Double]] =
    for {
      api <- _api(core)
      groups <- _sequence(accepted.groupBy(_.sourceId).toVector.sortBy(_._1).map { case (sourceid, keys) =>
        _query_until(api, core, query, sourceid, keys, limit.max(1).min(keys.size), 10)
      })
    } yield groups.flatten.groupMapReduce(_._1)(_._2)(_.max(_))

  private def _api(core: ActionCall.Core): Consequence[SemanticIntegrationFederationApi] =
    core.component match {
      case Some(component: BokPrimaryComponent) =>
        component.semanticIntegrationFederation()(using core.executionContext)
      case Some(component) =>
        Consequence.stateInvalid(s"BoK action is bound to an unsupported component: ${component.getClass.getName}")
      case None =>
        Consequence.serviceUnavailable("BoK component is not available in the current action context")
    }

  private def _query_until(
    api: SemanticIntegrationFederationApi,
    core: ActionCall.Core,
    query: String,
    sourceid: String,
    accepted: Set[BokCandidateKey],
    targetcount: Int,
    providerlimit: Int
  ): Consequence[Vector[(BokCandidateKey, Double)]] =
    for {
      action <- _request(query, sourceid, providerlimit)
      response <- api.queryDataset(action)(using core.executionContext)
      page <- _scores(response)
      scores = page.scores.filter(x => accepted.contains(x._1))
      result <-
        if (
          scores.map(_._1).distinct.size >= targetcount ||
          page.resultCount < providerlimit ||
          providerlimit >= BokFederationRetriever.maximumProviderResultLimit
        )
          Consequence.success(scores)
        else
          _query_until(
            api,
            core,
            query,
            sourceid,
            accepted,
            targetcount,
            (providerlimit * 2).min(BokFederationRetriever.maximumProviderResultLimit)
          )
    } yield result

  private def _request(
    query: String,
    sourceid: String,
    limit: Int
  ): Consequence[FederationDatasetQueryRequest] = {
    val record = Record.dataAuto(
      "text" -> query,
      "sourceId" -> sourceid,
      "limit" -> limit.max(1).min(100)
    )
    FederationDatasetQueryRequest.createC(record)
  }

  private def _scores(response: FederationDatasetQueryResponse): Consequence[QueryPage] =
    _sequence(response.results.map(_score)).map(x => QueryPage(response.results.size, x.flatten))

  private def _score(value: FederationDatasetQueryResult): Consequence[Option[(BokCandidateKey, Double)]] = {
      val candidate = for {
        metadata <- value.metadata.map(_.value)
        json <- _metadata_json(metadata)
        if _metadata_field(json, "domain").contains("bok")
        datasetid <- _metadata_field(json, "datasetId")
      } yield BokCandidateKey(datasetid, value.sourceId.value, value.documentId.value) -> value.score
      Consequence.success(candidate)
  }

  private def _metadata_field(json: io.circe.Json, name: String): Option[String] =
    json.hcursor.get[String](name).toOption.orElse(
      json.hcursor.downField("domain").get[String](name).toOption
    )

  private def _metadata_json(value: String): Option[io.circe.Json] =
    parser.parse(value).toOption.map(_decode_json_string(_, 4))

  private def _decode_json_string(json: io.circe.Json, remaining: Int): io.circe.Json =
    if (remaining <= 0)
      json
    else
      json.asString.flatMap(x => parser.parse(x).toOption) match {
        case Some(decoded) => _decode_json_string(decoded, remaining - 1)
        case None => json
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
