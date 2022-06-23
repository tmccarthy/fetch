package au.id.tmm.fetch.osm

import au.id.tmm.fetch.osm.model.common.{OsmObject, OsmType}
import au.id.tmm.fetch.osm.model.request.SearchRequest.ViewBox
import au.id.tmm.fetch.osm.model.request.{LookupRequest, OsmRequestConfig, SearchRequest}
import au.id.tmm.fetch.osm.model.response.Place
import cats.effect.IO
import sttp.client3._
import sttp.client3.circe.asJson
import sttp.model.Uri
import sttp.model.Uri.QuerySegment

import scala.collection.immutable.ArraySeq

final class NominatimOsm private (
  sttpBackend: SttpBackend[IO, Any],
  config: NominatimOsm.Config,
) {

  private val searchUri: Uri = config.baseUri.addPath("search")
  private val lookupUri: Uri = config.baseUri.addPath("lookup")

  private val baseNomatimOsmRequest = basicRequest
    .header(sttp.model.HeaderNames.UserAgent, config.userAgent)

  // TODO should I build rate limiting into this client?

  def search(request: SearchRequest): IO[ArraySeq[Place]] = {
    val querySegments: List[QuerySegment] = querySegmentsFor(request.osmRequestConfig) ++ (
      List(
        "q" -> request.query,
      ) ++ request.limitToCountries.map { countries =>
        "countrycodes" -> countries.toList.mkString(",")
      } ++ request.limit.map { limit =>
        "limit" -> limit.toString
      } ++ request.viewBox.toList.flatMap { viewBox =>
        val viewBoxParam = "viewbox" -> List(
          viewBox.boundingBox.minLong,
          viewBox.boundingBox.minLat,
          viewBox.boundingBox.maxLong,
          viewBox.boundingBox.maxLat,
        ).mkString(",")

        viewBox match {
          case ViewBox.Soft(_) => List(viewBoxParam)
          case ViewBox.Hard(_) => List(viewBoxParam, "bounded" -> "1")
        }
      } ++ request.dedupe.map { dedupe =>
        "dedupe" -> (if (dedupe) "1" else "0")
      }
    ).map { case (key, value) => QuerySegment.KeyValue(key, value) }

    val sttpRequest = baseNomatimOsmRequest
      .get(searchUri.addQuerySegments(querySegments))
      .response(asJson[ArraySeq[Place]])

    for {
      response <- sttpRequest.send(sttpBackend)
      places   <- IO.fromEither(response.body)
    } yield places
  }

  def lookup(request: LookupRequest): IO[ArraySeq[Place]] = {
    val osmIdsParam = request.osmObjects
      .map {
        case OsmObject(osmType, id) => {
          val typeCode = osmType match {
            case OsmType.Relation => "R"
            case OsmType.Node     => "N"
            case OsmType.Way      => "W"
          }

          s"$typeCode${id.asLong}"
        }
      }
      .mkString(",")

    val querySegments = querySegmentsFor(request.osmRequestConfig) :+
      QuerySegment.KeyValue("osm_ids", osmIdsParam)

    val sttpRequest = baseNomatimOsmRequest
      .get(lookupUri.addQuerySegments(querySegments))
      .response(asJson[ArraySeq[Place]])

    for {
      response <- sttpRequest.send(sttpBackend)
      places   <- IO.fromEither(response.body)
    } yield places
  }

  private def querySegmentsFor(osmRequestConfig: OsmRequestConfig): List[QuerySegment] = {
    val builder = List.newBuilder[(String, String)]

    builder.addOne("format" -> "json")
    builder.addOne("email"  -> this.config.email)
    builder.addOne(
      "accept-language" -> osmRequestConfig.acceptLanguage.map(_.asString).getOrElse(this.config.acceptLanguage),
    )

    osmRequestConfig.outputIncludes.foreach {
      case OsmRequestConfig.OutputIncludes.AddressDetails => builder.addOne("addressdetails" -> "1")
      case OsmRequestConfig.OutputIncludes.ExtraTags      => builder.addOne("extratags" -> "1")
      case OsmRequestConfig.OutputIncludes.NameDetails    => builder.addOne("namedetails" -> "1")
    }

    builder.result().map { case (key, value) => QuerySegment.KeyValue(key, value) }
  }

}

object NominatimOsm {
  final case class Config(
    email: String,
    acceptLanguage: String,
    userAgent: String,
    baseUri: Uri = Uri("https", "nominatim.openstreetmap.org"),
  )

  def apply(
    sttpBackend: SttpBackend[IO, Any],
    config: NominatimOsm.Config,
  ): NominatimOsm = new NominatimOsm(sttpBackend, config)

}
