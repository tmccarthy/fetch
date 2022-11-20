package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model.{AtomicBlock, BlockId, PageNumber, Table}
import au.id.tmm.utilities.errors.ExceptionOr
import software.amazon.awssdk.services.textract.{model => sdk}

private[parsing] object Tables {

  import Common._
  import Relationships._

  def parseCell(
    atomBlockLookup: Map[BlockId, AtomicBlock],
    block: sdk.Block,
  ): ExceptionOr[Table.Cell] =
    for {
      _           <- requireBlockType(block, sdk.BlockType.CELL)
      id          <- BlockId.fromString(block.id)
      pageNumber  <- PageNumber(block.page)
      geometry    <- parseGeometry(block.geometry)
      columnIndex <- requireNonNull(block.columnIndex)
      _           <- requireValue(block.columnSpan, 1)
      rowIndex    <- requireNonNull(block.rowIndex)
      _           <- requireValue(block.rowSpan, 1)
      children    <- lookupOrFail(atomBlockLookup, block, sdk.RelationshipType.CHILD)
    } yield Table.Cell(
      id,
      pageNumber,
      geometry,
      Table.Cell.Index(
        rowIndex,
        columnIndex,
      ),
      children,
    )

  def parseMergedCell(
    cellLookup: Map[BlockId, Table.Cell],
    block: sdk.Block,
  ): ExceptionOr[Table.MergedCell] =
    for {
      _           <- requireBlockType(block, sdk.BlockType.CELL)
      id          <- BlockId.fromString(block.id)
      pageNumber  <- PageNumber(block.page)
      geometry    <- parseGeometry(block.geometry)
      columnIndex <- requireNonNull(block.columnIndex)
      columnSpan  <- requireNonNull(block.columnSpan)
      rowIndex    <- requireNonNull(block.rowIndex)
      rowSpan     <- requireNonNull(block.rowSpan)
      children    <- lookupOrFail(cellLookup, block, sdk.RelationshipType.CHILD)
    } yield Table.MergedCell(
      id,
      pageNumber,
      geometry,
      Table.Cell.Index(rowIndex, columnIndex),
      Table.MergedCell.Span(rowSpan, columnSpan),
      children,
    )

  def parseTable(
    cellLookup: Map[BlockId, Table.Cell],
    mergedCellLookup: Map[BlockId, Table.MergedCell],
    block: sdk.Block,
  ): ExceptionOr[Table] =
    for {
      _           <- requireBlockType(block, sdk.BlockType.TABLE)
      id          <- BlockId.fromString(block.id)
      pageNumber  <- PageNumber(block.page)
      geometry    <- parseGeometry(block.geometry)
      cells       <- lookupOrFail(cellLookup, block, sdk.RelationshipType.CHILD)
      mergedCells <- lookupOrFail(mergedCellLookup, block, sdk.RelationshipType.MERGED_CELL)
    } yield Table(
      id,
      pageNumber,
      geometry,
      cells,
      mergedCells,
    )

}
