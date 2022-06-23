package au.id.tmm.fetch.osm

import au.id.tmm.fetch.osm.Geocoder.{ManualOverrideAction, ManualOverrides, Query}
import au.id.tmm.fetch.osm.model.common.OsmObject
import au.id.tmm.fetch.osm.model.request.{LookupRequest, OsmRequestConfig, SearchRequest}
import au.id.tmm.fetch.osm.model.request.SearchRequest.ViewBox
import au.id.tmm.fetch.osm.model.response.Place
import au.id.tmm.utilities.errors.GenericException
import cats.effect.IO

import scala.annotation.tailrec

final class Geocoder private (
  osmClient: NominatimOsm,
  manualOverrides: ManualOverrides,
) {

  @tailrec
  def geocode(
    query: Query,
    osmRequestConfig: OsmRequestConfig,
    viewBox: Option[ViewBox],
  ): IO[Option[Place]] = {
    val overrideAction = manualOverrides.actionFor(query)

    overrideAction match {
      case Some(ManualOverrideAction.GeocodeAs(osmObject)) =>
        geocodeUsingLookup(osmObject, osmRequestConfig).map(Some.apply)

      case Some(ManualOverrideAction.GeocodeAsNull) =>
        IO.pure(None)

      case Some(ManualOverrideAction.AttemptGeocodeAs(query)) =>
        geocode(query, osmRequestConfig, viewBox)

      case None =>
        geocodeUsingSearch(query, osmRequestConfig, viewBox)
    }
  }

  private def geocodeUsingSearch(
    query: Query,
    osmRequestConfig: OsmRequestConfig,
    viewBox: Option[ViewBox],
  ): IO[Option[Place]] = osmClient
    .search(
      SearchRequest(
        query.asString,
        viewBox = viewBox,
        osmRequestConfig = osmRequestConfig,
      ),
    )
    .map(_.headOption)

  private def geocodeUsingLookup(
    osmObject: OsmObject,
    osmRequestConfig: OsmRequestConfig,
  ): IO[Place] =
    for {
      places <- osmClient.lookup(LookupRequest(osmObject, osmRequestConfig))
      place  <- IO.fromOption(places.headOption)(GenericException("Geocode using manual lookup failed"))
    } yield place

}

object Geocoder {

  def apply(
    osmClient: NominatimOsm,
    manualOverrides: ManualOverrides,
  ): Geocoder = new Geocoder(osmClient, manualOverrides)

  final case class Query(asString: String) extends AnyVal

  trait ManualOverrides {
    def actionFor(query: Query): Option[ManualOverrideAction]
  }

  sealed trait ManualOverrideAction

  object ManualOverrideAction {

    /**
      * Fetch this osm object directly and use this to geocode
      */
    final case class GeocodeAs(osmObject: OsmObject) extends ManualOverrideAction

    /**
      * Geocode the place as NULL in the database: marking as "uncodable" and preventing future attempts to code the
      * place
      */
    final case object GeocodeAsNull extends ManualOverrideAction

    /**
      * Adjust the place name and try again
      */
    final case class AttemptGeocodeAs(query: Query) extends ManualOverrideAction

  }

}
