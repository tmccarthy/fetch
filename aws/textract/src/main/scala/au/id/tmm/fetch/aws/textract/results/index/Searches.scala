package au.id.tmm.fetch.aws.textract.results.index

import au.id.tmm.fetch.aws.textract.model.{AnalysisResult, Block}
import au.id.tmm.utilities.errors.ExceptionOr
import au.id.tmm.utilities.errors.syntax._

import scala.annotation.tailrec
import scala.collection.immutable.{ArraySeq, ListSet}
import cats.syntax.traverse.toTraverseOps

object Searches {

  private def isAncestor(
    maybeAncestor: Block,
    maybeChild: Block,
  )(implicit
    index: AnalysisResultIndex,
  ): ExceptionOr[Boolean] =
    for {
      maybeParent <- index.untypedParentOf(maybeChild)
      isAncestorResult <- maybeParent match {
        case Some(parent) => if (maybeParent.contains(parent)) Right(true) else isAncestor(maybeAncestor, parent)
        case None         => Right(false)
      }
    } yield isAncestorResult

  private def removeBlocksWithSharedAncestors(
    blocks: Seq[Block],
  )(implicit
    index: AnalysisResultIndex,
  ): ExceptionOr[Seq[Block]] = {
    @tailrec
    def unsafeGo(remainingBlocksToProcess: Seq[Block], blocksWithoutSharedAncestors: ListSet[Block]): ListSet[Block] =
      remainingBlocksToProcess match {
        case Seq() => blocksWithoutSharedAncestors
        case candidate +: remainingBlocks =>
          if (blocks.exists(b => isAncestor(b, candidate).getOrThrow)) {
            unsafeGo(remainingBlocks, blocksWithoutSharedAncestors)
          } else {
            val blocksToRemove = blocks.filter(b => isAncestor(candidate, b).getOrThrow).toSet

            unsafeGo(remainingBlocks, blocksWithoutSharedAncestors.diff(blocksToRemove).incl(candidate))
          }
      }

    ExceptionOr.catchIn {
      unsafeGo(remainingBlocksToProcess = blocks, blocksWithoutSharedAncestors = ListSet.empty)
        .to(ArraySeq)
    }
  }

  def recursivelySearch[B <: Block](
    blocks: Seq[Block],
  )(
    collect: PartialFunction[Block, B],
  )(implicit
    index: AnalysisResultIndex,
  ): ExceptionOr[LazyList[B]] =
    for {
      blocksWithoutSharedAncestors <- removeBlocksWithSharedAncestors(blocks)
      results                      <- blocksWithoutSharedAncestors.to(LazyList).flatTraverse(b => recursivelySearch(b)(collect))
    } yield results

  def recursivelySearch[B <: Block](
    block: Block,
  )(
    collect: PartialFunction[Block, B],
  )(implicit
    index: AnalysisResultIndex,
  ): ExceptionOr[LazyList[B]] =
    for {
      _ <- index.failIfNotPartOfDocument(block)
      resultIterator =
        BlockIterator
          .recursivelyIterateBlockAndChildren(block, includeKeyValueSets = true)
          .collect(collect)
    } yield LazyList.from(resultIterator)

  def recursivelySearch[B <: Block](
    analysisResult: AnalysisResult,
  )(
    collect: PartialFunction[Block, B],
  )(implicit
    index: AnalysisResultIndex,
  ): ExceptionOr[LazyList[B]] =
    for {
      _       <- index.failIfNotAnalysisResult(analysisResult)
      results <- recursivelySearch(analysisResult.pages)(collect)
    } yield results

  def recursivelySearchWholeDocument[B <: Block](
    collect: PartialFunction[Block, B],
  )(implicit
    index: AnalysisResultIndex,
  ): ExceptionOr[LazyList[B]] =
    for {
      results <- recursivelySearch(index.analysisResult.pages)(collect)
    } yield results

}
