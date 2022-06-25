package au.id.tmm.fetch.files

import java.io.InputStream
import java.nio.charset.Charset

import cats.effect.IO

import scala.io.Codec

object Text {

  def string(
    makeInputStream: IO[InputStream],
    charset: Charset,
  ): IO[String] =
    Closeables.resourceFrom(makeInputStream).use { is =>
      IO(new String(is.readAllBytes(), charset))
    }

  def lines(charset: Charset): fs2.Pipe[IO, Byte, String] =
    _.through(fs2.text.decodeWithCharset(charset))
      .through(fs2.text.lines)

  def lines(
    makeInputStream: IO[InputStream],
    charset: Charset,
    chunkSize: Int = 5,
  ): fs2.Stream[IO, String] =
    for {
      inputStream <- fs2.Stream.resource(Closeables.resourceFrom(makeInputStream))
      source      <- fs2.Stream.eval(IO(scala.io.Source.fromInputStream(inputStream)(Codec(charset))))
      lines       <- fs2.Stream.fromIterator[IO](source.getLines(), chunkSize)
    } yield lines

}
