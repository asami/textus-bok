package org.simplemodeling.textus.bok.runtime

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.record.Record
import org.simplemodeling.textus.bok.impl.BokPrimaryComponent
import org.simplemodeling.textus.bok.value.{BokEvidence, BokTerm, ComponentReference}
import org.simplemodeling.textus.semanticintegration.api.SemanticIntegrationFederationApi
import org.simplemodeling.textus.semanticintegration.value.{FederationDatasetReplacementRequest, FederationDatasetResponse}

/*
 * BoK-owned projection into the provider-neutral SIE component contract.
 *
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final case class BokFederationPublication(
  state: String,
  record: Record
)

object BokFederationPublisher {
  private val _term_predicate = "urn:textus:bok:predicate:definition"
  private val _component_predicate = "urn:textus:bok:predicate:kind"

  def replace(
    core: ActionCall.Core,
    normalized: NormalizedBokSource
  ): Consequence[BokFederationPublication] =
    for {
      api <- _api(core)
      request <- replacementRequest(normalized)
      response <- api.replaceDataset(request)(using core.executionContext)
      publication <- _publication(response)
    } yield publication

  private[bok] def termDocumentId(term: BokTerm): String =
    _stable_id("bok-term-document", term.termId.value)

  private[bok] def componentDocumentId(component: ComponentReference): String =
    _stable_id("bok-component-document", component.kind.value, component.name.value)

  private def _api(core: ActionCall.Core): Consequence[SemanticIntegrationFederationApi] =
    core.component match {
      case Some(component: BokPrimaryComponent) =>
        component.semanticIntegrationFederation()(using core.executionContext)
      case Some(component) =>
        Consequence.stateInvalid(s"BoK action is bound to an unsupported component: ${component.getClass.getName}")
      case None =>
        Consequence.serviceUnavailable("BoK component is not available in the current action context")
    }

  private[bok] def replacementRequest(
    normalized: NormalizedBokSource
  ): Consequence[FederationDatasetReplacementRequest] = {
    val evidences = _evidences(normalized)
    val evidenceids = evidences.map { case (evidence, id) => _evidence_key(evidence) -> id }.toMap
    val documents = normalized.terms.map(_term_document(_, evidenceids)) ++
      normalized.components.map(_component_document(normalized.source.datasetId.value, _, evidenceids))
    val assertions = normalized.terms.map(_term_assertion(_, evidenceids)) ++
      normalized.components.map(_component_assertion(_, evidenceids))
    val record = Record.dataAuto(
      "datasetId" -> normalized.source.datasetId.value,
      "sourceId" -> normalized.source.sourceId.value,
      "generation" -> normalized.source.generation.value,
      "documents" -> documents,
      "assertions" -> assertions,
      "evidence" -> evidences.map { case (evidence, id) => _evidence_record(evidence, id) }
    )
    FederationDatasetReplacementRequest.createC(record)
  }

  private def _evidences(
    normalized: NormalizedBokSource
  ): Vector[(BokEvidence, String)] =
    (normalized.terms.map(_.evidence) ++ normalized.components.map(_.evidence))
      .groupBy(_evidence_key)
      .toVector
      .sortBy(_._1)
      .map { case (_, values) =>
        val evidence = values.head
        evidence -> _stable_id("bok-evidence", _evidence_key(evidence))
      }

  private def _term_document(
    term: BokTerm,
    evidenceids: Map[String, String]
  ): Record =
    Record.dataAuto(
      "id" -> termDocumentId(term),
      "sourceId" -> term.evidence.sourceId.value,
      "title" -> term.title.value,
      "uri" -> term.evidence.uri.value,
      "content" -> term.definition.value,
      "mediaType" -> "text/plain",
      "metadata" -> Json.obj(
        "domain" -> Json.fromString("bok"),
        "recordKind" -> Json.fromString("term"),
        "termId" -> Json.fromString(term.termId.value),
        "termType" -> Json.fromString(term.termType.value),
        "category" -> term.category.map(x => Json.fromString(x.value)).getOrElse(Json.Null),
        "datasetId" -> Json.fromString(term.datasetId.value)
      ).noSpaces,
      "evidenceIds" -> Vector(evidenceids(_evidence_key(term.evidence)))
    )

  private def _term_assertion(
    term: BokTerm,
    evidenceids: Map[String, String]
  ): Record =
    Record.dataAuto(
      "id" -> _stable_id("bok-term-assertion", term.termId.value),
      "subject" -> s"urn:textus:bok:term:${_digest(term.termId.value)}",
      "predicate" -> _term_predicate,
      "objectValue" -> term.definition.value,
      "objectType" -> "literal",
      "metadata" -> Json.obj(
        "domain" -> Json.fromString("bok"),
        "recordKind" -> Json.fromString("term"),
        "termId" -> Json.fromString(term.termId.value)
      ).noSpaces,
      "evidenceIds" -> Vector(evidenceids(_evidence_key(term.evidence)))
    )

  private def _component_document(
    datasetid: String,
    component: ComponentReference,
    evidenceids: Map[String, String]
  ): Record =
    Record.dataAuto(
      "id" -> componentDocumentId(component),
      "sourceId" -> component.evidence.sourceId.value,
      "title" -> component.title.value,
      "uri" -> component.evidence.uri.value,
      "content" -> _component_content(component),
      "mediaType" -> "text/plain",
      "metadata" -> Json.obj(
        "domain" -> Json.fromString("bok"),
        "recordKind" -> Json.fromString("component-reference"),
        "datasetId" -> Json.fromString(datasetid),
        "kind" -> Json.fromString(component.kind.value),
        "name" -> Json.fromString(component.name.value),
        "version" -> component.version.map(x => Json.fromString(x.value)).getOrElse(Json.Null),
        "catalogId" -> component.catalogId.map(x => Json.fromString(x.value)).getOrElse(Json.Null),
        "organization" -> component.organization.map(x => Json.fromString(x.value)).getOrElse(Json.Null)
      ).noSpaces,
      "evidenceIds" -> Vector(evidenceids(_evidence_key(component.evidence)))
    )

  private def _component_assertion(
    component: ComponentReference,
    evidenceids: Map[String, String]
  ): Record =
    Record.dataAuto(
      "id" -> _stable_id("bok-component-assertion", component.kind.value, component.name.value),
      "subject" -> s"urn:textus:bok:component:${component.kind.value}:${_digest(component.name.value)}",
      "predicate" -> _component_predicate,
      "objectValue" -> component.kind.value,
      "objectType" -> "literal",
      "metadata" -> Json.obj(
        "domain" -> Json.fromString("bok"),
        "recordKind" -> Json.fromString("component-reference"),
        "name" -> Json.fromString(component.name.value)
      ).noSpaces,
      "evidenceIds" -> Vector(evidenceids(_evidence_key(component.evidence)))
    )

  private def _component_content(component: ComponentReference): String =
    Vector(
      Some(component.title.value),
      Some(s"${component.kind.value} component ${component.name.value}"),
      component.organization.map(x => s"organization ${x.value}"),
      component.version.map(x => s"version ${x.value}")
    ).flatten.mkString(". ")

  private def _evidence_record(evidence: BokEvidence, id: String): Record =
    Record.dataAuto(
      "id" -> id,
      "uri" -> evidence.uri.value,
      "sourceVersion" -> evidence.sourceVersion.map(_.value),
      "observedAt" -> evidence.observedAt.map(_.value),
      "freshness" -> evidence.freshness.map(_.value)
    )

  private def _evidence_key(evidence: BokEvidence): String =
    Vector(
      evidence.sourceId.value,
      evidence.uri.value,
      evidence.sourceVersion.map(_.value).getOrElse(""),
      evidence.observedAt.map(_.value).getOrElse(""),
      evidence.freshness.map(_.value).getOrElse("")
    ).mkString("\u0000")

  private def _stable_id(prefix: String, values: String*): String =
    s"$prefix:${_digest(values.mkString("\u0000"))}"

  private def _digest(value: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map(byte => f"${byte & 0xff}%02x")
      .mkString

  private def _publication(response: FederationDatasetResponse): Consequence[BokFederationPublication] =
    Consequence.success(BokFederationPublication(response.state.value, response.toRecord()))
}
