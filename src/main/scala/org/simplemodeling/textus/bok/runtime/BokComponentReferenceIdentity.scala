package org.simplemodeling.textus.bok.runtime

import org.simplemodeling.textus.bok.value.ComponentReference

/*
 * Canonical BoK component-reference identity and ordering.
 *
 * @since   Aug. 14, 2026
 * @version Aug. 14, 2026
 * @author  ASAMI, Tomoharu
 */

/**
 * Canonical identity and ordering for an existence-only component reference.
 *
 * Organization is an explicit optional identity dimension. An unqualified
 * reference therefore remains distinct from every qualified reference while
 * retaining the legacy two-value ID input used for organization-less records.
 */
object BokComponentReferenceIdentity {
  final case class Key(
    kind: String,
    organization: Option[String],
    name: String
  ) {
    /** Stable values for IDs: None intentionally keeps the legacy pair. */
    def values: Vector[String] = organization match {
      case Some(value) => Vector(kind, value, name)
      case None => Vector(kind, name)
    }

    /** Stable textual key with explicit None/Some semantics. */
    def key: String = organization match {
      case Some(value) => Vector(kind, "some", value, name).mkString("\u0000")
      case None => Vector(kind, "none", name).mkString("\u0000")
    }

    /** Kind, unqualified-before-qualified, organization, name, version. */
    def orderKey(version: Option[String]): (String, Int, String, String, String) =
      (kind, organization.fold(0)(_ => 1), organization.getOrElse(""), name, version.getOrElse(""))
  }

  def key(component: ComponentReference): Key =
    Key(component.kind.value, component.organization.map(_.value), component.name.value)

  def key(kind: String, organization: Option[String], name: String): Key =
    Key(kind, organization, name)

  def identityValues(component: ComponentReference): Vector[String] =
    key(component).values

  def identityKey(component: ComponentReference): String =
    key(component).key

  def orderKey(component: ComponentReference): (String, Int, String, String, String) =
    key(component).orderKey(component.version.map(_.value))

  /** Match an optional organization as an exact qualifier or wildcard. */
  def matches(
    component: ComponentReference,
    kind: String,
    organization: Option[String],
    name: String
  ): Boolean = {
    val identity = key(component)
    identity.kind == kind &&
      identity.name == name &&
      organization.forall(value => identity.organization.contains(value))
  }
}
