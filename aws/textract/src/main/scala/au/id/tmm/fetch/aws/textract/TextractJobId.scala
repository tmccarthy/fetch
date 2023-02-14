package au.id.tmm.fetch.aws.textract

import au.id.tmm.utilities.errors.ExceptionOr
import io.circe.{Codec, Decoder, Encoder}

final case class TextractJobId(asString: String) extends AnyVal

object TextractJobId {
  def fromString(string: String): ExceptionOr[TextractJobId] =
    Right(TextractJobId(string))

  implicit val codec: Codec[TextractJobId] = Codec.from(
    Decoder[String].map(TextractJobId.apply),
    Encoder[String].contramap(_.asString),
  )
}
