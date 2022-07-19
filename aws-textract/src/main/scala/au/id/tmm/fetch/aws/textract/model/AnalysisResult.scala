package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.fetch.aws.textract.TextractJobId
import io.circe.{Codec, Decoder, Encoder}

import scala.collection.immutable.ArraySeq

final case class AnalysisResult(
  jobId: TextractJobId,
  pages: ArraySeq[Page],
)

object AnalysisResult {
  implicit val encoder: Codec[AnalysisResult] =
    Codec.from(
      Decoder.forProduct2("jobId", "pages")(AnalysisResult.apply),
      Encoder.forProduct2("jobId", "pages")(a => (a.jobId, a.pages)),
    )
}
