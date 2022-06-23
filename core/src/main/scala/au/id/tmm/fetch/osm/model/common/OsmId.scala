package au.id.tmm.fetch.osm.model.common

import io.circe.Decoder

final case class OsmId(asLong: Long) extends AnyVal

object OsmId {
  implicit val decoder: Decoder[OsmId] = Decoder[Long].map(OsmId.apply)
}
