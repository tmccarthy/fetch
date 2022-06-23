package au.id.tmm.fetch.files

import java.io.{IOException, InputStream}
import java.nio.file.Path
import java.util.zip.{ZipEntry, ZipFile}

import cats.effect.{IO, Resource}
import cats.syntax.monadError._

import scala.jdk.CollectionConverters.IteratorHasAsScala

object Zip {

  def openZipFile(zipFilePath: Path): Resource[IO, ZipFile] =
    Closeables.resourceFrom(IO(new ZipFile(zipFilePath.toFile)))

  def listZipEntries(zipFile: ZipFile): fs2.Stream[IO, ZipEntry] =
    fs2.Stream.fromIterator[IO](zipFile.entries().asIterator().asScala, chunkSize = 5)

  def openZipEntry(zipFilePath: Path, zipEntryName: String): Resource[IO, InputStream] =
    Closeables.resourceFrom(IO(new ZipFile(zipFilePath.toFile))).flatMap { zipFile =>
      Resource.suspend {
        for {
          maybeZipEntry <- IO(Option(zipFile.getEntry(zipEntryName))).adaptError { case e: IllegalStateException =>
            new IOException("Zip file was unexpectedly closed", e)
          }

          zipEntry <- maybeZipEntry match {
            case Some(zipEntry) => IO.pure(zipEntry): IO[ZipEntry]
            case None =>
              IO.raiseError(new IOException(s"No zip entry of name $zipEntryName in zip file $zipFilePath")): IO[
                ZipEntry,
              ]
          }
        } yield openZipEntry(zipFile, zipEntry)
      }
    }

  def openZipEntry(zipFile: ZipFile, zipEntry: ZipEntry): Resource[IO, InputStream] =
    Closeables.resourceFrom(IO(zipFile.getInputStream(zipEntry)))

}
