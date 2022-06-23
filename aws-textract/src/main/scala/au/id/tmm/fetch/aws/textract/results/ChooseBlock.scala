package au.id.tmm.fetch.aws.textract.results

import au.id.tmm.fetch.aws.textract.model.ReadableText
import au.id.tmm.collections.syntax.toIterableOps
import au.id.tmm.utilities.errors.ExceptionOr
import me.xdrop.fuzzywuzzy.FuzzySearch

import scala.collection.immutable.ArraySeq

trait ChooseBlock[B] {
  def chooseFrom(blocks: ArraySeq[B]): ExceptionOr[B]
}

object ChooseBlock {
  def only[B]: ChooseBlock[B] = blocks => blocks.onlyElementOrException
  def onlyMatching[B, B2 >: B](predicate: B2 => Boolean): ChooseBlock[B] =
    blocks => blocks.filter(predicate).onlyElementOrException
  def firstBy[B, B2 >: B](ordering: Ordering[B2]): ChooseBlock[B] = blocks => blocks.sorted(ordering).headOrException

  def mostSimilarToText[B <: ReadableText](
    searchText: String,
    comparisonAlgorithm: (String, String) => Int = FuzzySearch.partialRatio,
    threshold: Int                               = 88,
  ): ChooseBlock[B] = {
    val lowerCaseSearchText = searchText.toLowerCase

    blocks => {
      val scorePerBlock: ArraySeq[(B, Int)] = blocks.flatMap { b =>
        val score = comparisonAlgorithm(lowerCaseSearchText, b.readableText.toLowerCase)

        Option.when(score >= threshold)(b -> score)
      }

      scorePerBlock
        .sortBy { case (b, score) => score }
        .headOrException
        .map { case (b, score) => b }
    }
  }
}
