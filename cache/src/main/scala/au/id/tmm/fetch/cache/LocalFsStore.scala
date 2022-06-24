package au.id.tmm.fetch.cache

import java.nio.file.{Files, Path}

import au.id.tmm.fetch.files.{BytesSource, Downloading}
import au.id.tmm.utilities.errors.GenericException
import cats.effect.IO
import cats.effect.kernel.Sync

class LocalFsStore private (private val directory: Path) extends KVStore[IO, Path, BytesSource, Path] {
  private def resolve(path: Path): IO[Path] =
    for {
      resolved     <- IO(directory.resolve(path))
      isDirectory  <- IO(Files.isDirectory(resolved))
      _            <- IO.raiseWhen(isDirectory)(GenericException(s"$resolved is a directory"))
      isInStoreDir <- isInStoreDirectory(path)
      _            <- IO.raiseUnless(isInStoreDir)(GenericException(s"$resolved is outside store directory"))
    } yield resolved

  private def isInStoreDirectory(path: Path): IO[Boolean] =
    Sync[IO].tailRecM(path.toAbsolutePath) { p =>
      IO(Option(p.getParent))
        .flatMap {
          case Some(parent) =>
            IO(Files.isSameFile(directory, parent)).flatMap {
              case true  => IO.pure(Right(true))
              case false => IO.pure(Left(parent))
            }
          case None => IO.pure(Right(false))
        }
    }

  override def get(k: Path): IO[Option[Path]] =
    for {
      resolvedPath <- resolve(k)
      present      <- contains(resolvedPath)
      result = if (present) Some(resolvedPath) else None
    } yield result

  override def put(k: Path, v: BytesSource): IO[Path] =
    for {
      resolvedPath <- resolve(k)
      _ <- v match {
        case BytesSource.OfJavaInputStream(makeIS) =>
          Downloading.inputStreamToPath(resolvedPath, replaceExisting = true, makeIS)
        case BytesSource.OfFs2Stream(stream) =>
          Downloading.fs2StreamToPath(stream, resolvedPath, replaceExisting = true)
      }
    } yield resolvedPath

  override def contains(k: Path): IO[Boolean] = IO(Files.exists(k))
}

object LocalFsStore {
  def apply(directory: Path): LocalFsStore = new LocalFsStore(directory.toAbsolutePath)
}
