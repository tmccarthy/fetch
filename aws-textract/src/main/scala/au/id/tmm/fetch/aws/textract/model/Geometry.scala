package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.fetch.aws.textract.model.Geometry.Polygon.Point
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import io.circe.{Codec, Decoder, Encoder}

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

    implicit val codec: Codec[BoundingBox] = Codec.from(
      Decoder.forProduct4("left", "top", "height", "width")(BoundingBox.apply).emap(_.left.map(_.getMessage)),
      Encoder.forProduct4("left", "top", "height", "width")(b => (b.left, b.top, b.height, b.width)),
    )
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

      implicit val codec: Codec[Point] = Codec.from(
        Decoder.forProduct2("x", "y")(Point.apply).emap(_.left.map(_.toString)),
        Encoder.forProduct2("x", "y")(p => (p.x, p.y)),
      )
    }

    implicit val codec: Codec[Polygon] = Codec.from(
      Decoder[ArraySeq[Point]].map(Polygon.apply),
      Encoder[ArraySeq[Point]].contramap(_.points),
    )
  }

  implicit val codec: Codec[Geometry] = Codec.from(
    Decoder.forProduct2("boundingBox", "polygon")(Geometry.apply),
    Encoder.forProduct2("boundingBox", "polygon")(g => (g.boundingBox, g.polygon)),
  )

}
