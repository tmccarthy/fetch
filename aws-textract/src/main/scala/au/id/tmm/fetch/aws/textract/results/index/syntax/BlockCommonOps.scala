package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.Block
import au.id.tmm.fetch.aws.textract.results.index.{AnalysisResultIndex, Searches}
import au.id.tmm.utilities.errors.ExceptionOr

import scala.reflect.ClassTag

private[syntax] abstract class BlockCommonOps[B <: Block](block: B)(implicit index: AnalysisResultIndex) {
  def recursivelySearch[B2 <: Block](collect: PartialFunction[Block, B2]): ExceptionOr[LazyList[B2]] =
    Searches.recursivelySearch[B2](block)(collect)

  def recursivelySearchWithPredicate[B2 <: Block : ClassTag](predicate: B2 => Boolean): ExceptionOr[LazyList[B2]] =
    Searches.recursivelySearch[B2](block) {
      case b2: B2 if predicate(b2) => b2
    }
}
