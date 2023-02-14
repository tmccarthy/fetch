package au.id.tmm.fetch.aws.textract.results

import au.id.tmm.fetch.aws.textract.model.ReadableText

final class StringSimilarityOrdering private (base: String, compare: (String, String) => Int)
    extends Ordering[ReadableText] {
  private val lowerCaseBase: String = base.toLowerCase

  override def compare(x: ReadableText, y: ReadableText): Int =
    Ordering[Int].compare(
      compare(lowerCaseBase, x.readableText.toLowerCase),
      compare(lowerCaseBase, y.readableText.toLowerCase),
    )
}

object StringSimilarityOrdering {

  def bySimilarityTo(
    searchString: String,
    compare: (String, String) => Int,
  ): StringSimilarityOrdering = new StringSimilarityOrdering(searchString, compare)

}
