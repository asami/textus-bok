package org.simplemodeling.textus.bok.runtime

import io.circe.{Decoder, HCursor}
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
 * @version Jul. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final case class NormalizedBokSource(
  source: BokKnowledgeSource,
  terms: Vector[BokTerm],
  components: Vector[ComponentReference],
  warnings: Vector[BokWarning]
)

object BokSourceReader {
  private val _manifest_path = "metadata/cncf/knowledge-source.json"
  private val _manifest_schema = "cncf.knowledge-source.v1"
  private val _glossary_kind = "glossary-terms"
  private val _repository_kind = "component-repository-index"
  private val _reference_kind = "component-reference-index"
  private val _reference_schema = "cncf.component-reference-index.v1"

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
      warnings = _warnings(manifest, terms, components)
    } yield NormalizedBokSource(
      source,
      terms.sortBy(_.termId.value),
      components.sortBy(x => (x.kind.value, x.name.value, x.version.map(_.value).getOrElse(""))),
      warnings
    )

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
