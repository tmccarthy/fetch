package au.id.tmm.fetch.json

import io.circe.Decoder

sealed trait UnrecognisedOr[+U, +A] {
  def asOption: Option[A] = this match {
    case UnrecognisedOr.Of(value)       => Some(value)
    case UnrecognisedOr.Unrecognised(_) => None
  }

  def asEither[E](leftF: U => E): Either[E, A] = this match {
    case UnrecognisedOr.Of(value)          => Right(value)
    case UnrecognisedOr.Unrecognised(json) => Left(leftF(json))
  }
}

object UnrecognisedOr {
  final case class Of[A](value: A)                  extends UnrecognisedOr[Nothing, A]
  final case class Unrecognised[U](unrecognised: U) extends UnrecognisedOr[U, Nothing]

  implicit def decoder[U : Decoder, A : Decoder]: Decoder[UnrecognisedOr[U, A]] =
    Decoder[A].map(Of.apply) or Decoder[U].map(Unrecognised.apply)
}
