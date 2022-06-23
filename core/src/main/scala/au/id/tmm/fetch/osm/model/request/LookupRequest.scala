package au.id.tmm.fetch.osm.model.request

import au.id.tmm.fetch.osm.model.common.OsmObject

import scala.collection.immutable.ArraySeq

final case class LookupRequest(
  osmObjects: ArraySeq[OsmObject],
  osmRequestConfig: OsmRequestConfig,
)

object LookupRequest {
  def apply(osmObject: OsmObject, osmRequestConfig: OsmRequestConfig = OsmRequestConfig()): LookupRequest =
    LookupRequest(ArraySeq(osmObject), osmRequestConfig)
}
