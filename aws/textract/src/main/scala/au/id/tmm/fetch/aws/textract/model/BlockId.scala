package au.id.tmm.fetch.aws.textract.model

import java.util.UUID

import au.id.tmm.utilities.errors.ExceptionOr
import io.circe.{Codec, Decoder, Encoder}

final case class BlockId(asUUID: UUID) extends AnyVal

object BlockId {
  def fromString(string: String): ExceptionOr[BlockId] =
    ExceptionOr.catchIn(BlockId(UUID.fromString(string)))

  implicit val codec: Codec[BlockId] = Codec.from(
    Decoder[UUID].map(BlockId.apply),
    Encoder[UUID].contramap(_.asUUID),
  )
}
