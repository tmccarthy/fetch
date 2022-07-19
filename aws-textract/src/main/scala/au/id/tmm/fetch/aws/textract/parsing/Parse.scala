package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model._
import au.id.tmm.utilities.errors.ExceptionOr
import au.id.tmm.utilities.errors.syntax._
import cats.syntax.functor.toFunctorOps
import cats.syntax.traverse.toTraverseOps
import cats.syntax.traverseFilter.toTraverseFilterOps
import software.amazon.awssdk.services.textract.{model => sdk}

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

// TODO rename
object Parse {

  import Common._

  def parsePages(apiResponses: ArraySeq[sdk.GetDocumentAnalysisResponse]): ExceptionOr[ArraySeq[Page]] =
    for {
      allBlocks <-
        apiResponses
          .flatTraverse[ExceptionOr, sdk.Block] { r =>
            requireNonNull(r.blocks).map(_.asScala.to(ArraySeq))
          }

      pages <- Relationships.enrichAnyBlockNotFoundFailures(allBlocks, extractPages(allBlocks))
    } yield pages

  private def extractPages(allBlocks: ArraySeq[sdk.Block]): ExceptionOr[ArraySeq[Page]] =
    for {
      atomBlockById <- for {
        wordsById <- makeLookup[Word](
          allBlocks,
          sdk.BlockType.WORD,
          Words.parseWord,
        )
        selectionElementsById <- makeLookup[SelectionElement](
          allBlocks,
          sdk.BlockType.SELECTION_ELEMENT,
          SelectionElements.parseSelectionElement,
        )
      } yield wordsById.widen[AtomicBlock] ++ selectionElementsById.widen[AtomicBlock]

      linesById <- makeLookup[Line](allBlocks, sdk.BlockType.LINE, Lines.parseLine(atomBlockById, _))

      cellById   <- makeLookup[Table.Cell](allBlocks, sdk.BlockType.CELL, Tables.parseCell(atomBlockById, _))
      tablesById <- makeLookup[Table](allBlocks, sdk.BlockType.TABLE, Tables.parseTable(cellById, _))

      keyValueSetsLookup <- KeyValueSets.extractKeyValueSets(atomBlockById, allBlocks)

      pages <-
        extract[Page](allBlocks, sdk.BlockType.PAGE, Pages.parsePage(linesById, tablesById, keyValueSetsLookup, _))
    } yield pages

  private def extract[B <: HasBlockId](
    blocks: ArraySeq[sdk.Block],
    blockType: sdk.BlockType,
    make: sdk.Block => ExceptionOr[B],
  ): ExceptionOr[ArraySeq[B]] =
    blocks
      .traverseFilter {
        case sdkBlock if sdkBlock.blockType() == blockType =>
          make(sdkBlock)
            .map(Some(_))
            .wrapExceptionWithMessage(s"Failure while parsing a $blockType")
        case _ => Right(None)
      }

  private def makeLookup[B <: HasBlockId](
    blocks: ArraySeq[sdk.Block],
    blockType: sdk.BlockType,
    make: sdk.Block => ExceptionOr[B],
  ): ExceptionOr[Map[BlockId, B]] =
    extract[B](blocks, blockType, make)
      .map(lookupById[B])

  private def lookupById[B <: HasBlockId](bs: ArraySeq[B]): Map[BlockId, B] =
    bs.map(b => b.id -> b).toMap

}
