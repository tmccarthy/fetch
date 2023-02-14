package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{AnalysisResult, Block, Page, PageNumber}
import au.id.tmm.fetch.aws.textract.results.index.{AnalysisResultIndex, Searches}
import au.id.tmm.utilities.errors.ExceptionOr

final class AnalysisResultOps private (
  analysisResult: au.id.tmm.fetch.aws.textract.model.AnalysisResult,
)(implicit
  index: AnalysisResultIndex,
) {
  def recursivelySearch[B2 <: Block](collect: PartialFunction[Block, B2]): ExceptionOr[LazyList[B2]] =
    Searches.recursivelySearch(analysisResult)(collect)

  def getPage(pageNumber: PageNumber): ExceptionOr[Page] =
    ExceptionOr.catchIn(analysisResult.pages.apply(pageNumber.ordinal - 1))
}

object AnalysisResultOps {
  trait ToAnalysisResultOps {
    implicit def toAnalysisResultOps(
      analysisResult: AnalysisResult,
    )(implicit
      index: AnalysisResultIndex,
    ): AnalysisResultOps =
      new AnalysisResultOps(analysisResult)
  }
}
