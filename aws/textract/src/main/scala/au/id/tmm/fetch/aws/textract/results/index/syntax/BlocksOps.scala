package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.Block
import au.id.tmm.fetch.aws.textract.results.index.{AnalysisResultIndex, Searches}
import au.id.tmm.utilities.errors.ExceptionOr

final class BlocksOps private (blocks: Seq[Block])(implicit index: AnalysisResultIndex) {
  def recursivelySearch[B2 <: Block](collect: PartialFunction[Block, B2]): ExceptionOr[LazyList[B2]] =
    Searches.recursivelySearch(blocks)(collect)
}

object BlocksOps {
  trait ToBlocksOps {
    implicit def toBlocksOps(blocks: Seq[Block])(implicit index: AnalysisResultIndex): BlocksOps =
      new BlocksOps(blocks)
  }
}
