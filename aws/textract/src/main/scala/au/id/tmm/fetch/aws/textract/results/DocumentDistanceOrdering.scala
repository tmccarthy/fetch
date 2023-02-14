package au.id.tmm.fetch.aws.textract.results

import au.id.tmm.fetch.aws.textract.model.{Block, PageNumber}

/**
  * Compares two `Block`s by the distance through the document they appear. `Block`s that overlap cannot be compared
  */
object DocumentDistanceOrdering extends PartialOrdering[Block] {

  override def tryCompare(x: Block, y: Block): Option[Int] =
    if (x.pageNumber == y.pageNumber) {
      val (xTop, xBottom) = (x.geometry.boundingBox.top, x.geometry.boundingBox.bottom)
      val (yTop, yBottom) = (y.geometry.boundingBox.top, y.geometry.boundingBox.bottom)

      if (xBottom >= yTop) {
        Some(1)
      } else if (xTop <= yBottom) {
        Some(-1)
      } else {
        None
      }
    } else {
      Some(implicitly[Ordering[PageNumber]].compare(x.pageNumber, y.pageNumber))
    }

  override def lteq(x: Block, y: Block): Boolean = tryCompare(x, y).exists(_ <= 0)

}
