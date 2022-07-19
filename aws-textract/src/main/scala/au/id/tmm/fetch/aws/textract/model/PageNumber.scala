package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import io.circe.{Codec, Decoder, Encoder}

final case class PageNumber private (asInt: Int) extends AnyVal

object PageNumber {
  def apply(asInt: Int): ExceptionOr[PageNumber] =
    asInt match {
      case p if p > 0 => Right(new PageNumber(p))
      case badPage    => Left(GenericException(s"Bad confidence value $badPage"))
    }

  val `1`: PageNumber  = PageNumber(1).fold(e => throw new AssertionError(e), p => p)
  val `2`: PageNumber  = PageNumber(2).fold(e => throw new AssertionError(e), p => p)
  val `3`: PageNumber  = PageNumber(3).fold(e => throw new AssertionError(e), p => p)
  val `4`: PageNumber  = PageNumber(4).fold(e => throw new AssertionError(e), p => p)
  val `5`: PageNumber  = PageNumber(5).fold(e => throw new AssertionError(e), p => p)
  val `6`: PageNumber  = PageNumber(6).fold(e => throw new AssertionError(e), p => p)
  val `7`: PageNumber  = PageNumber(7).fold(e => throw new AssertionError(e), p => p)
  val `8`: PageNumber  = PageNumber(8).fold(e => throw new AssertionError(e), p => p)
  val `9`: PageNumber  = PageNumber(9).fold(e => throw new AssertionError(e), p => p)
  val `10`: PageNumber = PageNumber(10).fold(e => throw new AssertionError(e), p => p)

  implicit val ordering: Ordering[PageNumber] = Ordering.by(_.asInt)

  implicit val codec: Codec[PageNumber] = Codec.from(
    Decoder[Int].map(PageNumber.apply).emap(_.left.map(_.toString)),
    Encoder[Int].contramap(_.asInt),
  )
}
