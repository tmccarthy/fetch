package au.id.tmm.fetch.osm.model.request

final case class OsmRequestConfig(
  outputIncludes: Set[OsmRequestConfig.OutputIncludes] = Set.empty,
  acceptLanguage: Option[OsmRequestConfig.AcceptLanguage] = None,
)

object OsmRequestConfig {

  sealed trait OutputIncludes

  object OutputIncludes {
    case object AddressDetails extends OutputIncludes
    case object ExtraTags      extends OutputIncludes
    case object NameDetails    extends OutputIncludes
  }

  final case class AcceptLanguage(asString: String) extends AnyVal

}
