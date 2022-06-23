package au.id.tmm.fetch.osm.model.response

import java.net.{URI, URISyntaxException}

import au.id.tmm.fetch.common.IanaLanguageSubtag
import Place.NameDetails
import au.id.tmm.fetch.osm.model.common.{BoundingBox, LatLong, OsmId, OsmType}
import io.circe.{Decoder, KeyDecoder}
import cats.syntax.traverse._

import scala.collection.immutable.ArraySeq

final case class Place(
  id: Place.Id,
  licence: String,
  osmType: OsmType,
  osmId: OsmId,
  boundingBox: BoundingBox,
  latLong: LatLong,
  displayName: Place.DisplayName,
  rank: Option[Place.Rank],
  mainTag: Tag,
  importance: Place.Importance,
  icon: Unit, // TODO
  address: Option[Place.Address],
  extraTags: ArraySeq[Tag],
  nameDetails: Option[NameDetails],
)

object Place {

  final case class Id(asLong: Long) extends AnyVal

  object Id {
    implicit val decoder: Decoder[Id] = Decoder[Long].map(Id.apply)
  }

  final case class DisplayName(asString: String) extends AnyVal

  object DisplayName {
    implicit val decoder: Decoder[DisplayName] = Decoder[String].map(DisplayName.apply)
  }

  final case class Rank(asInt: Int) extends AnyVal

  object Rank {
    implicit val decoder: Decoder[Rank] = Decoder[Int].map(Rank.apply)
  }

  final case class Importance(asDouble: Double) extends AnyVal

  object Importance {
    implicit val decoder: Decoder[Importance] = Decoder[Double].map(Importance.apply)
  }

  final case class Icon(asURI: URI) extends AnyVal

  object Icon {
    implicit val decoder: Decoder[Icon] = Decoder[String]
      .emap { s =>
        try Right(Icon(new URI(s)))
        catch {
          case e: URISyntaxException => Left(e.getMessage)
        }
      }
  }

  private implicit val boundingBoxDecoder: Decoder[BoundingBox] =
    Decoder[ArraySeq[Double]].emap {
      case arraySeq if arraySeq.length == 4 => Right(BoundingBox(arraySeq(0), arraySeq(1), arraySeq(2), arraySeq(3)))
      case bad                              => Left(s"Bad boundingBox $bad")
    }

  final case class NameDetails(
    name: Option[String],
    shortName: Option[String],
    pronounciation: Option[String],
    byIanaLanguageCode: Map[IanaLanguageSubtag, String],
    iataCode: Option[String], // https://en.wikipedia.org/wiki/IATA_airport_code
    icaoCode: Option[String], // Also for airports
  )

  object NameDetails {
    implicit val decoder: Decoder[NameDetails] = c =>
      for {
        name           <- c.get[Option[String]]("name")
        shortName      <- c.get[Option[String]]("short_name")
        iataCode       <- c.get[Option[String]]("iata")
        icaoCode       <- c.get[Option[String]]("icao")
        pronounciation <- c.get[Option[String]]("name:pronunciation")
        byIanaLanguageCode <- c.keys
          .getOrElse(List.empty)
          .filter(key => key.startsWith("name:") && key != "name:pronunciation")
          .to(ArraySeq)
          .traverse { key: String =>
            c.get[String](key).map(name => IanaLanguageSubtag(key.stripPrefix("name:")) -> name)
          }
      } yield NameDetails(name, shortName, pronounciation, byIanaLanguageCode.toMap, iataCode, icaoCode)
  }

  final case class Address(
    asMap: Map[Address.SubKey, String],
  )

  object Address {
    val empty = Address(Map.empty)

    sealed abstract class SubKey(val key: String)

    object SubKey {
      def apply(key: String): SubKey = key match {
        case SubKey.HouseNumber.key        => SubKey.HouseNumber
        case SubKey.HouseName.key          => SubKey.HouseName
        case SubKey.Flats.key              => SubKey.Flats
        case SubKey.ConscriptionNumber.key => SubKey.ConscriptionNumber
        case SubKey.Street.key             => SubKey.Street
        case SubKey.Place.key              => SubKey.Place
        case SubKey.Postcode.key           => SubKey.Postcode
        case SubKey.City.key               => SubKey.City
        case SubKey.Country.key            => SubKey.Country
        case SubKey.CountryCode.key        => SubKey.CountryCode
        case SubKey.Postbox.key            => SubKey.Postbox
        case SubKey.Full.key               => SubKey.Full
        case SubKey.Hamlet.key             => SubKey.Hamlet
        case SubKey.Suburb.key             => SubKey.Suburb
        case SubKey.SubDistrict.key        => SubKey.SubDistrict
        case SubKey.District.key           => SubKey.District
        case SubKey.Province.key           => SubKey.Province
        case SubKey.State.key              => SubKey.State
        case SubKey.County.key             => SubKey.County
        case unrecognisedKey               => SubKey.Other(unrecognisedKey)
      }

      case object HouseNumber                                  extends SubKey("housenumber")
      case object HouseName                                    extends SubKey("housename")
      case object Flats                                        extends SubKey("flats")
      case object ConscriptionNumber                           extends SubKey("conscriptionnumber")
      case object Street                                       extends SubKey("street")
      case object Place                                        extends SubKey("place")
      case object Postcode                                     extends SubKey("postcode")
      case object City                                         extends SubKey("city")
      case object Country                                      extends SubKey("country")
      case object CountryCode                                  extends SubKey("country_code")
      case object Postbox                                      extends SubKey("postbox")
      case object Full                                         extends SubKey("full")
      case object Hamlet                                       extends SubKey("hamlet")
      case object Suburb                                       extends SubKey("suburb")
      case object SubDistrict                                  extends SubKey("subdistrict")
      case object District                                     extends SubKey("district")
      case object Province                                     extends SubKey("province")
      case object State                                        extends SubKey("state")
      case object County                                       extends SubKey("county")
      final case class Other private (unrecognisedKey: String) extends SubKey(unrecognisedKey)

      implicit val decoder: KeyDecoder[SubKey] = KeyDecoder[String].map(apply)
    }

    implicit val decoder: Decoder[Address] = Decoder.decodeMap[SubKey, String].map(Address.apply)
  }

  implicit val decoder: Decoder[Place] = c =>
    for {
      id          <- c.get[Id]("place_id")
      licence     <- c.get[String]("licence")
      osmType     <- c.get[OsmType]("osm_type")
      osmId       <- c.get[OsmId]("osm_id")
      boundingBox <- c.get[BoundingBox]("boundingbox")
      lat         <- c.get[Double]("lat")
      long        <- c.get[Double]("lon")
      latLong = LatLong(lat, long)
      displayName  <- c.get[DisplayName]("display_name")
      placeRank    <- c.get[Option[Rank]]("place_rank")
      mainTagKey   <- c.get[String]("category") orElse c.get[String]("class")
      mainTagValue <- c.get[String]("type")
      mainTag = Tag(Tag.Key(mainTagKey), Tag.Value(mainTagValue))
      importance <- c.get[Importance]("importance")
      address    <- c.get[Option[Address]]("address")
      extraTags <- c
        .get[Option[Map[String, String]]]("extratags")
        .map { asMap =>
          asMap
            .getOrElse(Map.empty)
            .map { case (tagKey, tagValue) =>
              Tag(Tag.Key(tagKey), Tag.Value(tagValue))
            }
            .to(ArraySeq)
        }
      nameDetails <- c.get[Option[NameDetails]]("namedetails")
    } yield Place(
      id,
      licence,
      osmType,
      osmId,
      boundingBox,
      latLong,
      displayName,
      placeRank,
      mainTag,
      importance,
      (),
      address,
      extraTags,
      nameDetails,
    )

}
