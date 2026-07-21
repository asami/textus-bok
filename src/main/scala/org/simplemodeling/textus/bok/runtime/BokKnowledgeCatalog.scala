package org.simplemodeling.textus.bok.runtime

import org.goldenport.Consequence
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.value.*

/*
 * BoK-owned matching state, replaced only after a complete SIE publication.
 *
 * @since   Jul. 21, 2026
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokKnowledgeCatalog {
  private final case class TermEntry(datasetId: String, sourceId: String, term: BokTerm)
  private final case class ComponentEntry(datasetId: String, sourceId: String, reference: ComponentReference)

  private var _datasets = Map.empty[String, NormalizedBokSource]

  def commit(
    publication: BokFederationPublication,
    normalized: NormalizedBokSource
  ): Boolean = synchronized {
    if (_normalize(publication.state) == "complete") {
      _datasets = _datasets.updated(normalized.source.datasetId.value, normalized)
      true
    } else {
      false
    }
  }

  def searchTerms(
    query: String,
    category: Option[String],
    limit: Int
  )(
    candidateScores: Set[BokCandidateKey] => Consequence[Map[BokCandidateKey, Double]]
  ): Consequence[SearchTermsResponse] = {
    val entries = _terms.filter { entry =>
      category.forall(x => entry.term.category.exists(y => _normalize(y.value) == _normalize(x)))
    }
    val exact = entries.filter(x => _term_identities(x.term).contains(_normalize(query)))
    if (_normalize(query).isEmpty || entries.isEmpty)
      Consequence.success(_term_response("no-match", query, Vector.empty, limit))
    else if (exact.nonEmpty)
      Consequence.success(_term_response(_term_status(exact.map(_.term)), query, exact.map(x => _term_match(x.term, "exact", 1.0, query)), limit))
    else
      candidateScores(entries.map(_term_candidate_key).toSet).map { scores =>
        val matches = entries.flatMap { entry =>
          scores.get(_term_candidate_key(entry))
            .filter(_ >= BokKnowledgeCatalog.MinimumCandidateScore)
            .map(_term_match(entry.term, "candidate", _, query))
        }.sortBy(x => (-x.score, x.term.title.value, x.term.termId.value))
        val status =
          if (matches.isEmpty) "no-match"
          else if (matches.size > 1 && _same_score(matches(0).score, matches(1).score)) "ambiguous"
          else "matched"
        _term_response(status, query, matches, limit)
      }
  }

  def explainTerm(query: String): ExplainTermResponse = {
    val exact = _terms.filter(x => _term_identities(x.term).contains(_normalize(query)))
    val status = if (_normalize(query).isEmpty || exact.isEmpty) "no-match" else _term_status(exact.map(_.term))
    val result = exact.sortBy(x => (x.term.title.value, x.term.termId.value)).headOption.map(x => _term_match(x.term, "exact", 1.0, query))
    ExplainTermResponse(
      BokQueryStatus(status),
      BokTermSearchText(query),
      result,
      _warnings(status)
    )
  }

  def searchComponentReferences(
    query: String,
    kind: Option[String],
    limit: Int
  )(
    candidateScores: Set[BokCandidateKey] => Consequence[Map[BokCandidateKey, Double]]
  ): Consequence[ComponentReferenceSearchResponse] = {
    val entries = _components.filter(x => kind.forall(y => _normalize(x.reference.kind.value) == _normalize(y)))
    val exact = entries.filter(x => _component_identities(x.reference).contains(_normalize(query)))
    if (_normalize(query).isEmpty || entries.isEmpty)
      Consequence.success(_component_response("no-match", query, Vector.empty, limit))
    else if (exact.nonEmpty)
      Consequence.success(_component_response("matched", query, exact.map(x => _component_match(x.reference, "exact", 1.0, query)), limit))
    else
      candidateScores(entries.map(_component_candidate_key).toSet).map { scores =>
        val matches = entries.flatMap { entry =>
          scores.get(_component_candidate_key(entry))
            .filter(_ >= BokKnowledgeCatalog.MinimumCandidateScore)
            .map(_component_match(entry.reference, "candidate", _, query))
        }.sortBy(x => (-x.score, x.reference.name.value, x.reference.kind.value))
        _component_response(if (matches.nonEmpty) "matched" else "no-match", query, matches, limit)
      }
  }

  def getComponentReference(
    name: String,
    version: Option[String],
    kind: Option[String]
  ): ComponentReferenceLookupResponse = {
    val matches = _components
      .filter(x => _normalize(x.reference.name.value) == _normalize(name))
      .filter(x => version.forall(y => x.reference.version.exists(z => _normalize(z.value) == _normalize(y))))
      .filter(x => kind.forall(y => _normalize(x.reference.kind.value) == _normalize(y)))
      .sortBy(x => (x.reference.kind.value, x.reference.name.value, x.reference.version.map(_.value).getOrElse("")))
    val status = if (matches.isEmpty) "no-match" else if (matches.size == 1) "matched" else "ambiguous"
    ComponentReferenceLookupResponse(BokQueryStatus(status), matches.headOption.map(_.reference), _warnings(status))
  }

  private def _terms: Vector[TermEntry] = synchronized {
    _datasets.toVector.sortBy(_._1).flatMap { case (datasetid, source) =>
      source.terms.map(TermEntry(datasetid, source.source.sourceId.value, _))
    }
  }

  private def _components: Vector[ComponentEntry] = synchronized {
    _datasets.toVector.sortBy(_._1).flatMap { case (datasetid, source) =>
      source.components.map(ComponentEntry(datasetid, source.source.sourceId.value, _))
    }
  }

  private def _term_identities(term: BokTerm): Set[String] =
    Set(_normalize(term.termId.value), _normalize(term.title.value)).filter(_.nonEmpty)

  private def _component_identities(component: ComponentReference): Set[String] =
    Set(_normalize(component.name.value), _normalize(component.title.value)).filter(_.nonEmpty)

  private def _term_candidate_key(entry: TermEntry): BokCandidateKey =
    BokCandidateKey(entry.datasetId, entry.sourceId, BokFederationPublisher.termDocumentId(entry.term))

  private def _component_candidate_key(entry: ComponentEntry): BokCandidateKey =
    BokCandidateKey(
      entry.datasetId,
      entry.sourceId,
      BokFederationPublisher.componentDocumentId(entry.reference)
    )

  private def _term_status(terms: Vector[BokTerm]): String =
    if (terms.forall(!_sufficient_evidence(_)))
      "insufficient-evidence"
    else if (terms.size > 1) {
      val definitions = terms.map(x => _normalize(x.definition.value)).filter(_.nonEmpty).distinct
      if (definitions.size > 1) "conflict" else "ambiguous"
    } else
      "matched"

  private def _sufficient_evidence(term: BokTerm): Boolean =
    term.definition.value.trim.nonEmpty && term.evidence.uri.value.trim.nonEmpty

  private def _term_match(term: BokTerm, kind: String, score: Double, query: String): BokTermMatch =
    BokTermMatch(
      term,
      BokMatchKind(kind),
      score,
      BokMatchRationale(
        if (kind == "exact") s"Exact BoK term identity or title match for '$query'."
        else s"Provider-backed semantic candidate for '$query'; select it explicitly before grounding."
      )
    )

  private def _component_match(
    component: ComponentReference,
    kind: String,
    score: Double,
    query: String
  ): ComponentReferenceMatch =
    ComponentReferenceMatch(
      component,
      BokMatchKind(kind),
      score,
      BokMatchRationale(
        if (kind == "exact") s"Exact component identity or title match for '$query'."
        else s"Provider-backed component candidate for '$query'."
      )
    )

  private def _term_response(
    status: String,
    query: String,
    matches: Vector[BokTermMatch],
    limit: Int
  ): SearchTermsResponse =
    SearchTermsResponse(
      BokQueryStatus(status),
      BokTermSearchText(query),
      matches.take(_limit(limit)),
      _warnings(status)
    )

  private def _component_response(
    status: String,
    query: String,
    matches: Vector[ComponentReferenceMatch],
    limit: Int
  ): ComponentReferenceSearchResponse =
    ComponentReferenceSearchResponse(
      BokQueryStatus(status),
      ComponentSearchText(query),
      matches.take(_limit(limit)),
      _warnings(status)
    )

  private def _warnings(status: String): Vector[BokWarning] = status match {
    case "ambiguous" => Vector(BokWarning("Multiple BoK records satisfy the request; select an explicit identity."))
    case "conflict" => Vector(BokWarning("BoK contains conflicting curated definitions for the requested identity."))
    case "insufficient-evidence" => Vector(BokWarning("The matched BoK term has no usable definition or attributable evidence."))
    case _ => Vector.empty
  }

  private def _limit(value: Int): Int = value.max(1).min(100)

  private def _same_score(lhs: Double, rhs: Double): Boolean =
    math.abs(lhs - rhs) < 0.000001

  private def _normalize(value: String): String =
    value.trim.toLowerCase.replaceAll("[^\\p{L}\\p{N}]+", " ").trim
}

object BokKnowledgeCatalog {
  val MinimumCandidateScore = 0.2
}
