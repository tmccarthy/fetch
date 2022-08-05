package au.id.tmm.fetch.cache

import java.io.IOException
import java.nio.file.{Files, Path}

import au.id.tmm.fetch.files.{BytesSource, Downloading}
import au.id.tmm.utilities.errors.GenericException
import cats.effect.IO

class LocalFsStore private (private val directory: Path) extends KVStore[IO, Path, BytesSource, Path] {
  private def resolve(path: Path): IO[Path] =
    for {
      resolved    <- IO(directory.resolve(path))
      isDirectory <- IO(Files.isDirectory(resolved))
      _           <- IO.raiseWhen(isDirectory)(GenericException(s"$resolved is a directory"))
    } yield resolved

  override def get(k: Path): IO[Option[Path]] =
    for {
      resolvedPath <- resolve(k)
      present      <- contains(resolvedPath)
      result = if (present) Some(resolvedPath) else None
    } yield result

  override def contains(k: Path): IO[Boolean] = IO(Files.exists(k))

  override def put(k: Path, v: BytesSource): IO[Path] =
    for {
      resolvedPath          <- resolve(k)
      parentDirectoryExists <- IO(Files.exists(resolvedPath.getParent))
      _ <- IO.whenA(!parentDirectoryExists) {
        IO(Files.createDirectories(resolvedPath.getParent)).as(())
      }
      _ <- v match {
        case BytesSource.OfJavaInputStream(makeIS) =>
          Downloading.inputStreamToPath(resolvedPath, replaceExisting = true, makeIS)
        case BytesSource.OfFs2Stream(stream) =>
          Downloading.fs2StreamToPath(stream, resolvedPath, replaceExisting = true)
        case BytesSource.Suspend(makeBytes) =>
          makeBytes.flatMap(bytes => Downloading.bytesToPath(resolvedPath, replaceExisting = true, bytes))
      }
    } yield resolvedPath

  override def drop(k: Path): IO[Unit] =
    for {
      resolvedPath <- resolve(k)
      _            <- IO(Files.deleteIfExists(resolvedPath))
    } yield ()
}

object LocalFsStore {
  def apply(directory: Path): IO[LocalFsStore] =
    for {
      absoluteDir <- IO(directory.toAbsolutePath)
      valid       <- IO(Files.isDirectory(absoluteDir))
      _           <- IO.raiseWhen(!valid)(new IOException(s"$absoluteDir is not a directory"))
    } yield new LocalFsStore(absoluteDir)
}
