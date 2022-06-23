package au.id.tmm.fetch.datagovau.model.response

import java.net.URI
import java.time.ZonedDateTime

import au.id.tmm.fetch.json._
import io.circe.Decoder

import scala.collection.immutable.ArraySeq

final case class Dataset(
  identifier: Dataset.Identifier,
  title: String,
  description: Option[String],
  indexed: ZonedDateTime,
  issued: ZonedDateTime,
  modified: Option[ZonedDateTime],
  landingPage: URI,
  publisher: Dataset.Publisher,
  distributions: ArraySeq[Dataset.Distribution],
)

object Dataset {
  final case class Identifier(asString: String) extends AnyVal

  object Identifier {
    implicit val decoder: Decoder[Identifier] = Decoder[String].map(Identifier.apply)
  }

  final case class Publisher(
    identifier: Publisher.Identifier,
    name: String,
    description: Option[String],
  )

  object Publisher {
    final case class Identifier(asString: String) extends AnyVal

    object Identifier {
      implicit val decoder: Decoder[Identifier] = Decoder[String].map(Identifier.apply)
    }

    implicit val decoder: Decoder[Publisher] = Decoder.forProduct3("identifier", "name", "description")(Publisher.apply)
  }

  final case class Distribution(
    download: URI,
    format: UnrecognisedStringOr[Distribution.Format],
    identifier: Distribution.Identifier,
    issued: ZonedDateTime,
    modified: Option[ZonedDateTime],
    mediaType: Distribution.MediaType,
    title: String,
  )

  object Distribution {
    sealed trait Format

    object Format {
      case object Csv extends Format

      implicit val decoder: Decoder[Format] = Decoder[String].emap {
        case "CSV" => Right(Csv)
        case other => Left(other)
      }
    }

    final case class Identifier(asString: String) extends AnyVal

    object Identifier {
      implicit val decoder: Decoder[Identifier] = Decoder[String].map(Identifier.apply)
    }

    final case class MediaType(asString: String) extends AnyVal

    object MediaType {
      implicit val decoder: Decoder[MediaType] = Decoder[String].map(MediaType.apply)
    }

    implicit val decoder: Decoder[Distribution] =
      Decoder.forProduct7("downloadURL", "format", "identifier", "issued", "modified", "mediaType", "title")(
        Distribution.apply,
      )
  }

  implicit val decoder: Decoder[Dataset] = Decoder.forProduct9(
    "identifier",
    "title",
    "description",
    "indexed",
    "issued",
    "modified",
    "landingPage",
    "publisher",
    "distributions",
  )(Dataset.apply)
}
