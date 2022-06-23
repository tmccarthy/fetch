package au.id.tmm.fetch.datagovau.model.request

import java.time.LocalDate

final case class SearchDatasetsRequest(
  query: Option[String] = None,
  start: Option[SearchDatasetsRequest.ResultIndex] = None,
  limit: Option[SearchDatasetsRequest.Limit] = None,
  facetSize: Option[SearchDatasetsRequest.FacetSize] = None,
  publisher: Option[String] = None, // TODO this apparently supports a list
  dateFrom: Option[LocalDate] = None,
  dateTo: Option[LocalDate] = None,
)

object SearchDatasetsRequest {
  final case class ResultIndex(asInt: Int) extends AnyVal
  final case class Limit(asInt: Int)       extends AnyVal
  final case class FacetSize(asInt: Int)   extends AnyVal
}
