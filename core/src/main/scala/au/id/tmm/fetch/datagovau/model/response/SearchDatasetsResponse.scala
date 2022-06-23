package au.id.tmm.fetch.datagovau.model.response

import java.time.ZonedDateTime

import io.circe.Decoder

import scala.collection.immutable.ArraySeq

final case class SearchDatasetsResponse(
  hitCount: SearchDatasetsResponse.HitCount,
  datasets: ArraySeq[Dataset],
  temporal: SearchDatasetsResponse.Temporal,
)

object SearchDatasetsResponse {
  final case class HitCount(asInt: Int) extends AnyVal

  object HitCount {
    implicit val decoder: Decoder[HitCount] = Decoder[Int].map(HitCount.apply)
  }

  final case class Temporal(start: ZonedDateTime, end: ZonedDateTime)

  object Temporal {
    implicit val decoder: Decoder[Temporal] = c =>
      for {
        start <- c.downField("start").get[ZonedDateTime]("date")
        end   <- c.downField("end").get[ZonedDateTime]("date")
      } yield Temporal(start, end)
  }

  implicit val decoder: Decoder[SearchDatasetsResponse] =
    Decoder.forProduct3("hitCount", "dataSets", "temporal")(SearchDatasetsResponse.apply)
}
