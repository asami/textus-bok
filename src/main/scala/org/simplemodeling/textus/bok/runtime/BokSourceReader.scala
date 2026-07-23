package org.simplemodeling.textus.bok.runtime

import io.circe.{Decoder, HCursor, Json, JsonObject}
import io.circe.parser
import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.repository.ComponentRepositoryIndex
import org.goldenport.cncf.resource.ResourceReference
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.value.*

/*
 * Metadata-only BoK source reader over the CNCF ResourceAccess DSL.
 *
 * @since   Jul. 21, 2026
 * @version Jul. 23, 2026
 * @author  ASAMI, Tomoharu
 */
final case class NormalizedBokSource(
  source: BokKnowledgeSource,
  terms: Vector[BokTerm],
  components: Vector[ComponentReference],
  warnings: Vector[BokWarning],
  topology: BokKnowledgeTopology = BokKnowledgeTopology.empty
)

final case class BokKnowledgeSourceReference(kind: String, value: String, uri: Option[String])
final case class BokKnowledgeComponentReference(kind: String, name: String)
final case class BokKnowledgeNode(
  id: String,
  label: String,
  kind: String,
  evidence: BokEvidence,
  category: Option[String] = None,
  terms: Vector[String] = Vector.empty,
  tags: Vector[String] = Vector.empty,
  componentReference: Option[BokKnowledgeComponentReference] = None
)
final case class BokKnowledgeRelationship(
  subjectId: String,
  predicate: String,
  objectId: String,
  label: Option[String],
  evidence: BokEvidence,
  category: Option[String] = None,
  terms: Vector[String] = Vector.empty,
  tags: Vector[String] = Vector.empty
)
final case class BokKnowledgeTopology(
  nodes: Vector[BokKnowledgeNode],
  relationships: Vector[BokKnowledgeRelationship],
  truncated: Boolean,
  sourceRef: Option[BokKnowledgeSourceReference] = None
)
object BokKnowledgeTopology {
  val empty = BokKnowledgeTopology(Vector.empty, Vector.empty, false)
}

object BokSourceReader {
  private val _manifest_path = "metadata/cncf/knowledge-source.json"
  private val _manifest_schema = "cncf.knowledge-source.v1"
  private val _glossary_kind = "glossary-terms"
  private val _repository_kind = "component-repository-index"
  private val _reference_kind = "component-reference-index"
  private val _reference_schema = "cncf.component-reference-index.v1"
  private val _graph_kind = "rdf-graph-summary"
  private val _graph_schema = "cozy.rdf-graph-summary.v1"
  private val _maximum_graph_nodes = 512
  private val _maximum_graph_relationships = 2048
  private val _maximum_identifier_length = 1024
  private val _maximum_label_length = 512
  private val _maximum_term_references = 32
  private val _maximum_tags = 32

  def read(
    context: ExecutionContext,
    source: BokKnowledgeSource
  ): Consequence[NormalizedBokSource] =
    for {
      base <- ResourceReference.parseC(source.resource.value)
      manifestreference <- ResourceReference.resolveC(base, _manifest_path)
      manifesttext <- context.resources.readText(manifestreference)
      manifest <- _parse_manifest(manifesttext)
      references <- _resolve_resources(base, manifest.resources)
      recognized = references.filter(x =>
        x._1 == _glossary_kind || x._1 == _repository_kind || x._1 == _reference_kind
      )
      _ <- _require(
        recognized.nonEmpty,
        "BoK KnowledgeSource contains no recognized glossary or component repository metadata"
      )
      terms <- _read_terms(context, source, references.filter(_._1 == _glossary_kind).map(_._2))
      repositorycomponents <- _read_components(
        context,
        source,
        base,
        references.filter(_._1 == _repository_kind).map(_._2)
      )
      referencecomponents <- _read_component_references(
        context,
        source,
        base,
        references.filter(_._1 == _reference_kind).map(_._2)
      )
      components = repositorycomponents ++ referencecomponents
      topology <- _read_topology(context, source, references.filter(_._1 == _graph_kind).map(_._2))
      _ <- _validate_topology_component_references(topology, components)
      warnings = _warnings(manifest, terms, components) ++ Option.when(topology.truncated)(BokWarning("BoK graph summary is truncated"))
    } yield NormalizedBokSource(
      source,
      terms.sortBy(_.termId.value),
      components.sortBy(x => (x.kind.value, x.name.value, x.version.map(_.value).getOrElse(""))),
      warnings,
      topology
    )

  private def _read_topology(context: ExecutionContext, source: BokKnowledgeSource, references: Vector[ResourceReference]): Consequence[BokKnowledgeTopology] =
    if (references.isEmpty) Consequence.success(BokKnowledgeTopology.empty)
    else if (references.size != 1) Consequence.resourceInvalid("BoK KnowledgeSource has multiple rdf-graph-summary resources")
    else context.resources.readText(references.head).flatMap(_parse_topology(source, references.head, _))

  private def _parse_topology(source: BokKnowledgeSource, reference: ResourceReference, value: String): Consequence[BokKnowledgeTopology] =
    parser.parse(value).left.map(_.message).flatMap(json => json.asObject.toRight("graph summary must be a JSON object")) match {
      case Right(graph) if graph("schemaVersion").flatMap(_.asString).contains(_graph_schema) && graph("kind").flatMap(_.asString).contains(_graph_kind) =>
        _decode_topology(source, reference, graph) match {
          case Right(topology) => Consequence.success(topology)
          case Left(message) => Consequence.resourceInvalid(s"Invalid BoK graph summary: $message")
        }
      case Right(_) => Consequence.resourceInvalid("Unsupported BoK graph summary schemaVersion or kind")
      case Left(message) => Consequence.resourceInvalid(s"Invalid BoK graph summary: $message")
    }

  private def _decode_topology(
    source: BokKnowledgeSource,
    reference: ResourceReference,
    graph: JsonObject
  ): Either[String, BokKnowledgeTopology] =
    for {
      sourceref <- _decode_source_reference(graph)
      nodesjson <- _required_array(graph, "nodes")
      edgesjson <- _required_array(graph, "edges")
      _ <- _require_either(nodesjson.size <= _maximum_graph_nodes, s"nodes exceeds maximum $_maximum_graph_nodes")
      _ <- _require_either(edgesjson.size <= _maximum_graph_relationships, s"edges exceeds maximum $_maximum_graph_relationships")
      decodednodes <- _decode_nodes(nodesjson, _evidence(source, reference))
      decodedrelationships <- _decode_relationships(edgesjson, _evidence(source, reference))
      truncated <- _required_boolean(graph, "truncated")
      _ <- _validate_topology(decodednodes, decodedrelationships)
    } yield BokKnowledgeTopology(
      decodednodes.distinct.sortBy(_.id),
      decodedrelationships.distinct.sortBy(x => (x.subjectId, x.predicate, x.objectId)),
      truncated,
      Some(sourceref)
    )

  private def _decode_source_reference(graph: JsonObject): Either[String, BokKnowledgeSourceReference] =
    for {
      objectvalue <- graph("sourceRef").flatMap(_.asObject).toRight("sourceRef must be an object")
      kind <- _required_identifier(objectvalue, "kind", "sourceRef.kind")
      _ <- _require_either(kind == "bok-site", "sourceRef.kind must be bok-site")
      value <- _required_identifier(objectvalue, "value", "sourceRef.value")
      uri <- _optional_label(objectvalue, "uri", "sourceRef.uri")
    } yield BokKnowledgeSourceReference(kind, value, uri)

  private def _decode_nodes(values: Vector[Json], evidence: BokEvidence): Either[String, Vector[BokKnowledgeNode]] =
    _sequence_either(values.zipWithIndex.map { case (value, index) =>
      for {
        objectvalue <- value.asObject.toRight(s"nodes[$index] must be an object")
        id <- _required_identifier(objectvalue, "id", s"nodes[$index].id")
        label <- _required_label(objectvalue, "label", s"nodes[$index].label")
        kind <- _required_identifier(objectvalue, "node_type", s"nodes[$index].node_type")
        category <- _optional_label(objectvalue, "category", s"nodes[$index].category")
        terms <- _optional_identifiers(objectvalue, "terms", s"nodes[$index].terms", _maximum_term_references)
        tags <- _optional_labels(objectvalue, "tags", s"nodes[$index].tags", _maximum_tags)
        componentreference <- _decode_component_reference(objectvalue, kind, s"nodes[$index].componentRef")
      } yield BokKnowledgeNode(id, label, kind, evidence, category, terms, tags, componentreference)
    })

  private def _decode_component_reference(
    objectvalue: JsonObject,
    nodekind: String,
    label: String
  ): Either[String, Option[BokKnowledgeComponentReference]] =
    objectvalue("componentRef") match {
      case None | Some(Json.Null) => Right(None)
      case Some(value) => for {
        reference <- value.asObject.toRight(s"$label must be an object")
        kind <- _required_identifier(reference, "kind", s"$label.kind")
        _ <- _require_either(Set("car", "sar").contains(kind), s"$label.kind must be car or sar")
        _ <- _require_either(nodekind == "component-reference", s"$label requires node_type component-reference")
        name <- _required_identifier(reference, "name", s"$label.name")
      } yield Some(BokKnowledgeComponentReference(kind, name))
    }

  private def _decode_relationships(values: Vector[Json], evidence: BokEvidence): Either[String, Vector[BokKnowledgeRelationship]] =
    _sequence_either(values.zipWithIndex.map { case (value, index) =>
      for {
        objectvalue <- value.asObject.toRight(s"edges[$index] must be an object")
        subject <- _required_identifier(objectvalue, "source", s"edges[$index].source")
        predicate <- _required_identifier(objectvalue, "predicate", s"edges[$index].predicate")
        obj <- _required_identifier(objectvalue, "target", s"edges[$index].target")
        label <- _optional_label(objectvalue, "label", s"edges[$index].label")
        category <- _optional_label(objectvalue, "category", s"edges[$index].category")
        terms <- _optional_identifiers(objectvalue, "terms", s"edges[$index].terms", _maximum_term_references)
        tags <- _optional_labels(objectvalue, "tags", s"edges[$index].tags", _maximum_tags)
      } yield BokKnowledgeRelationship(subject, predicate, obj, label, evidence, category, terms, tags)
    })

  private def _validate_topology(
    nodes: Vector[BokKnowledgeNode],
    relationships: Vector[BokKnowledgeRelationship]
  ): Either[String, Unit] = {
    val nodeconflicts = nodes.groupBy(_.id).collect { case (id, values) if values.distinct.size > 1 => id }.toVector.sorted
    val edgeconflicts = relationships.groupBy(x => (x.subjectId, x.predicate, x.objectId)).collect {
      case (identity, values) if values.distinct.size > 1 => identity
    }.toVector.sortBy(identity => identity.toString)
    val nodeids = nodes.map(_.id).toSet
    val dangling = relationships.filter(x => !nodeids.contains(x.subjectId) || !nodeids.contains(x.objectId))
    for {
      _ <- _require_either(nodeconflicts.isEmpty, s"conflicting node identities: ${nodeconflicts.mkString(", ")}")
      _ <- _require_either(edgeconflicts.isEmpty, s"conflicting edge identities: ${edgeconflicts.mkString(", ")}")
      _ <- _require_either(dangling.isEmpty, "edges contain dangling endpoints")
    } yield ()
  }

  private def _validate_topology_component_references(
    topology: BokKnowledgeTopology,
    components: Vector[ComponentReference]
  ): Consequence[Unit] =
    _sequence(topology.nodes.flatMap(_.componentReference).distinct.map { reference =>
      val matches = components.filter(component =>
        component.kind.value == reference.kind && component.name.value == reference.name
      )
      _require(
        matches.size == 1,
        s"BoK graph componentRef ${reference.kind}:${reference.name} must match exactly one selected component index entry"
      )
    }).map(_ => ())

  private def _required_array(graph: JsonObject, field: String): Either[String, Vector[Json]] =
    graph(field).flatMap(_.asArray).toRight(s"$field must be an array")

  private def _required_boolean(graph: JsonObject, field: String): Either[String, Boolean] =
    graph(field).flatMap(_.asBoolean).toRight(s"$field must be a boolean")

  private def _required_identifier(objectvalue: JsonObject, field: String, label: String): Either[String, String] =
    _required_string(objectvalue, field, label, _maximum_identifier_length)

  private def _required_label(objectvalue: JsonObject, field: String, label: String): Either[String, String] =
    _required_string(objectvalue, field, label, _maximum_label_length)

  private def _required_string(objectvalue: JsonObject, field: String, label: String, maximum: Int): Either[String, String] =
    objectvalue(field).flatMap(_.asString).filter(_.nonEmpty).toRight(s"$label is required").flatMap { value =>
      _require_either(value.length <= maximum, s"$label exceeds maximum $maximum").map(_ => value)
    }

  private def _optional_label(objectvalue: JsonObject, field: String, label: String): Either[String, Option[String]] =
    objectvalue(field) match {
      case None | Some(Json.Null) => Right(None)
      case Some(value) => value.asString.filter(_.nonEmpty).toRight(s"$label must be a non-empty string").flatMap { text =>
        _require_either(text.length <= _maximum_label_length, s"$label exceeds maximum $_maximum_label_length").map(_ => Some(text))
      }
    }

  private def _optional_identifiers(objectvalue: JsonObject, field: String, label: String, maximum: Int): Either[String, Vector[String]] =
    _optional_strings(objectvalue, field, label, maximum, _maximum_identifier_length)

  private def _optional_labels(objectvalue: JsonObject, field: String, label: String, maximum: Int): Either[String, Vector[String]] =
    _optional_strings(objectvalue, field, label, maximum, _maximum_label_length)

  private def _optional_strings(
    objectvalue: JsonObject,
    field: String,
    label: String,
    maximum: Int,
    maximumlength: Int
  ): Either[String, Vector[String]] =
    objectvalue(field) match {
      case None | Some(Json.Null) => Right(Vector.empty)
      case Some(value) => for {
        values <- value.asArray.toRight(s"$label must be an array")
        _ <- _require_either(values.size <= maximum, s"$label exceeds maximum $maximum")
        result <- _sequence_either(values.zipWithIndex.map { case (entry, index) =>
          entry.asString.filter(_.nonEmpty).toRight(s"$label[$index] must be a non-empty string").flatMap { text =>
            _require_either(text.length <= maximumlength, s"$label[$index] exceeds maximum $maximumlength").map(_ => text)
          }
        })
      } yield result.distinct.sorted
    }

  private def _require_either(condition: Boolean, message: => String): Either[String, Unit] =
    if (condition) Right(()) else Left(message)

  private def _sequence_either[A](values: Vector[Either[String, A]]): Either[String, Vector[A]] =
    values.foldLeft(Right(Vector.empty): Either[String, Vector[A]]) { (result, value) =>
      for {
        collected <- result
        element <- value
      } yield collected :+ element
    }

  private def _parse_manifest(value: String): Consequence[KnowledgeSourceManifest] =
    parser.parse(value).left.map(_.message).flatMap(_.as[KnowledgeSourceManifest]) match {
      case Right(manifest) if manifest.schemaVersion == _manifest_schema =>
        Consequence.success(manifest)
      case Right(manifest) =>
        Consequence.resourceInvalid(s"Unsupported BoK KnowledgeSource schemaVersion: ${manifest.schemaVersion}")
      case Left(message) =>
        Consequence.resourceInvalid(s"Invalid BoK KnowledgeSource manifest: $message")
    }

  private def _resolve_resources(
    base: ResourceReference,
    resources: Vector[KnowledgeSourceResource]
  ): Consequence[Vector[(String, ResourceReference)]] =
    _sequence(resources.map { resource =>
      ResourceReference.resolveC(base, resource.href).map(resource.kind -> _)
    })

  private def _read_terms(
    context: ExecutionContext,
    source: BokKnowledgeSource,
    references: Vector[ResourceReference]
  ): Consequence[Vector[BokTerm]] =
    for {
      indexes <- _sequence(references.map { reference =>
        context.resources.readText(reference).flatMap(_parse_term_index).map(reference -> _)
      })
      terms <- _sequence(indexes.flatMap { case (reference, index) =>
        index.terms.map(_term(source, reference, _))
      })
      _ <- _unique("BoK term", terms.map(_.termId.value))
    } yield terms

  private def _parse_term_index(value: String): Consequence[TermIndex] =
    parser.parse(value).left.map(_.message).flatMap(_.as[TermIndex]) match {
      case Right(index) => Consequence.success(index)
      case Left(message) => Consequence.resourceInvalid(s"Invalid BoK glossary term index: $message")
    }

  private def _term(
    source: BokKnowledgeSource,
    reference: ResourceReference,
    term: TermDocument
  ): Consequence[BokTerm] =
    Consequence.successOrPropertyNotFound(
      s"definition for BoK term ${term.id}",
      term.definitionText.orElse(term.summary).orElse(term.definitionHtml)
    ).map { definition =>
      BokTerm(
        BokTermId(term.id),
        BokTermTitle(term.title),
        BokTermDefinition(definition),
        term.category.map(BokTermCategory.apply),
        BokTermType(term.termType.getOrElse("concept")),
        source.datasetId,
        _evidence(source, reference)
      )
    }

  private def _read_components(
    context: ExecutionContext,
    source: BokKnowledgeSource,
    base: ResourceReference,
    references: Vector[ResourceReference]
  ): Consequence[Vector[ComponentReference]] =
    for {
      indexes <- _sequence(references.map { reference =>
        context.resources.readText(reference).flatMap(_parse_repository_index)
      })
      components <- _sequence(indexes.flatMap(_.artifacts).map { entry =>
        ResourceReference.resolveC(base, s"repository/catalog/${entry.catalog}").map { catalogreference =>
          ComponentReference(
            sourceId = Some(ComponentSourceId(source.sourceId.value)),
            catalogId = Some(ComponentCatalogId(entry.catalog)),
            organization = None,
            name = ComponentName(entry.artifactId),
            title = ComponentTitle(entry.artifactId),
            kind = ComponentKind(entry.kind.name),
            version = entry.recommended.orElse(entry.latestStable).orElse(entry.latestSnapshot).map(ComponentVersion.apply),
            evidence = _evidence(source, catalogreference)
          )
        }
      })
      _ <- _unique("BoK component reference", components.map(x => s"${x.kind.value}:${x.name.value}"))
    } yield components

  private def _parse_repository_index(value: String): Consequence[ComponentRepositoryIndex] =
    ComponentRepositoryIndex.parse(value) match {
      case Right(index) => Consequence.success(index)
      case Left(message) => Consequence.resourceInvalid(message)
    }

  private def _read_component_references(
    context: ExecutionContext,
    source: BokKnowledgeSource,
    base: ResourceReference,
    references: Vector[ResourceReference]
  ): Consequence[Vector[ComponentReference]] =
    for {
      indexes <- _sequence(references.map { reference =>
        context.resources.readText(reference).flatMap(_parse_reference_index).map(reference -> _)
      })
      components <- _sequence(indexes.flatMap { case (reference, index) =>
        index.entries.map(_component_reference(source, base, reference, index.kind, _))
      })
      _ <- _unique("BoK component reference", components.map(x => s"${x.kind.value}:${x.name.value}"))
    } yield components

  private def _parse_reference_index(value: String): Consequence[ComponentReferenceIndex] =
    parser.parse(value).left.map(_.message).flatMap(_.as[ComponentReferenceIndex]) match {
      case Right(index) if index.schemaVersion == _reference_schema && Set("car", "sar").contains(index.kind) =>
        Consequence.success(index)
      case Right(index) if index.schemaVersion != _reference_schema =>
        Consequence.resourceInvalid(s"Unsupported component reference index schemaVersion: ${index.schemaVersion}")
      case Right(index) =>
        Consequence.resourceInvalid(s"Unsupported component reference index kind: ${index.kind}")
      case Left(message) =>
        Consequence.resourceInvalid(s"Invalid BoK component reference index: $message")
    }

  private def _component_reference(
    source: BokKnowledgeSource,
    base: ResourceReference,
    indexreference: ResourceReference,
    indexkind: String,
    entry: ComponentReferenceEntry
  ): Consequence[ComponentReference] =
    for {
      _ <- _require(entry.kind.forall(_ == indexkind),
        s"Component reference kind mismatch for ${entry.name}: index=$indexkind entry=${entry.kind.getOrElse("")}")
      evidencereference <- entry.publicPath match {
        case Some(path) => ResourceReference.resolveC(base, path)
        case None => Consequence.success(indexreference)
      }
    } yield ComponentReference(
      sourceId = Some(ComponentSourceId(source.sourceId.value)),
      catalogId = None,
      organization = None,
      name = ComponentName(entry.name),
      title = ComponentTitle(entry.title.getOrElse(entry.name)),
      kind = ComponentKind(indexkind),
      version = entry.recommended.orElse(entry.latestStable).orElse(entry.latestSnapshot).map(ComponentVersion.apply),
      evidence = _evidence(source, evidencereference)
    )

  private def _evidence(
    source: BokKnowledgeSource,
    reference: ResourceReference
  ): BokEvidence =
    BokEvidence(
      BokEvidenceUri(reference.print),
      source.sourceId,
      Some(BokSourceVersion(source.generation.value)),
      None,
      None
    )

  private def _warnings(
    manifest: KnowledgeSourceManifest,
    terms: Vector[BokTerm],
    components: Vector[ComponentReference]
  ): Vector[BokWarning] = {
    val recognized = manifest.resources.map(_.kind).toSet
    Vector(
      Option.when(!recognized.contains(_glossary_kind))(BokWarning("KnowledgeSource has no glossary-terms resource")),
      Option.when(!recognized.contains(_repository_kind) && !recognized.contains(_reference_kind))(
        BokWarning("KnowledgeSource has no component index resource")
      ),
      Option.when(terms.isEmpty && recognized.contains(_glossary_kind))(BokWarning("KnowledgeSource glossary is empty")),
      Option.when(components.isEmpty &&
        (recognized.contains(_repository_kind) || recognized.contains(_reference_kind)))(
        BokWarning("KnowledgeSource component repository is empty")
      )
    ).flatten
  }

  private def _unique(label: String, values: Vector[String]): Consequence[Unit] = {
    val duplicates = values.groupBy(identity).collect {
      case (value, occurrences) if occurrences.size > 1 => value
    }.toVector.sorted
    _require(duplicates.isEmpty, s"Duplicate $label identities: ${duplicates.mkString(", ")}")
  }

  private def _require(condition: Boolean, message: => String): Consequence[Unit] =
    if (condition) Consequence.unit else Consequence.resourceInvalid(message)

  private def _sequence[A](values: Vector[Consequence[A]]): Consequence[Vector[A]] =
    values.foldLeft(Consequence.success(Vector.empty[A])) { (result, value) =>
      for {
        collected <- result
        element <- value
      } yield collected :+ element
    }

  private final case class KnowledgeSourceManifest(
    schemaVersion: String,
    resources: Vector[KnowledgeSourceResource]
  )

  private final case class KnowledgeSourceResource(
    kind: String,
    href: String
  )

  private final case class TermIndex(terms: Vector[TermDocument])

  private final case class ComponentReferenceIndex(
    schemaVersion: String,
    kind: String,
    entries: Vector[ComponentReferenceEntry]
  )

  private final case class ComponentReferenceEntry(
    name: String,
    title: Option[String],
    kind: Option[String],
    recommended: Option[String],
    latestStable: Option[String],
    latestSnapshot: Option[String],
    publicPath: Option[String]
  )

  private final case class TermDocument(
    id: String,
    title: String,
    category: Option[String],
    definitionText: Option[String],
    definitionHtml: Option[String],
    summary: Option[String],
    termType: Option[String]
  )

  private given Decoder[KnowledgeSourceResource] = (cursor: HCursor) =>
    for {
      kind <- cursor.downField("kind").as[String]
      href <- cursor.downField("href").as[String]
    } yield KnowledgeSourceResource(kind, href)

  private given Decoder[KnowledgeSourceManifest] = (cursor: HCursor) =>
    for {
      schemaversion <- cursor.downField("schemaVersion").as[String]
      resources <- cursor.downField("resources").as[Vector[KnowledgeSourceResource]]
    } yield KnowledgeSourceManifest(schemaversion, resources)

  private given Decoder[TermDocument] = (cursor: HCursor) =>
    for {
      id <- cursor.downField("id").as[String]
      title <- cursor.downField("title").as[String]
      category <- cursor.downField("category").as[Option[String]]
      definitiontext <- cursor.downField("definition_text").as[Option[String]]
      definitionhtml <- cursor.downField("definition_html").as[Option[String]]
      summary <- cursor.downField("summary").as[Option[String]]
      termtype <- cursor.downField("term_type").as[Option[String]]
    } yield TermDocument(id, title, category, definitiontext, definitionhtml, summary, termtype)

  private given Decoder[TermIndex] = (cursor: HCursor) =>
    cursor.downField("terms").as[Vector[TermDocument]].map(TermIndex.apply)

  private given Decoder[ComponentReferenceEntry] = (cursor: HCursor) =>
    for {
      name <- cursor.downField("name").as[String]
      title <- cursor.downField("title").as[Option[String]]
      kind <- cursor.downField("kind").as[Option[String]]
      recommended <- cursor.downField("recommended").as[Option[String]]
      lateststable <- cursor.downField("latest_stable").as[Option[String]]
      latestsnapshot <- cursor.downField("latest_snapshot").as[Option[String]]
      publicpath <- cursor.downField("public_path").as[Option[String]]
    } yield ComponentReferenceEntry(
      name,
      title,
      kind,
      recommended,
      lateststable,
      latestsnapshot,
      publicpath
    )

  private given Decoder[ComponentReferenceIndex] = (cursor: HCursor) =>
    for {
      schemaversion <- cursor.downField("schemaVersion").as[String]
      kind <- cursor.downField("kind").as[String]
      entries <- cursor.downField("entries").as[Vector[ComponentReferenceEntry]]
    } yield ComponentReferenceIndex(schemaversion, kind, entries)
}
