package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}

final case class Confidence private (asFloat: Float) extends AnyVal

object Confidence {
  def apply(asFloat: Float): ExceptionOr[Confidence] =
    asFloat match {
      case f if f >= 0 && f <= 100 => Right(new Confidence(f))
      case badConfidence           => Left(GenericException(s"Bad confidence value $badConfidence"))
    }
}
