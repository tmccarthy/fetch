package au.id.tmm.fetch.aws.textract.results.index

import au.id.tmm.fetch.aws.textract.model.{Block, Line, Table}

sealed trait SiblingsUnderPage {
  def asUntypedBlock: Block =
    this match {
      case SiblingsUnderPage.OfLine(line)   => line
      case SiblingsUnderPage.OfTable(table) => table
    }
}

object SiblingsUnderPage {
  final case class OfLine(line: Line)    extends SiblingsUnderPage
  final case class OfTable(table: Table) extends SiblingsUnderPage
}
