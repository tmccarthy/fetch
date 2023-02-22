package au.id.tmm.fetch.aws.s3

import au.id.tmm.collections.NonEmptyArraySeq

import scala.collection.immutable.ArraySeq

// TODO need some way to model a key where it is fixed to the start of the root. Or, some way to represent an S3 Key
//      segment that hasn't been resolved against the root of a bucket
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
