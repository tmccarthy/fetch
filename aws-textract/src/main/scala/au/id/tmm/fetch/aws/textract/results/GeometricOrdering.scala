package au.id.tmm.fetch.aws.textract.results

import au.id.tmm.fetch.aws.textract.model.Block
import au.id.tmm.fetch.aws.textract.model.Geometry.Polygon.Point

trait GeometricOrdering extends Ordering[Block] {

  protected def score(block: Block): Double

  override final def compare(x: Block, y: Block): Int = Ordering.Double.TotalOrdering.compare(score(x), score(y))

}

object GeometricOrdering {

  private def pointFor(block: Block): Point = block.geometry.boundingBox.centre

  private def distanceBetween(left: Point, right: Point): Float =
    math.sqrt(math.pow(right.x - left.x, 2) + math.pow(right.y - left.y, 2)).toFloat

  sealed trait PageSide

  object PageSide {
    case object Top  extends PageSide
    case object Left extends PageSide
  }

  def byDistanceFrom(pageSide: PageSide): GeometricOrdering =
    pageSide match {
      case PageSide.Top  => b => pointFor(b).y
      case PageSide.Left => b => pointFor(b).x
    }

  sealed abstract class PageCorner(val point: Point)

  object PageCorner {
    case object TopLeft     extends PageCorner(Point(0, 0).getOrElse(throw new AssertionError()))
    case object TopRight    extends PageCorner(Point(1, 0).getOrElse(throw new AssertionError()))
    case object BottomLeft  extends PageCorner(Point(0, 1).getOrElse(throw new AssertionError()))
    case object BottomRight extends PageCorner(Point(1, 1).getOrElse(throw new AssertionError()))
  }

  def byDistanceFrom(pageCorner: PageCorner): GeometricOrdering =
    b => distanceBetween(pageCorner.point, pointFor(b))

  def byDistanceFrom(block: Block): GeometricOrdering =
    b => distanceBetween(pointFor(block), pointFor(b))

}
