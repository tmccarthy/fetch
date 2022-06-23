package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{Page, Table}
import au.id.tmm.fetch.aws.textract.results.index.{AnalysisResultIndex, SiblingsUnderPage}
import au.id.tmm.utilities.errors.ExceptionOr

import scala.collection.immutable.ArraySeq

final class TableOps private (table: Table)(implicit index: AnalysisResultIndex) extends BlockCommonOps[Table](table) {
  def parent: ExceptionOr[Page] =
    index.parentOf(table)

  def siblings: ExceptionOr[ArraySeq[SiblingsUnderPage]] =
    index.siblingsOf(table)

  def findCell(columnIndex: Int, rowIndex: Int): ExceptionOr[Table.Cell] =
    index.findCell(table, columnIndex, rowIndex)
}

object TableOps {
  trait ToTableOps {
    implicit def toTableOps(table: Table)(implicit index: AnalysisResultIndex): TableOps =
      new TableOps(table)
  }
}
