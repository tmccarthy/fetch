package au.id.tmm.fetch.aws.textract.results.index

import au.id.tmm.fetch.aws.textract.model.{AtomicBlock, Block, Line, Table}

import scala.collection.immutable.ArraySeq

sealed trait AtomicBlockParent {
  def asUntypedBlock: Block =
    this match {
      case AtomicBlockParent.OfLine(line) => line
      case AtomicBlockParent.OfCell(cell) => cell
    }

  def children: ArraySeq[AtomicBlock] =
    this match {
      case AtomicBlockParent.OfLine(line) => line.children
      case AtomicBlockParent.OfCell(cell) => cell.children
    }
}

object AtomicBlockParent {
  final case class OfLine(line: Line)       extends AtomicBlockParent
  final case class OfCell(cell: Table.Cell) extends AtomicBlockParent
}
