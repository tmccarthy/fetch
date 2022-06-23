package au.id.tmm.fetch.aws.textract.results

import au.id.tmm.fetch.aws.textract.model._
import au.id.tmm.fetch.aws.textract.results.GeometricOrdering.PageSide
import au.id.tmm.fetch.aws.textract.results.GeometricOrdering.PageSide.Top

import scala.collection.immutable.ArraySeq

object BlockPredicates {

  def lineHasWordsLike(searchText: String)(line: Line): Boolean = hasWordsLike(searchText, line.children)
  def cellHasWordsLike(searchText: String)(cell: Table.Cell): Boolean = hasWordsLike(searchText, cell.children)
  def keyHasWordsLike(searchText: String)(key: KeyValueSet.Key): Boolean = hasWordsLike(searchText, key.children)
  def valueHasWordsLike(searchText: String)(value: KeyValueSet.Value): Boolean =
    hasWordsLike(searchText, value.children)

  def hasWordsLike(searchText: String, atomicBlocks: ArraySeq[AtomicBlock]): Boolean = {
    val blockWords: ArraySeq[String] = atomicBlocks.flatMap {
      case _: SelectionElement => ArraySeq.empty
      case w: Word             => reduceToSimpleTextArray(w.text)
    }

    val searchWords: ArraySeq[String] = reduceToSimpleTextArray(searchText)

    blockWords.containsSlice(searchWords)
  }

  private def reduceToSimpleTextArray(string: String): ArraySeq[String] =
    ArraySeq.unsafeWrapArray(string.toLowerCase.replaceAll("""[^\w\s]""", "").split("""\s+"""))

  def beneath(referenceBlock: Block)(block: Block): Boolean =
    GeometricOrdering.byDistanceFrom(Top).lteq(referenceBlock, block)

  def above(referenceBlock: Block)(block: Block): Boolean =
    GeometricOrdering.byDistanceFrom(PageSide.Top).gteq(referenceBlock, block)

  def between(referenceBlock1: Block, referenceBlock2: Block)(block: Block): Boolean =
    if (GeometricOrdering.byDistanceFrom(PageSide.Top).lteq(referenceBlock1, referenceBlock2)) {
      beneath(referenceBlock1)(block) && above(referenceBlock2)(block)
    } else if (GeometricOrdering.byDistanceFrom(PageSide.Top).gteq(referenceBlock1, referenceBlock2)) {
      beneath(referenceBlock2)(block) && above(referenceBlock1)(block)
    } else {
      false
    }

  def strictlyBeneath(referenceBlock: Block)(block: Block): Boolean =
    DocumentDistanceOrdering.lteq(referenceBlock, block)

  def strictlyAbove(referenceBlock: Block)(block: Block): Boolean = DocumentDistanceOrdering.gteq(referenceBlock, block)

  def strictlyBetween(referenceBlock1: Block, referenceBlock2: Block)(block: Block): Boolean =
    if (DocumentDistanceOrdering.lteq(referenceBlock1, referenceBlock2)) {
      strictlyBeneath(referenceBlock1)(block) && strictlyAbove(referenceBlock2)(block)
    } else if (DocumentDistanceOrdering.gteq(referenceBlock1, referenceBlock2)) {
      strictlyBeneath(referenceBlock2)(block) && strictlyAbove(referenceBlock1)(block)
    } else {
      false
    }

  def strictlyWithin(enclosingBlock: Block)(block: Block): Boolean =
    enclosingBlock.pageNumber == block.pageNumber &&
      enclosingBlock.geometry.boundingBox.top <= block.geometry.boundingBox.top &&
      enclosingBlock.geometry.boundingBox.left <= block.geometry.boundingBox.left &&
      enclosingBlock.geometry.boundingBox.right >= block.geometry.boundingBox.right &&
      enclosingBlock.geometry.boundingBox.bottom >= block.geometry.boundingBox.bottom

}
