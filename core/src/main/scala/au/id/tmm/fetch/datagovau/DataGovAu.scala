package au.id.tmm.fetch.datagovau

import au.id.tmm.fetch.datagovau.model.request.SearchDatasetsRequest
import au.id.tmm.fetch.datagovau.model.response.SearchDatasetsResponse
import cats.effect.IO
import sttp.client3.circe.asJson
import sttp.client3.{SttpBackend, basicRequest}
import sttp.model.Uri
import sttp.model.Uri.QuerySegment

class DataGovAu private (
  sttpBackend: SttpBackend[IO, Any],
  config: DataGovAu.Config,
) {

  private val searchDatasetsUri: Uri = config.baseUri.addPath("api", "v0", "search", "datasets")

  private val baseRequest = basicRequest.header(sttp.model.HeaderNames.UserAgent, config.userAgent)

  def searchDatasets(request: SearchDatasetsRequest): IO[SearchDatasetsResponse] = {
    val querySegments: List[QuerySegment] = {
      List(
      ) ++ request.query.map { query =>
        "query" -> query
      } ++ request.start.map { start =>
        "start" -> start.asInt.toString
      } ++ request.limit.map { limit =>
        "limit" -> limit.asInt.toString
      } ++ request.facetSize.map { facetSize =>
        "facetSize" -> facetSize.asInt.toString
      } ++ request.publisher.map { publisher =>
        "publisher" -> publisher
      } ++ request.dateFrom.map { dateFrom =>
        "dateFrom" -> dateFrom.toString
      } ++ request.dateTo.map { dateTo =>
        "dateTo" -> dateTo.toString
      }
    }.map { case (key, value) => QuerySegment.KeyValue(key, value) }

    val sttpRequest = baseRequest
      .get(searchDatasetsUri.addQuerySegments(querySegments))
      .response(asJson[SearchDatasetsResponse])

    for {
      responseOrError <- sttpRequest.send(sttpBackend)
      response        <- IO.fromEither(responseOrError.body)
    } yield response
  }

}

object DataGovAu {
  final case class Config(
    userAgent: String,
    baseUri: Uri = Uri("https", "data.gov.au"),
  )

  def apply(sttpBackend: SttpBackend[IO, Any], config: Config): DataGovAu = new DataGovAu(sttpBackend, config)
}
