package au.id.tmm.fetch.osm.model.request

import au.id.tmm.fetch.osm.model.common.BoundingBox
import cats.data.NonEmptyList

final case class SearchRequest(
  query: String,
  limitToCountries: Option[NonEmptyList[SearchRequest.CountryCode]] = None,
  limit: Option[Int] = None,
  viewBox: Option[SearchRequest.ViewBox] = None,
  dedupe: Option[Boolean] = None,
  osmRequestConfig: OsmRequestConfig = OsmRequestConfig(),
)

object SearchRequest {

  final case class CountryCode(asString: String) extends AnyVal

  sealed trait ViewBox {
    def boundingBox: BoundingBox
  }

  object ViewBox {
    final case class Soft(boundingBox: BoundingBox) extends ViewBox
    final case class Hard(boundingBox: BoundingBox) extends ViewBox
  }
}
