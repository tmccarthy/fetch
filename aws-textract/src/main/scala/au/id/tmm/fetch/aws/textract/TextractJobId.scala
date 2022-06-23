package au.id.tmm.fetch.aws.textract

import au.id.tmm.utilities.errors.ExceptionOr

final case class TextractJobId(asString: String) extends AnyVal

object TextractJobId {
  def fromString(string: String): ExceptionOr[TextractJobId] =
    Right(TextractJobId(string))
}
