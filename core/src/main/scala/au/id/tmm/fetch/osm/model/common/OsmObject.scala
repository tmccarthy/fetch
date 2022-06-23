package au.id.tmm.fetch.osm.model.common

import java.net.URI

// TODO should probably replace every instance of type/id with this
final case class OsmObject(osmType: OsmType, osmId: OsmId) {
  def detailsPage: URI = new URI(
    s"https://nominatim.openstreetmap.org/ui/details.html?osmtype=${osmType.singleLetterCode}&osmid=${osmId.asLong}",
  )
}
