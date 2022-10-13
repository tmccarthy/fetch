package au.id.tmm.fetch.aws.textract.results.index

import au.id.tmm.fetch.aws.textract.model.Page.Child
import au.id.tmm.fetch.aws.textract.model._
import au.id.tmm.fetch.aws.textract.results.index.AnalysisResultIndex.NotFoundInResults
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException, ProductException}

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.util.chaining.scalaUtilChainingOps

// TODO this modeling is awful. Do away with passing around an implicit and just wrap the analysisResult in something like
//      "IndexedAnalysisResult" or something
final class AnalysisResultIndex private (
  private[index] val analysisResult: AnalysisResult,
  atomicBlockParents: collection.Map[AtomicBlock, AtomicBlockParent],
  cellParents: collection.Map[Table.Cell, Table],
  tableParents: collection.Map[Table, Page],
  lineParents: collection.Map[Line, Page],
  kvSetParents: collection.Map[KeyValueSet, Page],
  tableCellLookup: collection.Map[Table, Map[(Int, Int), Table.Cell]],
  kvSetsForKeys: collection.Map[KeyValueSet.Key, KeyValueSet],
  kvSetsForValues: collection.Map[KeyValueSet.Value, KeyValueSet],
) {

  private val pages: Set[Page] = analysisResult.pages.toSet

  def failIfNotAnalysisResult(analysisResult: AnalysisResult): ExceptionOr[Unit] =
    Either.cond(
      analysisResult == this.analysisResult,
      (),
      GenericException(
        s"Different analysis result ${analysisResult.jobId.asString}. Expected ${this.analysisResult.jobId.asString}",
      ),
    )

  def failIfNotPartOfDocument(block: Block): ExceptionOr[Unit] = {
    val isPartOfDocument = block match {
      case block: AtomicBlock       => atomicBlockParents.contains(block)
      case line: Line               => lineParents.contains(line)
      case page: Page               => pages.contains(page)
      case table: Table             => tableParents.contains(table)
      case cell: Table.Cell         => cellParents.contains(cell)
      case key: KeyValueSet.Key     => kvSetsForKeys.contains(key)
      case value: KeyValueSet.Value => kvSetsForValues.contains(value)
    }

    Either.cond(isPartOfDocument, (), NotFoundInResults(block))
  }

  def parentOf(atomicBlock: AtomicBlock): ExceptionOr[AtomicBlockParent] =
    atomicBlockParents.get(atomicBlock).toRight(NotFoundInResults(atomicBlock))

  def parentOf(cell: Table.Cell): ExceptionOr[Table] =
    cellParents.get(cell).toRight(NotFoundInResults(cell))

  def parentOf(table: Table): ExceptionOr[Page] =
    tableParents.get(table).toRight(NotFoundInResults(table))

  def parentOf(line: Line): ExceptionOr[Page] =
    lineParents.get(line).toRight(NotFoundInResults(line))

  def parentOf(keyValueSet: KeyValueSet): ExceptionOr[Page] =
    kvSetParents.get(keyValueSet).toRight(NotFoundInResults(keyValueSet.key))

  def parentOf(key: KeyValueSet.Key): ExceptionOr[Page] =
    kvSetFor(key).flatMap(parentOf)

  def parentOf(value: KeyValueSet.Value): ExceptionOr[Page] =
    kvSetFor(value).flatMap(parentOf)

  def siblingsOf(atomicBlock: AtomicBlock): ExceptionOr[ArraySeq[AtomicBlock]] =
    parentOf(atomicBlock).map(atomicBlockParent => atomicBlockParent.children.filter(b => b != atomicBlock))

  def siblingsOf(cell: Table.Cell): ExceptionOr[ArraySeq[Table.Cell]] =
    parentOf(cell).map { table =>
      table.children.filter(c => c != cell)
    }

  def siblingsOf(table: Table): ExceptionOr[ArraySeq[SiblingsUnderPage]] =
    parentOf(table).map { page =>
      page.children.collect {
        case Page.Child.OfLine(line)                                   => SiblingsUnderPage.OfLine(line)
        case Page.Child.OfTable(siblingTable) if siblingTable != table => SiblingsUnderPage.OfTable(siblingTable)
      }
    }

  def siblingsOf(line: Line): ExceptionOr[ArraySeq[SiblingsUnderPage]] =
    parentOf(line).map { page =>
      page.children.collect {
        case Page.Child.OfLine(siblingLine) if siblingLine != line => SiblingsUnderPage.OfLine(siblingLine)
        case Page.Child.OfTable(table)                             => SiblingsUnderPage.OfTable(table)
      }
    }

  def siblingsOf(keyValueSet: KeyValueSet): ExceptionOr[ArraySeq[KeyValueSet]] =
    parentOf(keyValueSet).map(p =>
      p.children.collect { case Page.Child.OfKeyValueSet(kvSet) =>
        kvSet
      },
    )

  def siblingsOf(page: Page): ExceptionOr[ArraySeq[Page]] =
    for {
      _ <- failIfNotPartOfDocument(page)
    } yield analysisResult.pages.filter(p => p != page)

  def findCell(
    table: Table,
    columnIndex: Int,
    rowIndex: Int,
  ): ExceptionOr[Table.Cell] =
    for {
      cellLookup <-
        tableCellLookup
          .get(table)
          .toRight(NotFoundInResults(table))

      cell <-
        cellLookup
          .get((columnIndex, rowIndex))
          .toRight(GenericException(s"Cell $columnIndex, $rowIndex not found"))
    } yield cell

  def kvSetFor(key: KeyValueSet.Key): ExceptionOr[KeyValueSet] =
    kvSetsForKeys.get(key).toRight(NotFoundInResults(key))

  def valueFor(key: KeyValueSet.Key): ExceptionOr[KeyValueSet.Value] = kvSetFor(key).map(_.value)

  def kvSetFor(value: KeyValueSet.Value): ExceptionOr[KeyValueSet] =
    kvSetsForValues.get(value).toRight(NotFoundInResults(value))

  def keyFor(value: KeyValueSet.Value): ExceptionOr[KeyValueSet.Key] = kvSetFor(value).map(_.key)

  private[index] def untypedParentOf(block: Block): ExceptionOr[Option[Block]] =
    for {
      _ <- failIfNotPartOfDocument(block)
      parent <- block match {
        case block: AtomicBlock                                  => parentOf(block).map(p => Some(p.asUntypedBlock))
        case line: Line                                          => parentOf(line).map(Some.apply)
        case table: Table                                        => parentOf(table).map(Some.apply)
        case cell: Table.Cell                                    => parentOf(cell).map(Some.apply)
        case _: Page | _: KeyValueSet.Key | _: KeyValueSet.Value => Right(None)
      }
    } yield parent

}

object AnalysisResultIndex {

  final case class NotFoundInResults(block: Block) extends ProductException

  def apply(analysisResult: AnalysisResult): AnalysisResultIndex = {
    val atomicBlockParents: mutable.Map[AtomicBlock, AtomicBlockParent] = mutable.Map()
    val cellParents: mutable.Map[Table.Cell, Table]                     = mutable.Map()
    val tableParents: mutable.Map[Table, Page]                          = mutable.Map()
    val lineParents: mutable.Map[Line, Page]                            = mutable.Map()
    val kvSetParents: mutable.Map[KeyValueSet, Page]                    = mutable.Map()

    val tableCellLookup: mutable.Map[Table, Map[(Int, Int), Table.Cell]] = mutable.Map()
    val kvForKeyLookup: mutable.Map[KeyValueSet.Key, KeyValueSet]        = mutable.Map()
    val kvForValueLookup: mutable.Map[KeyValueSet.Value, KeyValueSet]    = mutable.Map()

    analysisResult.pages.foreach { page =>
      page.children.foreach {
        case Child.OfLine(line) => {
          val lineAsAtomicBlockParent = AtomicBlockParent.OfLine(line)
          lineParents.put(line, page)
          line.children.foreach { atomicBlock =>
            atomicBlockParents
              .put(atomicBlock, lineAsAtomicBlockParent)
              .tap {
                case Some(_) => throw new AssertionError()
                case None    => ()
              }
          }
        }
        case Child.OfTable(table) => {
          tableParents.put(table, page)
          val cellLookupBuilder = Map.newBuilder[(Int, Int), Table.Cell]
          table.children.foreach { cell =>
            val cellAsAtomicBlockParent = AtomicBlockParent.OfCell(cell)
            cellParents.put(cell, table)
            cell.children.foreach { atomicBlock =>
              atomicBlockParents
                .put(atomicBlock, cellAsAtomicBlockParent)
                .tap {
                  case Some(_) => throw new AssertionError()
                  case None    => ()
                }
            }
            cellLookupBuilder.addOne((cell.columnIndex, cell.rowIndex), cell)
          }
          tableCellLookup.addOne(table -> cellLookupBuilder.result())
        }
        case Child.OfKeyValueSet(keyValueSet) => {
          kvSetParents.put(keyValueSet, page)
          kvForKeyLookup.put(keyValueSet.key, keyValueSet)
          kvForValueLookup.put(keyValueSet.value, keyValueSet)
        }
      }
    }

    new AnalysisResultIndex(
      analysisResult,
      atomicBlockParents,
      cellParents,
      tableParents,
      lineParents,
      kvSetParents,
      tableCellLookup,
      kvForKeyLookup,
      kvForValueLookup,
    )
  }

}
