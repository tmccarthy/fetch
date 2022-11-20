package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model.Geometry
import au.id.tmm.fetch.aws.textract.model.Geometry.BoundingBox
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.syntax.traverse.toTraverseOps
import software.amazon.awssdk.services.textract.{model => sdk}

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

private[parsing] object Common {

  def parseGeometry(sdkGeometry: sdk.Geometry): ExceptionOr[Geometry] =
    for {
      sdkBoundingBox <- requireNonNull(sdkGeometry.boundingBox)
      boundingBox <- BoundingBox(
        sdkBoundingBox.left,
        sdkBoundingBox.top,
        sdkBoundingBox.height,
        sdkBoundingBox.width,
      )
      sdkPoints <- requireNonNull(sdkGeometry.polygon()).map(_.asScala.to(ArraySeq))
      points    <- sdkPoints.traverse(p => Geometry.Polygon.Point(p.x, p.y))
    } yield Geometry(boundingBox, Geometry.Polygon(points))

  def requireNonNull[A](a: A): ExceptionOr[A] =
    if (a == null) {
      Left(new NullPointerException)
    } else {
      Right(a)
    }

  def requireValue[A](a: A, expected: A): ExceptionOr[Unit] =
    if (a == expected) {
      Right(())
    } else {
      Left(GenericException(s"Expected $expected, but was $a"))
    }

  def requireBlockType(block: sdk.Block, expectedType: sdk.BlockType): ExceptionOr[Unit] =
    if (block.blockType() == expectedType) {
      Right(())
    } else {
      Left(GenericException(s"Expected $expectedType, but was ${block.blockType}"))
    }

}
