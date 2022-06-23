package au.id.tmm.fetch.files

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path}
import java.util.zip.ZipFile

import au.id.tmm.fetch.files.LocalFsCache.logger
import au.id.tmm.utilities.errors.ProductException
import cats.effect.IO
import cats.effect.kernel.Resource
import com.github.tototoshi.csv.{CSVFormat, DefaultCSVFormat}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3._
import sttp.model.{StatusCode, Uri}

// TODO this probably needs to be more generic in terms of what kinds of requests (eg not just GET)
// TODO probably needs to track a "version" or something
// TODO could do a TTL based on the operating-system level updated time or a local store of what is going on
final class LocalFsCache private (sttpBackend: SttpBackend[IO, Any], private val directory: Path) {

  def get(uri: java.net.URI): IO[LocalFsCache.Entry] = get(Uri(uri))

  def get(uri: Uri): IO[LocalFsCache.Entry] =
    for {
      destination <- destinationFor(uri)
      _ <- Downloading.inputStreamToPath(
        destination,
        replaceExisting = false,
        makeStream = for {
          _        <- IO(logger.info(s"Downloading $uri"))
          response <- sttpBackend.send(basicRequest.get(uri).response(asByteArray))
          asBytes <- IO.fromEither {
            response.body.left
              .map(LocalFsCache.FetchError(response.code, _))
          }
        } yield new ByteArrayInputStream(asBytes),
      )
    } yield new LocalFsCache.Entry(destination)

  private def destinationFor(uri: Uri): IO[Path] =
    IO(directory.resolve(FilenameUtils.getName(uri.path.last)))

}

object LocalFsCache {

  private val logger: Logger = LoggerFactory.getLogger(classOf[LocalFsCache])

  def apply(sttpBackend: SttpBackend[IO, Any], directory: Path): IO[LocalFsCache] =
    for {
      _ <- IO(Files.createDirectories(directory))
    } yield new LocalFsCache(sttpBackend, directory)

  def temp(sttpBackend: SttpBackend[IO, Any]): Resource[IO, LocalFsCache] =
    Resource.make(
      acquire = for {
        directory <- IO(Files.createTempDirectory(classOf[LocalFsCache].getSimpleName))
        cache     <- LocalFsCache(sttpBackend, directory)
      } yield cache,
    )(
      release = cache => IO(FileUtils.deleteDirectory(cache.directory.toFile)),
    )

  final class Entry private[LocalFsCache] (val path: Path) {
    private val makeInputStream: IO[InputStream] = IO(Files.newInputStream(path))
    val inputStream: Resource[IO, InputStream]   = Closeables.resourceFrom(makeInputStream)

    val fs2Path: fs2.io.file.Path       = fs2.io.file.Path.fromNioPath(path)
    val fs2Stream: fs2.Stream[IO, Byte] = fs2.io.file.Files[IO].readAll(fs2Path)

    def lines(charset: Charset = StandardCharsets.UTF_8): fs2.Stream[IO, String] =
      fs2Stream.through(Text.lines(charset))

    def csvLines(
      format: CSVFormat = new DefaultCSVFormat {},
      charset: Charset = StandardCharsets.UTF_8,
    ): fs2.Stream[IO, List[String]] =
      Csv.streamCsv(format, makeInputStream, charset)
    def csvLinesWithHeader(
      format: CSVFormat = new DefaultCSVFormat {},
      charset: Charset = StandardCharsets.UTF_8,
    ): fs2.Stream[IO, Map[String, String]] =
      Csv.streamCsvWithHeaders(format, makeInputStream, charset)

    val zipFile: Resource[IO, ZipFile] = Zip.openZipFile(path)
  }

  final case class FetchError(code: StatusCode, responseBody: String) extends ProductException

}
