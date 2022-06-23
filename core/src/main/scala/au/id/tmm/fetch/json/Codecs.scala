package au.id.tmm.fetch.json

import java.net.URI

import io.circe.{Decoder, Encoder}

import scala.util.Try

private[json] trait Codecs {

  implicit val decodeJavaUri: Decoder[URI] = Decoder[String].emapTry(s => Try(URI.create(s)))
  implicit val encodeJavaUri: Encoder[URI] = Encoder[String].contramap(_.toString)

}
