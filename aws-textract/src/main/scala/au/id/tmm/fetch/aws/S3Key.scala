package au.id.tmm.fetch.aws

import au.id.tmm.collections.NonEmptyArraySeq

import scala.collection.immutable.ArraySeq

final case class S3Key(pathElements: NonEmptyArraySeq[String]) extends AnyVal {
  def resolve(that: S3Key): S3Key  = S3Key(this.pathElements appendedAll that.pathElements)
  def resolve(that: String): S3Key = resolve(S3Key(that))
  def toRaw: String                = this.pathElements.iterator.mkString(sep = "/")
}

object S3Key {
  def apply(asString: String): S3Key =
    new S3Key(
      NonEmptyArraySeq
        .fromArraySeq(ArraySeq.unsafeWrapArray(asString.split('/')))
        .getOrElse(NonEmptyArraySeq.of("")),
    )
  def apply(headPathElement: String, tailPathElements: String*): S3Key =
    new S3Key(NonEmptyArraySeq.fromHeadTail(headPathElement, tailPathElements))
}
