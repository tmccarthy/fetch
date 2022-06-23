package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.fetch.aws.textract.TextractJobId

import scala.collection.immutable.ArraySeq

final case class AnalysisResult(
  jobId: TextractJobId,
  pages: ArraySeq[Page],
)
