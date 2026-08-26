package org.simplemodeling.textus.bok.runtime

import org.simplemodeling.textus.bok.value.{BokEvidence, ComponentReference}

/*
 * Runtime semantic metadata admitted from structured sources.
 *
 * @since   Aug. 26, 2026
 * @version Aug. 27, 2026
 * @author  ASAMI, Tomoharu
 */
/** Runtime-only semantic metadata admitted from the two structured source resources. */
final case class BokSemanticRecord(
  kind: String,
  identity: String,
  title: String,
  summary: String,
  documentId: String,
  sectionId: Option[String],
  canonicalUrl: String,
  indexedAt: String,
  visibility: String,
  authority: String,
  sourceId: String,
  datasetId: String,
  generation: String,
  digest: String,
  stale: Boolean,
  componentReference: Option[ComponentReference] = None
) {
  def evidence: BokEvidence = BokEvidence(
    org.simplemodeling.textus.bok.datatype.BokEvidenceUri(canonicalUrl),
    org.simplemodeling.textus.bok.datatype.BokSourceId(sourceId),
    Some(org.simplemodeling.textus.bok.datatype.BokSourceVersion(generation)),
    None,
    None
  )
}

/** Selected profile and caller disclosure boundary for direct semantic reads. */
final case class BokSemanticAccess(
  selection: ResolvedBokProfile,
  privilege: BokSemanticAccess.CallerPrivilege
) {
  def permits(record: BokSemanticRecord): Boolean = privilege match {
    case BokSemanticAccess.CallerPrivilege.Public => record.visibility == "public"
    case BokSemanticAccess.CallerPrivilege.MetadataReader => true
  }
}

object BokSemanticAccess {
  enum CallerPrivilege {
    case Public
    case MetadataReader
  }
}
