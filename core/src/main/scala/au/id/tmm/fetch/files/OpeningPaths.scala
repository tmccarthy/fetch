package au.id.tmm.fetch.files

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path}
import java.util.zip.ZipFile

import cats.effect.IO
import cats.effect.kernel.Resource
import com.github.tototoshi.csv.{CSVFormat, DefaultCSVFormat}

object OpeningPaths {
  def makeInputStream(path: Path): IO[InputStream]       = IO(Files.newInputStream(path))
  def inputStream(path: Path): Resource[IO, InputStream] = Closeables.resourceFrom(makeInputStream(path))

  def fs2Path(path: Path): fs2.io.file.Path       = fs2.io.file.Path.fromNioPath(path)
  def fs2Stream(path: Path): fs2.Stream[IO, Byte] = fs2.io.file.Files[IO].readAll(fs2Path(path))

  def lines(path: Path, charset: Charset = StandardCharsets.UTF_8): fs2.Stream[IO, String] =
    fs2Stream(path).through(Text.lines(charset))

  def csvLines(
    path: Path,
    format: CSVFormat = new DefaultCSVFormat {},
    charset: Charset = StandardCharsets.UTF_8,
  ): fs2.Stream[IO, List[String]] =
    Csv.streamCsv(format, makeInputStream(path), charset)
  def csvLinesWithHeader(
    path: Path,
    format: CSVFormat = new DefaultCSVFormat {},
    charset: Charset = StandardCharsets.UTF_8,
  ): fs2.Stream[IO, Map[String, String]] =
    Csv.streamCsvWithHeaders(format, makeInputStream(path), charset)

  def zipFile(path: Path): Resource[IO, ZipFile] = Zip.openZipFile(path)
}
