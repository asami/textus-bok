package org.simplemodeling.textus.bok.runtime

import org.goldenport.Consequence
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.value.*

/*
 * BoK-owned matching state, replaced only after a complete SIE publication.
 *
 * @since   Jul. 21, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final class BokKnowledgeCatalog {
  private val _semantic_manifest_kinds = Set("component-manifest", "component-knowledge-consumer-contract", "semantic-index")

  private final case class TermEntry(datasetid: String, sourceid: String, term: BokTerm)
  private final case class ComponentEntry(datasetid: String, sourceid: String, reference: ComponentReference)
  private final case class MapSource(normalized: NormalizedBokSource)
  private final case class MapNode(source: MapSource, node: BokKnowledgeNode)
  private final case class MapRelationship(source: MapSource, relationship: BokKnowledgeRelationship)
  private final case class MapLimit(value: Int, warning: Option[BokWarning])

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

  def selectedTopology(datasetId: String): Option[BokKnowledgeTopology] = synchronized {
    _datasets.get(datasetId).map(_.topology)
  }

  def getKnowledgeMap(
    resolved: ResolvedBokProfile,
    category: Option[String],
    termType: Option[String],
    focus: Option[String],
    nodeLimit: Option[Int],
    relationshipLimit: Option[Int]
  ): GetKnowledgeMapResponse = synchronized {
    val nodebound = _map_limit(nodeLimit, BokKnowledgeCatalog.DEFAULT_KNOWLEDGE_MAP_NODE_LIMIT, "node")
    val relationshipbound = _map_limit(relationshipLimit, BokKnowledgeCatalog.DEFAULT_KNOWLEDGE_MAP_RELATIONSHIP_LIMIT, "relationship")
    val sources = _map_sources(resolved)
    val nodes = sources.flatMap(_map_nodes)
    val relationships = sources.flatMap(_map_relationships)
    val filtered = nodes.filter(_matches_map_filters(_, category, termType))
    val seeds = focus match {
      case Some(value) => filtered.filter(_matches_map_focus(_, value))
      case None => filtered
    }
    val scoped = category.nonEmpty || termType.nonEmpty || focus.nonEmpty
    val seedkeys = seeds.map(_map_node_key).toSet
    val projectedrelationships =
      if (scoped) relationships.filter { relationship =>
        seedkeys.contains(_map_subject_key(relationship)) || seedkeys.contains(_map_object_key(relationship))
      }
      else relationships
    val candidatekeys =
      if (scoped) seedkeys ++ projectedrelationships.flatMap(x => Vector(_map_subject_key(x), _map_object_key(x)))
      else nodes.map(_map_node_key).toSet
    val candidatenodes = nodes.filter(x => candidatekeys.contains(_map_node_key(x))).sortBy(_map_node_key)
    val orderednodes =
      if (scoped)
        candidatenodes.filter(x => seedkeys.contains(_map_node_key(x))) ++
          candidatenodes.filterNot(x => seedkeys.contains(_map_node_key(x)))
      else
        candidatenodes
    val resultnodes = orderednodes.take(nodebound.value)
    val resultkeys = resultnodes.map(_map_node_key).toSet
    val endpointrelationships = projectedrelationships
      .filter(x => resultkeys.contains(_map_subject_key(x)) && resultkeys.contains(_map_object_key(x)))
      .sortBy(_map_relationship_key)
    val resultrelationships = endpointrelationships.take(relationshipbound.value)
    val truncated = sources.exists(_.normalized.topology.truncated) ||
      candidatenodes.size > resultnodes.size ||
      projectedrelationships.size > endpointrelationships.size ||
      endpointrelationships.size > resultrelationships.size
    val warnings = (
      sources.flatMap(_.normalized.warnings) ++
        nodebound.warning.toVector ++
        relationshipbound.warning.toVector ++
        Option.when(truncated)(BokWarning("Knowledge Map result is truncated"))
    ).distinct
    val status = if (resultnodes.nonEmpty) "matched" else "no-match"
    GetKnowledgeMapResponse(
      BokQueryStatus(status),
      _selection(resolved),
      sources.map(_map_selected_source),
      resultnodes.map(_map_node),
      resultrelationships.map(_map_relationship),
      nodebound.value,
      relationshipbound.value,
      truncated,
      warnings
    )
  }

  def searchTerms(
    resolved: ResolvedBokProfile,
    query: String,
    category: Option[String],
    limit: Int
  )(
    candidateScores: Set[BokCandidateKey] => Consequence[Map[BokCandidateKey, Double]]
  ): Consequence[SearchTermsResponse] = {
    val entries = _terms(resolved).filter { entry =>
      category.forall(x => entry.term.category.exists(y => _normalize(y.value) == _normalize(x)))
    }
    val exact = entries.filter(x => _term_identities(x.term).contains(_normalize(query)))
    if (_normalize(query).isEmpty || entries.isEmpty)
      Consequence.success(_term_response(resolved, "no-match", query, Vector.empty, limit))
    else if (exact.nonEmpty)
      Consequence.success(_term_response(resolved, _term_status(exact.map(_.term)), query, exact.map(x => _term_match(x.term, "exact", 1.0, query)), limit))
    else
      candidateScores(entries.map(_term_candidate_key).toSet).map { scores =>
        val matches = entries.flatMap { entry =>
          scores.get(_term_candidate_key(entry))
            .filter(_ >= BokKnowledgeCatalog.MINIMUM_CANDIDATE_SCORE)
            .map(_term_match(entry.term, "candidate", _, query))
        }.sortBy(x => (-x.score, x.term.title.value, x.term.termId.value))
        val status =
          if (matches.isEmpty) "no-match"
          else if (matches.size > 1 && _same_score(matches(0).score, matches(1).score)) "ambiguous"
          else "matched"
        _term_response(resolved, status, query, matches, limit)
      }
  }

  def explainTerm(resolved: ResolvedBokProfile, query: String): ExplainTermResponse = {
    val exact = _terms(resolved).filter(x => _term_identities(x.term).contains(_normalize(query)))
    val status = if (_normalize(query).isEmpty || exact.isEmpty) "no-match" else _term_status(exact.map(_.term))
    val result = exact.sortBy(x => (x.term.title.value, x.term.termId.value)).headOption.map(x => _term_match(x.term, "exact", 1.0, query))
    ExplainTermResponse(
      BokQueryStatus(status),
      _selection(resolved),
      BokTermSearchText(query),
      result,
      _warnings(status)
    )
  }

  def searchComponentReferences(
    resolved: ResolvedBokProfile,
    query: String,
    kind: Option[String],
    limit: Int
  )(
    candidateScores: Set[BokCandidateKey] => Consequence[Map[BokCandidateKey, Double]]
  ): Consequence[ComponentReferenceSearchResponse] = {
    val entries = _components(resolved).filter(x => kind.forall(y => _normalize(x.reference.kind.value) == _normalize(y)))
    val exact = entries.filter(x => _component_identities(x.reference).contains(_normalize(query)))
    if (_normalize(query).isEmpty || entries.isEmpty)
      Consequence.success(_component_response(resolved, "no-match", query, Vector.empty, limit))
    else if (exact.nonEmpty)
      Consequence.success(_component_response(
        resolved,
        "matched",
        query,
        exact.sortBy(x => BokComponentReferenceIdentity.orderKey(x.reference))
          .map(x => _component_match(x.reference, "exact", 1.0, query)),
        limit
      ))
    else
      candidateScores(entries.map(_component_candidate_key).toSet).map { scores =>
        val matches = entries.flatMap { entry =>
          scores.get(_component_candidate_key(entry))
            .filter(_ >= BokKnowledgeCatalog.MINIMUM_CANDIDATE_SCORE)
            .map(_component_match(entry.reference, "candidate", _, query))
        }.sortBy(x => (-x.score, BokComponentReferenceIdentity.orderKey(x.reference)))
        _component_response(resolved, if (matches.nonEmpty) "matched" else "no-match", query, matches, limit)
      }
  }

  def getComponentReference(
    resolved: ResolvedBokProfile,
    name: String,
    version: Option[String],
    kind: Option[String],
    organization: Option[String]
  ): ComponentReferenceLookupResponse = {
    val matches = _components(resolved)
      .filter(x => _normalize(x.reference.name.value) == _normalize(name))
      .filter(x => version.forall(y => x.reference.version.exists(z => _normalize(z.value) == _normalize(y))))
      .filter(x => kind.forall(y => _normalize(x.reference.kind.value) == _normalize(y)))
      .filter(x => organization.forall(y => x.reference.organization.exists(z => _normalize(z.value) == _normalize(y))))
      .sortBy(x => BokComponentReferenceIdentity.orderKey(x.reference))
    val status = if (matches.isEmpty) "no-match" else if (matches.size == 1) "matched" else "ambiguous"
    val reference = matches match {
      case Vector(single) => Some(single.reference)
      case _ => None
    }
    ComponentReferenceLookupResponse(BokQueryStatus(status), _selection(resolved), reference, _warnings(status))
  }

  def searchSemanticKnowledge(
    access: BokSemanticAccess,
    query: String,
    limit: Int
  )(candidateScores: Set[BokCandidateKey] => Consequence[Map[BokCandidateKey, Double]]): Consequence[SemanticKnowledgeSearchResponse] = {
    val bound = _semantic_limit(limit)
    val records = _semantic_records(access).filter(_semantic_matches(_, query))
    if (records.nonEmpty) Consequence.success(_semantic_search_response(access.selection, query, records, bound))
    else candidateScores(Set.empty).map(_ => _semantic_search_response(access.selection, query, Vector.empty, bound))
  }

  def discoverSemanticKnowledge(access: BokSemanticAccess, limit: Int): SemanticKnowledgeDiscoveryResponse =
    discoverSemanticKnowledge(access, "", limit)

  def discoverSemanticKnowledge(access: BokSemanticAccess, query: String, limit: Int): SemanticKnowledgeDiscoveryResponse = {
    val bound = _semantic_limit(limit)
    val records = _semantic_records(access).filter(x => query.trim.isEmpty || _semantic_matches(x, query))
    val results = records
    SemanticKnowledgeDiscoveryResponse(
      BokQueryStatus(if (results.nonEmpty) "matched" else "no-match"),
      _selection(access.selection),
      query,
      results.take(bound).map(_semantic_record),
      bound,
      results.size > bound,
      Vector.empty
    )
  }

  def getSemanticManifest(access: BokSemanticAccess, identity: String): SemanticManifestResponse =
    _semantic_lookup(access, _all_semantic_records(access.selection).filter(record =>
      record.identity == identity && _semantic_manifest_kinds.contains(record.kind)
    )) { record =>
      SemanticManifestResponse(BokQueryStatus("matched"), _selection(access.selection), Some(_semantic_record(record)), Some(record.summary), Some(record.authority), record.componentReference, Vector.empty)
    } { status => SemanticManifestResponse(BokQueryStatus(status), _selection(access.selection), None, None, None, None, _warnings(status)) }

  def getSemanticResource(access: BokSemanticAccess, identity: String): SemanticResourceResponse =
    _semantic_lookup(access, _all_semantic_records(access.selection).filter(record =>
      record.identity == identity && !_semantic_manifest_kinds.contains(record.kind) && record.kind != "smartdox-section"
    )) { record =>
      SemanticResourceResponse(BokQueryStatus("matched"), _selection(access.selection), Some(_semantic_record(record)), Some(record.summary), Some(record.authority), record.componentReference, Vector.empty)
    } { status => SemanticResourceResponse(BokQueryStatus(status), _selection(access.selection), None, None, None, None, _warnings(status)) }

  def getSemanticSection(access: BokSemanticAccess, documentId: String, sectionId: String): SemanticSectionResponse = {
    _semantic_lookup(access, _all_semantic_records(access.selection).filter(record =>
      record.kind == "smartdox-section" && record.documentId == documentId && record.sectionId.contains(sectionId)
    )) { record =>
      SemanticSectionResponse(BokQueryStatus("matched"), _selection(access.selection), Some(_semantic_record(record)), Some(record.summary), Some(record.authority), record.componentReference, Vector.empty)
    } { status => SemanticSectionResponse(BokQueryStatus(status), _selection(access.selection), None, None, None, None, _warnings(status)) }
  }

  private def _semantic_lookup[A](
    access: BokSemanticAccess,
    candidates: Vector[BokSemanticRecord]
  )(matched: BokSemanticRecord => A)(missing: String => A): A =
    candidates match {
      case Vector() => missing("no-match")
      case _ if candidates.size > 1 => missing("ambiguous")
      case Vector(record) if record.stale => missing("stale")
      case Vector(record) if !access.permits(record) => missing("forbidden")
      case Vector(record) => matched(record)
    }

  private def _semantic_records(access: BokSemanticAccess): Vector[BokSemanticRecord] =
    _all_semantic_records(access.selection).filter(x => !x.stale && access.permits(x))

  private def _all_semantic_records(resolved: ResolvedBokProfile): Vector[BokSemanticRecord] = synchronized {
    _selected_sources(resolved).flatMap(_.semanticRecords)
  }

  private def _semantic_matches(record: BokSemanticRecord, query: String): Boolean = {
    val needle = _normalize(query)
    val searchable = Vector(
      record.identity,
      record.kind,
      record.title,
      record.summary,
      record.documentId,
      record.sectionId.getOrElse(""),
      record.canonicalUrl
    ).mkString(" ")
    needle.nonEmpty && _normalize(searchable).contains(needle)
  }

  private def _semantic_limit(value: Int): Int = value.max(1).min(100)

  private def _semantic_search_response(resolved: ResolvedBokProfile, query: String, records: Vector[BokSemanticRecord], bound: Int): SemanticKnowledgeSearchResponse = {
    val results = records.sortBy(x => (x.kind, x.identity))
    SemanticKnowledgeSearchResponse(BokQueryStatus(if (results.nonEmpty) "matched" else "no-match"), _selection(resolved), query, results.take(bound).map(_semantic_record), bound, results.size > bound, None, None, None, Vector.empty)
  }

  private def _semantic_record(record: BokSemanticRecord): SemanticKnowledgeRecord =
    SemanticKnowledgeRecord(record.kind, record.identity, record.title, record.summary, record.documentId, record.sectionId, record.canonicalUrl, record.indexedAt, record.visibility, record.authority, record.sourceId, record.datasetId, record.generation, record.digest, record.stale, record.componentReference, record.evidence)

  private def _terms(resolved: ResolvedBokProfile): Vector[TermEntry] = synchronized {
    _selected_sources(resolved).flatMap { source =>
      source.terms.map(TermEntry(
        source.source.datasetId.value,
        source.source.sourceId.value,
        _
      ))
    }
  }

  private def _components(resolved: ResolvedBokProfile): Vector[ComponentEntry] = synchronized {
    _selected_sources(resolved).flatMap { source =>
      source.components.map(ComponentEntry(
        source.source.datasetId.value,
        source.source.sourceId.value,
        _
      ))
    }
  }

  private def _selected_sources(resolved: ResolvedBokProfile): Vector[NormalizedBokSource] =
    _datasets.values.toVector
      .filter(_matches_resolved(_, resolved))
      .sortBy(x => (x.source.datasetId.value, x.source.sourceId.value))

  private def _matches_resolved(source: NormalizedBokSource, resolved: ResolvedBokProfile): Boolean =
    source.source.datasetId == resolved.datasetId &&
      source.source.sourceId == resolved.sourceId &&
      source.source.generation == resolved.generation

  private def _map_sources(resolved: ResolvedBokProfile): Vector[MapSource] =
    _selected_sources(resolved)
      .map(MapSource.apply)

  private def _map_nodes(source: MapSource): Vector[MapNode] =
    source.normalized.topology.nodes.map(MapNode(source, _))

  private def _map_relationships(source: MapSource): Vector[MapRelationship] =
    source.normalized.topology.relationships.map(MapRelationship(source, _))

  private def _matches_map_filters(
    node: MapNode,
    category: Option[String],
    termtype: Option[String]
  ): Boolean =
    category.forall(x => node.node.category.exists(y => _normalize(y) == _normalize(x))) &&
      termtype.forall(x => _map_term_types(node).contains(_normalize(x)))

  private def _matches_map_focus(node: MapNode, focus: String): Boolean =
    _map_focus_values(node).contains(_normalize(focus))

  private def _map_term_types(node: MapNode): Set[String] =
    if (_normalize(node.node.kind) != "term") Set.empty
    else _map_terms(node).map(x => _normalize(x.termType.value)).filter(_.nonEmpty).toSet

  private def _map_terms(node: MapNode): Vector[BokTerm] = {
    val references = (node.node.terms :+ node.node.id).map(_normalize).toSet
    node.source.normalized.terms.filter(x => references.contains(_normalize(x.termId.value)))
  }

  private def _map_focus_values(node: MapNode): Set[String] =
    (node.node.id +: node.node.label +: node.node.terms).map(_normalize).filter(_.nonEmpty).toSet

  private def _map_node_key(node: MapNode): (String, String) =
    (node.source.normalized.source.datasetId.value, node.node.id)

  private def _map_subject_key(relationship: MapRelationship): (String, String) =
    (relationship.source.normalized.source.datasetId.value, relationship.relationship.subjectId)

  private def _map_object_key(relationship: MapRelationship): (String, String) =
    (relationship.source.normalized.source.datasetId.value, relationship.relationship.objectId)

  private def _map_relationship_key(relationship: MapRelationship): (String, String, String, String) =
    (
      relationship.source.normalized.source.datasetId.value,
      relationship.relationship.subjectId,
      relationship.relationship.predicate,
      relationship.relationship.objectId
    )

  private def _map_limit(requested: Option[Int], default: Int, label: String): MapLimit = {
    val effective = requested.map(_.max(1).min(default)).getOrElse(default)
    val warning = requested.filter(_ != effective).map { value =>
      BokWarning(s"Knowledge Map $label limit $value is clamped to $effective")
    }
    MapLimit(effective, warning)
  }

  private def _map_selected_source(source: MapSource): BokKnowledgeMapSelectedSource =
    BokKnowledgeMapSelectedSource(
      source.normalized.source.datasetId,
      source.normalized.source.sourceId,
      source.normalized.source.generation,
      source.normalized.topology.sourceRef.map { x =>
        BokKnowledgeMapSourceReference(
          BokKnowledgeMapSourceReferenceKind(x.kind),
          BokKnowledgeMapSourceReferenceValue(x.value),
          x.uri.map(BokKnowledgeMapSourceReferenceUri.apply)
        )
      },
      source.normalized.topology.truncated,
      source.normalized.warnings
    )

  private def _map_node(node: MapNode): BokKnowledgeMapNode =
    BokKnowledgeMapNode(
      node.source.normalized.source.datasetId,
      node.source.normalized.source.sourceId,
      BokKnowledgeMapNodeId(node.node.id),
      BokKnowledgeMapNodeLabel(node.node.label),
      BokKnowledgeMapNodeKind(node.node.kind),
      node.node.category.map(BokTermCategory.apply),
      node.node.terms.map(BokTermId.apply),
      _map_terms(node),
      _map_component_references(node),
      node.node.tags.map(BokKnowledgeMapTag.apply),
      node.node.evidence
    )

  private def _map_component_references(node: MapNode): Vector[ComponentReference] =
    node.node.componentReference.toVector.flatMap { reference =>
      node.source.normalized.components.filter(component =>
        BokComponentReferenceIdentity.matches(
          component,
          reference.kind,
          reference.organization,
          reference.name
        ) &&
          reference.version.forall(value => component.version.exists(_.value == value))
      )
    }.sortBy(BokComponentReferenceIdentity.orderKey)

  private def _map_relationship(relationship: MapRelationship): BokKnowledgeMapRelationship =
    BokKnowledgeMapRelationship(
      relationship.source.normalized.source.datasetId,
      relationship.source.normalized.source.sourceId,
      BokKnowledgeMapNodeId(relationship.relationship.subjectId),
      BokKnowledgeMapPredicate(relationship.relationship.predicate),
      BokKnowledgeMapNodeId(relationship.relationship.objectId),
      relationship.relationship.label.map(BokKnowledgeMapNodeLabel.apply),
      relationship.relationship.category.map(BokTermCategory.apply),
      relationship.relationship.terms.map(BokTermId.apply),
      relationship.relationship.tags.map(BokKnowledgeMapTag.apply),
      relationship.relationship.evidence
    )

  private def _term_identities(term: BokTerm): Set[String] =
    Set(_normalize(term.termId.value), _normalize(term.title.value)).filter(_.nonEmpty)

  private def _component_identities(component: ComponentReference): Set[String] =
    Set(_normalize(component.name.value), _normalize(component.title.value)).filter(_.nonEmpty)

  private def _term_candidate_key(entry: TermEntry): BokCandidateKey =
    BokCandidateKey(entry.datasetid, entry.sourceid, BokFederationPublisher.termDocumentId(entry.term))

  private def _component_candidate_key(entry: ComponentEntry): BokCandidateKey =
    BokCandidateKey(
      entry.datasetid,
      entry.sourceid,
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
    resolved: ResolvedBokProfile,
    status: String,
    query: String,
    matches: Vector[BokTermMatch],
    limit: Int
  ): SearchTermsResponse =
    SearchTermsResponse(
      BokQueryStatus(status),
      _selection(resolved),
      BokTermSearchText(query),
      matches.take(_limit(limit)),
      _warnings(status)
    )

  private def _component_response(
    resolved: ResolvedBokProfile,
    status: String,
    query: String,
    matches: Vector[ComponentReferenceMatch],
    limit: Int
  ): ComponentReferenceSearchResponse =
    ComponentReferenceSearchResponse(
      BokQueryStatus(status),
      _selection(resolved),
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

  private def _selection(resolved: ResolvedBokProfile): BokResolvedSelection =
    BokResolvedSelection(
      BokProfile(resolved.resolvedProfile),
      resolved.projectId.map(BokProjectId.apply),
      resolved.datasetId,
      resolved.sourceId,
      resolved.generation,
      resolved.evidence
    )

  private def _same_score(lhs: Double, rhs: Double): Boolean =
    math.abs(lhs - rhs) < 0.000001

  private def _normalize(value: String): String =
    value.trim.toLowerCase.replaceAll("[^\\p{L}\\p{N}]+", " ").trim
}

object BokKnowledgeCatalog {
  val MINIMUM_CANDIDATE_SCORE = 0.2
  val DEFAULT_KNOWLEDGE_MAP_NODE_LIMIT = 128
  val DEFAULT_KNOWLEDGE_MAP_RELATIONSHIP_LIMIT = 256
}
