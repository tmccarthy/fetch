package au.id.tmm.fetch.files

import java.io.{IOException, InputStream}
import java.nio.charset.Charset

import cats.effect.IO
import com.github.tototoshi.csv.{CSVFormat, CSVParser, CSVReader}
import fs2.Pipe

object Csv {

  // TODO consider https://fs2-data.gnieh.org/documentation/csv/

  // TODO could probably narrow the effect type here
  def streamCsvPipe(csvFormat: CSVFormat): Pipe[IO, String, List[String]] = {
    val parser: CSVParser = new CSVParser(csvFormat)

    stream =>
      stream.evalMap { line =>
        parser.parseLine(line) match {
          case Some(row) => IO.pure(row)
          case None      => IO.raiseError(new IOException(s"""Invalid line '${line.take(500)}'"""))
        }
      }
  }

  def streamCsv(
    csvFormat: CSVFormat,
    makeInputStream: IO[InputStream],
    charset: Charset,
  ): fs2.Stream[IO, List[String]] =
    for {
      csvReader <- csvReaderFrom(csvFormat, makeInputStream, charset)
      row       <- fs2.Stream.fromIterator[IO](csvReader.iterator, chunkSize = 100)
    } yield row.toList

  def streamCsvWithHeaders(
    csvFormat: CSVFormat,
    makeInputStream: IO[InputStream],
    charset: Charset,
  ): fs2.Stream[IO, Map[String, String]] =
    for {
      csvReader      <- csvReaderFrom(csvFormat, makeInputStream, charset)
      rowWithHeaders <- fs2.Stream.fromIterator[IO](csvReader.iteratorWithHeaders, chunkSize = 100)
    } yield rowWithHeaders

  private def csvReaderFrom(
    csvFormat: CSVFormat,
    makeInputStream: IO[InputStream],
    charset: Charset,
  ): fs2.Stream[IO, CSVReader] =
    for {
      inputStream <- fs2.Stream.resource(Closeables.resourceFrom(makeInputStream))
      csvReader = CSVReader.open(new java.io.InputStreamReader(inputStream, charset))(csvFormat)
    } yield csvReader

}
