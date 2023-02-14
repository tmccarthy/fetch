package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{Line, Page}
import au.id.tmm.fetch.aws.textract.results.index.{AnalysisResultIndex, SiblingsUnderPage}
import au.id.tmm.utilities.errors.ExceptionOr

import scala.collection.immutable.ArraySeq

final class LineOps private (line: Line)(implicit index: AnalysisResultIndex) extends BlockCommonOps[Line](line) {
  def parent: ExceptionOr[Page] =
    index.parentOf(line)

  def siblings: ExceptionOr[ArraySeq[SiblingsUnderPage]] =
    index.siblingsOf(line)
}

object LineOps {
  trait ToLineOps {
    implicit def toLineOps(line: Line)(implicit index: AnalysisResultIndex): LineOps =
      new LineOps(line)
  }
}
