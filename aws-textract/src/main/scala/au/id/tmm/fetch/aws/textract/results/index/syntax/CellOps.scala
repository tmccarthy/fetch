package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.Table
import au.id.tmm.fetch.aws.textract.results.index.AnalysisResultIndex
import au.id.tmm.utilities.errors.ExceptionOr

import scala.collection.immutable.ArraySeq

final class CellOps private (cell: Table.Cell)(implicit index: AnalysisResultIndex)
    extends BlockCommonOps[Table.Cell](cell) {
  def parent: ExceptionOr[Table] =
    index.parentOf(cell)

  def siblings: ExceptionOr[ArraySeq[Table.Cell]] =
    index.siblingsOf(cell)
}

object CellOps {
  trait ToCellOps {
    implicit def toCellOps(cell: Table.Cell)(implicit index: AnalysisResultIndex): CellOps =
      new CellOps(cell)
  }
}
