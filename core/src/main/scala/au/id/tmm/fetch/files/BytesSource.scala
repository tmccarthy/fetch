package au.id.tmm.fetch.files

import java.io.InputStream

import cats.effect.IO

import scala.collection.immutable.ArraySeq

sealed trait BytesSource

object BytesSource {
  def Pure(bytes: ArraySeq[Byte]): BytesSource.Suspend = BytesSource.Suspend(IO.pure(bytes))

  final case class OfJavaInputStream(makeIS: IO[InputStream]) extends BytesSource
  final case class OfFs2Stream(stream: fs2.Stream[IO, Byte])  extends BytesSource
  final case class Suspend(makeBytes: IO[ArraySeq[Byte]])     extends BytesSource
}
