package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.fetch.aws.textract.model.Geometry.Polygon.Point
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}

import scala.collection.immutable.ArraySeq

final case class Geometry(
  boundingBox: Geometry.BoundingBox,
  polygon: Geometry.Polygon,
)

object Geometry {

  private def requireNonNegative(double: Float): ExceptionOr[Float] =
    if (double >= 0) {
      Right(double)
    } else {
      Left(GenericException(s"Expected positive but was $double"))
    }

  final case class BoundingBox private (
    left: Float,
    top: Float,
    height: Float,
    width: Float,
  ) {
    def bottom: Float = top + height
    def right: Float  = left + width
    def centre: Point =
      Point(top + (height / 2f), left + (width / 2)) match {
        case Right(p) => p
        case Left(e)  => throw new AssertionError(e)
      }
  }

  object BoundingBox {
    def apply(
      left: Float,
      top: Float,
      height: Float,
      width: Float,
    ): ExceptionOr[BoundingBox] =
      for {
        left   <- requireNonNegative(left)
        top    <- requireNonNegative(top)
        height <- requireNonNegative(height)
        width  <- requireNonNegative(width)
      } yield new BoundingBox(left, top, height, width)
  }

  final case class Polygon(
    points: ArraySeq[Polygon.Point],
  ) extends AnyVal

  object Polygon {

    final case class Point private (x: Float, y: Float)

    object Point {
      def apply(
        x: Float,
        y: Float,
      ): ExceptionOr[Point] =
        for {
          x <- requireNonNegative(x)
          y <- requireNonNegative(y)
        } yield new Point(x, y)
    }

  }

}
