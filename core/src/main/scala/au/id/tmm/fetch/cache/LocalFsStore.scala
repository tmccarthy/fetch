package au.id.tmm.fetch.cache

import java.io.InputStream
import java.nio.file.{Files, Path}

import au.id.tmm.fetch.cache.LocalFsStore.{Entry, Input}
import au.id.tmm.fetch.files.Downloading
import au.id.tmm.utilities.errors.GenericException
import cats.effect.kernel.Sync
import cats.effect.{IO, Resource}

class LocalFsStore private (private val directory: Path) extends KVStore[IO, Path, LocalFsStore.Input, LocalFsStore.Entry] {
  private def resolve(path: Path): IO[Path] =
    for {
      resolved <- IO(directory.resolve(path))
      isDirectory <- IO(Files.isDirectory(resolved))
      _ <- IO.raiseWhen(isDirectory)(GenericException(s"$resolved is a directory"))
      isInStoreDir <- isInStoreDirectory(path)
      _ <- IO.raiseUnless(isInStoreDir)(GenericException(s"$resolved is outside store directory"))
    } yield resolved

  private def isInStoreDirectory(path: Path): IO[Boolean] =
    Sync[IO].tailRecM(path.toAbsolutePath) { p =>
      IO(Option(p.getParent))
        .flatMap {
          case Some(parent) => IO(Files.isSameFile(directory, parent)).flatMap {
            case true => IO.pure(Right(true))
            case false => IO.pure(Left(parent))
          }
          case None => IO.pure(Right(false))
        }
    }

  override def get(k: Path): IO[Option[LocalFsStore.Entry]] =
    for {
      resolvedPath <- resolve(k)
      present <- contains(resolvedPath)
      result = if (present) Some(new Entry(resolvedPath)) else None
    } yield result

  override def put(k: Path, v: LocalFsStore.Input): IO[Unit] =
    for {
      resolvedPath <- resolve(k)
      _ <- v match {
        case Input.JavaIS(makeIS) => Downloading.inputStreamToPath(resolvedPath, replaceExisting = true, makeIS)
        case Input.Fs2Stream(stream) => Downloading.fs2StreamToPath(stream, resolvedPath, replaceExisting = true)
      }
    } yield new Entry(resolvedPath)

  override def contains(k: Path): IO[Boolean] = IO(Files.exists(k))
}

object LocalFsStore {
  def apply(directory: Path): LocalFsStore = new LocalFsStore(directory.toAbsolutePath)

  sealed trait Input

  object Input {
    final case class JavaIS(makeIS: IO[InputStream]) extends Input
    final case class Fs2Stream(stream: fs2.Stream[IO, Byte]) extends Input
  }

  final class Entry private[LocalFsStore] (private val path: Path) {
    val javaIS: IO[InputStream] = IO(Files.newInputStream(path))
    val javaISResource: Resource[IO, InputStream]   = Resource.fromAutoCloseable(javaIS)

    val fs2Path: fs2.io.file.Path       = fs2.io.file.Path.fromNioPath(path)
    val fs2Stream: fs2.Stream[IO, Byte] = fs2.io.file.Files[IO].readAll(fs2Path)
  }
}