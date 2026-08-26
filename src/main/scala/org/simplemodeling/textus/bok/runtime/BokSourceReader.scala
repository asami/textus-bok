package org.simplemodeling.textus.bok.runtime

import io.circe.{Decoder, HCursor, Json, JsonObject}
import io.circe.parser
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.goldenport.Consequence
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.knowledge.{
  ComponentKnowledgeManifestConsumerContractCodec,
  ComponentKnowledgeManifestConsumerResourceEvidence,
  PublicMetadataVisibility
}
import org.goldenport.cncf.repository.ComponentRepositoryIndex
import org.goldenport.cncf.resource.ResourceReference
import org.simplemodeling.textus.bok.datatype.*
import org.simplemodeling.textus.bok.value.*

/*
 * Metadata-only BoK source reader over the CNCF ResourceAccess DSL.
 *
 * @since   Jul. 21, 2026
 *  version Jul. 24, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
final case class NormalizedBokSource(
  source: BokKnowledgeSource,
  terms: Vector[BokTerm],
  components: Vector[ComponentReference],
  warnings: Vector[BokWarning],
  topology: BokKnowledgeTopology = BokKnowledgeTopology.empty,
  semanticRecords: Vector[BokSemanticRecord] = Vector.empty
)

final case class BokKnowledgeSourceReference(kind: String, value: String, uri: Option[String])
final case class BokKnowledgeComponentReference(
  kind: String,
  name: String,
  organization: Option[String] = None,
  version: Option[String] = None
)
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
  private val _manifest_kind = "component-manifest"
  private val _consumer_kind = "component-knowledge-consumer-contract"
  private val _semantic_kind = "semantic-index"
  private val _semantic_schema = "cncf.semantic-index.v1"
  private val _semantic_kinds = Set(_manifest_kind, _consumer_kind, _semantic_kind, "smartdox-document", "smartdox-section", "rdf-jsonld", "glossary", "ontology", "schema", "catalog")
  private val _semantic_source_kinds = Set(_consumer_kind, _semantic_kind)
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
      semanticresources <- _resolve_semantic_resources(base, manifest.resources)
      recognized = references.filter(x =>
        x._1 == _glossary_kind || x._1 == _repository_kind || x._1 == _reference_kind || _semantic_source_kinds.contains(x._1)
      )
      _ <- _validate_semantic_manifest(manifest.resources)
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
      _ <- _unique("BoK component reference", components.map(BokComponentReferenceIdentity.identityKey))
      topology <- _read_topology(context, source, references.filter(_._1 == _graph_kind).map(_._2))
      _ <- _validate_topology_component_references(topology, components)
      semanticrecords <- _read_semantic(context, source, semanticresources, components)
      warnings = _warnings(manifest, terms, components) ++ Option.when(topology.truncated)(BokWarning("BoK graph summary is truncated"))
    } yield NormalizedBokSource(
      source,
      terms.sortBy(_.termId.value),
      components.sortBy(BokComponentReferenceIdentity.orderKey),
      warnings,
      topology,
      semanticrecords
    )

  private def _validate_semantic_manifest(resources: Vector[KnowledgeSourceResource]): Consequence[Unit] = {
    val semantic = resources.filter(x => _semantic_source_kinds.contains(x.kind))
    if (semantic.isEmpty) Consequence.unit
    else for {
      _ <- _require(semantic.count(_.kind == _consumer_kind) == 1, "BoK semantic source requires one consumer contract")
      _ <- _require(semantic.count(_.kind == _semantic_kind) == 1, "BoK semantic source requires one semantic index")
      _ <- _require(semantic.map(_.kind).distinct.size == semantic.size, "BoK semantic source contains duplicate resources")
    } yield ()
  }

  private def _resolve_semantic_resources(base: ResourceReference, resources: Vector[KnowledgeSourceResource]): Consequence[Vector[(KnowledgeSourceResource, ResourceReference)]] =
    _sequence(resources.filter(x => _semantic_source_kinds.contains(x.kind)).map { resource =>
      if (!_safe_relative_href(resource.href)) Consequence.resourceInvalid("BoK semantic resource href is unsafe")
      else resource.sha256 match {
        case Some(digest) if digest.matches("[0-9a-f]{64}") => ResourceReference.resolveC(base, resource.href).map(resource -> _)
        case _ => Consequence.resourceInvalid("BoK semantic resource requires a valid SHA-256 digest")
      }
    })

  private def _safe_relative_href(value: String): Boolean = {
    val href = value.trim
    href.nonEmpty && !href.startsWith("/") && !href.contains("://") && !href.split('/').contains("..")
  }

  private def _read_semantic(context: ExecutionContext, source: BokKnowledgeSource, resources: Vector[(KnowledgeSourceResource, ResourceReference)], components: Vector[ComponentReference]): Consequence[Vector[BokSemanticRecord]] = {
    val consumer = resources.find(_._1.kind == _consumer_kind)
    val index = resources.find(_._1.kind == _semantic_kind)
    (consumer, index) match {
      case (None, None) => Consequence.success(Vector.empty)
      case (Some(contract), Some(semanticindex)) => for {
        contracttext <- _read_verified(context, contract._1, contract._2)
        indextext <- _read_verified(context, semanticindex._1, semanticindex._2)
        consumerrecords <- _decode_consumer(source, contracttext)
        indexrecords <- _decode_semantic_index(source, indextext, components)
        records = consumerrecords ++ indexrecords
        _ <- _unique("BoK semantic record", records.map(_.identity))
      } yield records
      case _ => Consequence.resourceInvalid("BoK semantic resources are incomplete")
    }
  }

  private def _read_verified(context: ExecutionContext, resource: KnowledgeSourceResource, reference: ResourceReference): Consequence[String] =
    context.resources.readText(reference).flatMap { text =>
      if (resource.sha256.contains(_sha256(text))) Consequence.success(text)
      else Consequence.resourceInvalid("BoK semantic resource digest mismatch")
    }

  private def _decode_consumer(source: BokKnowledgeSource, value: String): Consequence[Vector[BokSemanticRecord]] =
    ComponentKnowledgeManifestConsumerContractCodec.decodeC(value)
      .recoverWith(_ => Consequence.resourceInvalid("Invalid BoK component knowledge consumer contract"))
      .flatMap(_consumer_records(source, _))

  private def _consumer_records(
    source: BokKnowledgeSource,
    contract: org.goldenport.cncf.knowledge.ComponentKnowledgeManifestConsumerContract
  ): Consequence[Vector[BokSemanticRecord]] = {
    val records = contract.frameworkPublication.toVector.map(_framework_publication_record(source, _)) ++
      contract.publicDirective.toVector.map(_public_directive_record(source, contract.resources, _)) ++
      contract.skillCatalog.toVector.map(_skill_catalog_record(source, contract.resources, _))
    _sequence(records.map(_validate_consumer_record))
  }

  private def _validate_consumer_record(record: BokSemanticRecord): Consequence[BokSemanticRecord] = {
    val optionalmetadata = Vector(
      "product" -> record.product,
      "version" -> record.version,
      "profile" -> record.profile,
      "owner" -> record.owner,
      "license" -> record.license,
      "logicalPath" -> record.logicalPath,
      "chunkId" -> record.chunkId,
      "publicationGeneration" -> record.publicationGeneration,
      "publicationDigest" -> record.publicationDigest
    )
    optionalmetadata.collectFirst {
      case (field, Some(value)) if !_is_public_metadata(value) =>
        s"BoK consumer record.$field must be a non-empty public string"
    } match {
      case Some(message) => Consequence.resourceInvalid(message)
      case None => record.logicalPath match {
        case Some(value) if !_safe_relative_logical_path(value) =>
          Consequence.resourceInvalid("BoK consumer record.logicalPath must be a safe relative logical path")
        case _ => record.publicationDigest match {
          case Some(value) if !value.matches("[0-9a-f]{64}") =>
            Consequence.resourceInvalid("BoK consumer record.publicationDigest must be a lowercase SHA-256 digest")
          case _ => Consequence.success(record)
        }
      }
    }
  }

  private def _framework_publication_record(
    source: BokKnowledgeSource,
    value: org.goldenport.cncf.knowledge.ComponentKnowledgeManifestConsumerFrameworkPublicationEvidence
  ): BokSemanticRecord = {
    val title = s"${value.product} ${value.version}"
    BokSemanticRecord(
      "framework-publication",
      value.sourceIdentity,
      title,
      s"Framework publication metadata for $title.",
      value.documentId,
      value.sectionId,
      value.canonicalUrl,
      value.publicationGeneration,
      "public",
      "framework-publication",
      source.sourceId.value,
      source.datasetId.value,
      source.generation.value,
      value.sourceSha256,
      false,
      product = Some(value.product),
      version = Some(value.version),
      publicationGeneration = Some(value.publicationGeneration),
      publicationDigest = Some(value.sha256)
    )
  }

  private def _public_directive_record(
    source: BokKnowledgeSource,
    resources: Vector[ComponentKnowledgeManifestConsumerResourceEvidence],
    value: org.goldenport.cncf.knowledge.ComponentKnowledgeManifestConsumerPublicDirectiveMetadata
  ): BokSemanticRecord = {
    val (logicalpath, license) = _public_resource_metadata(resources, value.logicalIdentity.logicalResource)
    BokSemanticRecord(
      "directive-metadata",
      value.logicalIdentity.logicalResource,
      value.directiveId,
      s"Directive ${value.directiveId} rule ${value.ruleId}.",
      value.directiveId,
      Some(value.ruleId),
      value.guideReference,
      source.generation.value,
      _public_visibility(value.visibility),
      value.authority.code,
      source.sourceId.value,
      source.datasetId.value,
      source.generation.value,
      value.sourceSha256,
      false,
      version = Some(value.version),
      profile = Some(value.profileId),
      license = license,
      logicalPath = logicalpath
    )
  }

  private def _skill_catalog_record(
    source: BokKnowledgeSource,
    resources: Vector[ComponentKnowledgeManifestConsumerResourceEvidence],
    value: org.goldenport.cncf.knowledge.ComponentKnowledgeManifestConsumerSkillCatalogMetadata
  ): BokSemanticRecord = {
    val (logicalpath, license) = _public_resource_metadata(resources, value.logicalIdentity.logicalResource)
    BokSemanticRecord(
      "skill-metadata",
      value.logicalIdentity.logicalResource,
      value.catalogId,
      s"${value.purpose} (owner: ${value.owner}, version: ${value.version}).",
      value.catalogId,
      None,
      value.installationReference,
      source.generation.value,
      _public_visibility(value.visibility),
      "skill-catalog",
      source.sourceId.value,
      source.datasetId.value,
      source.generation.value,
      value.sourceSha256,
      false,
      version = Some(value.version),
      owner = Some(value.owner),
      license = license,
      logicalPath = logicalpath
    )
  }

  private def _public_resource_metadata(
    resources: Vector[ComponentKnowledgeManifestConsumerResourceEvidence],
    logicalresource: String
  ): (Option[String], Option[String]) =
    resources.filter(_.logicalIdentity.logicalResource == logicalresource) match {
      case Vector(resource) => (Some(resource.logicalPath), Some(resource.metadata.license))
      case _ => (None, None)
    }

  private def _public_visibility(value: PublicMetadataVisibility): String = value match {
    case PublicMetadataVisibility.Public | PublicMetadataVisibility.Ecosystem => "public"
    case _ => "restricted"
  }

  private def _decode_semantic_index(
    source: BokKnowledgeSource,
    value: String,
    components: Vector[ComponentReference]
  ): Consequence[Vector[BokSemanticRecord]] =
    parser.parse(value).left.map(_.message).flatMap(_.asObject.toRight("semantic index must be a JSON object")) match {
      case Left(message) => Consequence.resourceInvalid(s"Invalid BoK semantic index: $message")
      case Right(index) if !index("schemaVersion").flatMap(_.asString).contains(_semantic_schema) || !index("kind").flatMap(_.asString).contains(_semantic_kind) => Consequence.resourceInvalid("Unsupported semantic index kind or schema")
      case Right(index) if index("sourceId").flatMap(_.asString).forall(_ != source.sourceId.value) || index("datasetId").flatMap(_.asString).forall(_ != source.datasetId.value) || index("generation").flatMap(_.asString).forall(_ != source.generation.value) => Consequence.resourceInvalid("Semantic index source, dataset, or generation mismatch")
      case Right(index) => index("records").flatMap(_.asArray) match {
        case Some(records) => _sequence(records.toVector.map(_decode_record(source, _, components)))
        case None => Consequence.resourceInvalid("Semantic index records must be an array")
      }
    }

  private def _decode_record(source: BokKnowledgeSource, value: Json, components: Vector[ComponentReference]): Consequence[BokSemanticRecord] = value.asObject match {
    case None => Consequence.resourceInvalid("Semantic index record must be an object")
    case Some(record) =>
      val fields = Vector("kind", "identity", "title", "summary", "documentId", "canonicalUrl", "indexedAt", "visibility", "authority", "sha256")
      val strings = fields.map(x => record(x).flatMap(_.asString))
      val supported = Set(_manifest_kind, _consumer_kind, _semantic_kind, "smartdox-document", "smartdox-section", "rdf-jsonld", "glossary", "ontology", "schema", "catalog")
      if (strings.exists(_.forall(value => !_is_public_metadata(value)))) Consequence.resourceInvalid("Semantic index record is missing public metadata")
      else if (!supported.contains(strings.head.get) || !strings(5).get.startsWith("https://") || !strings(9).get.matches("[0-9a-f]{64}")) Consequence.resourceInvalid("Invalid semantic index record metadata")
      else {
        val decoded = for {
          sectionid <- _semantic_optional_string(record, "sectionId", "Semantic index record.sectionId")
          product <- _semantic_optional_string(record, "product", "Semantic index record.product")
          version <- _semantic_optional_string(record, "version", "Semantic index record.version")
          profile <- _semantic_optional_string(record, "profile", "Semantic index record.profile")
          owner <- _semantic_optional_string(record, "owner", "Semantic index record.owner")
          license <- _semantic_optional_string(record, "license", "Semantic index record.license")
          logicalpath <- _semantic_optional_string(record, "logicalPath", "Semantic index record.logicalPath")
          _ <- logicalpath.map(value => _require_either(_safe_relative_logical_path(value), "Semantic index record.logicalPath must be a safe relative logical path")).getOrElse(Right(()))
          chunkid <- _semantic_optional_string(record, "chunkId", "Semantic index record.chunkId")
          publicationgeneration <- _semantic_optional_string(record, "publicationGeneration", "Semantic index record.publicationGeneration")
          publicationdigest <- _semantic_optional_string(record, "publicationDigest", "Semantic index record.publicationDigest")
          _ <- publicationdigest.map(value => _require_either(value.matches("[0-9a-f]{64}"), "Semantic index record.publicationDigest must be a lowercase SHA-256 digest")).getOrElse(Right(()))
          componentreference <- _decode_semantic_component_reference(record, components)
        } yield BokSemanticRecord(
          strings(0).get,
          strings(1).get,
          strings(2).get,
          strings(3).get,
          strings(4).get,
          sectionid,
          strings(5).get,
          strings(6).get,
          strings(7).get,
          strings(8).get,
          source.sourceId.value,
          source.datasetId.value,
          source.generation.value,
          strings(9).get,
          record("stale").flatMap(_.asBoolean).getOrElse(false),
          componentreference,
          product,
          version,
          profile,
          owner,
          license,
          logicalpath,
          chunkid,
          publicationgeneration,
          publicationdigest
        )
        decoded.fold(
          message => Consequence.resourceInvalid(message),
          record => Consequence.success(record)
        )
      }
  }

  private def _decode_semantic_component_reference(
    record: JsonObject,
    components: Vector[ComponentReference]
  ): Either[String, Option[ComponentReference]] =
    record("componentReference") match {
      case None | Some(Json.Null) => Right(None)
      case Some(value) => for {
        reference <- value.asObject.toRight("Semantic index componentReference must be an object")
        kind <- reference("kind").flatMap(_.asString).filter(_is_public_metadata).toRight("Semantic index componentReference.kind is required")
        name <- reference("name").flatMap(_.asString).filter(_is_public_metadata).toRight("Semantic index componentReference.name is required")
        organization <- _semantic_optional_string(reference, "organization", "Semantic index componentReference.organization")
        version <- _semantic_optional_string(reference, "version", "Semantic index componentReference.version")
      } yield components.filter { component =>
        BokComponentReferenceIdentity.matches(component, kind, organization, name) &&
          version.forall(value => component.version.exists(_.value == value))
      } match {
        case Vector(single) => Some(single)
        case _ => None
      }
    }

  private def _semantic_optional_string(value: JsonObject, field: String, label: String): Either[String, Option[String]] =
    value(field) match {
      case None | Some(Json.Null) => Right(None)
      case Some(json) => json.asString.filter(_is_public_metadata).toRight(s"$label must be a non-empty public string").map(Some(_))
    }

  private def _is_public_metadata(value: String): Boolean =
    value.trim.nonEmpty && !value.contains("<") && !value.contains(">")

  private def _safe_relative_logical_path(value: String): Boolean = {
    val segments = value.split("/", -1)
    value == value.trim &&
      value.nonEmpty &&
      !value.startsWith("/") &&
      !value.matches("(?i)^[a-z]:.*") &&
      !value.contains("\\") &&
      !value.exists(character => Character.isISOControl(character)) &&
      segments.forall(segment => segment.nonEmpty && segment != "." && segment != "..")
  }

  private def _sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)).map(x => f"${x & 0xff}%02x").mkString

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
        organization <- _optional_identifier(reference, "organization", s"$label.organization")
        version <- _optional_identifier(reference, "version", s"$label.version")
      } yield Some(BokKnowledgeComponentReference(kind, name, organization, version))
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
        BokComponentReferenceIdentity.matches(
          component,
          reference.kind,
          reference.organization,
          reference.name
        ) &&
          reference.version.forall(value => component.version.exists(_.value == value))
      )
      _require(
        matches.size == 1,
        s"BoK graph componentRef ${_component_reference_identity(reference)} must match exactly one selected component index entry"
      )
    }).map(_ => ())

  private def _component_reference_identity(reference: BokKnowledgeComponentReference): String =
    Vector(
      Some(BokComponentReferenceIdentity.key(
        reference.kind,
        reference.organization,
        reference.name
      ).key),
      reference.version.map(value => s"version=$value")
    ).flatten.mkString(" ")

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

  private def _optional_identifier(objectvalue: JsonObject, field: String, label: String): Either[String, Option[String]] =
    objectvalue(field) match {
      case None | Some(Json.Null) => Right(None)
      case Some(value) => value.asString.filter(_.nonEmpty).toRight(s"$label must be a non-empty string").flatMap { text =>
        _require_either(text.length <= _maximum_identifier_length, s"$label exceeds maximum $_maximum_identifier_length").map(_ => Some(text))
      }
    }

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
    parser.parse(value).left.map(_.message).flatMap(_.asObject.toRight("manifest must be a JSON object")) match {
      case Left(message) =>
        Consequence.resourceInvalid(s"Invalid BoK KnowledgeSource manifest: $message")
      case Right(manifest) =>
        val parsed = for {
          schemaversion <- _manifest_string(manifest, "schemaVersion")
          values <- manifest("resources").flatMap(_.asArray).toRight("manifest field resources must be an array")
          resources <- _sequence_either(values.toVector.zipWithIndex.map { case (value, index) =>
            _parse_manifest_resource(value, index)
          })
        } yield KnowledgeSourceManifest(schemaversion, resources)
        parsed match {
          case Right(parsedmanifest) if parsedmanifest.schemaVersion == _manifest_schema =>
            Consequence.success(parsedmanifest)
          case Right(parsedmanifest) =>
            Consequence.resourceInvalid(s"Unsupported BoK KnowledgeSource schemaVersion: ${parsedmanifest.schemaVersion}")
          case Left(message) =>
            Consequence.resourceInvalid(s"Invalid BoK KnowledgeSource manifest: $message")
        }
    }

  private def _manifest_string(value: JsonObject, field: String): Either[String, String] =
    value(field).flatMap(_.asString).filter(_.nonEmpty).toRight(s"manifest field $field must be a non-empty string")

  private def _parse_manifest_resource(value: Json, index: Int): Either[String, KnowledgeSourceResource] =
    value.asObject.toRight(s"manifest resources[$index] must be an object").flatMap { resource =>
      for {
        kind <- _manifest_string(resource, "kind")
        href <- _manifest_string(resource, "href")
        sha256 <- resource("sha256") match {
          case None | Some(Json.Null) => Right(None)
          case Some(value) => value.asString.filter(_.nonEmpty)
            .toRight(s"manifest resources[$index] field sha256 must be a non-empty string")
            .map(Some(_))
        }
      } yield KnowledgeSourceResource(kind, href, sha256)
    }

  private def _resolve_resources(
    base: ResourceReference,
    resources: Vector[KnowledgeSourceResource]
  ): Consequence[Vector[(String, ResourceReference)]] =
    _sequence(resources.map { resource =>
      if (!_safe_relative_href(resource.href)) Consequence.resourceInvalid("BoK KnowledgeSource resource href is unsafe")
      else ResourceReference.resolveC(base, resource.href).map(resource.kind -> _)
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
            organization = entry.namespace.map(ComponentOrganization.apply),
            name = ComponentName(entry.artifactId),
            title = ComponentTitle(entry.artifactId),
            kind = ComponentKind(entry.kind.name),
            version = entry.recommended.orElse(entry.latestStable).orElse(entry.latestSnapshot).map(ComponentVersion.apply),
            evidence = _evidence(source, catalogreference)
          )
        }
      })
      _ <- _unique("BoK component reference", components.map(BokComponentReferenceIdentity.identityKey))
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
      _ <- _unique("BoK component reference", components.map(BokComponentReferenceIdentity.identityKey))
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
    href: String,
    sha256: Option[String]
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
