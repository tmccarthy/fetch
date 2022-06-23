package au.id.tmm.fetch.files

import java.io.InputStream
import java.nio.file.{Files, Path, StandardCopyOption}

import cats.effect.IO

object Downloading {

  def inputStreamToPath(
    destination: Path,
    replaceExisting: Boolean,
    makeStream: IO[InputStream],
  ): IO[Unit] =
    withReplaceExistingCheck(destination, replaceExisting)(
      Closeables.resourceFrom(makeStream).use[Unit] { stream =>
        val copyOptions = Option.when(replaceExisting)(StandardCopyOption.REPLACE_EXISTING).toList
        IO(Files.copy(stream, destination, copyOptions: _*)).as(())
      },
    )

  def fs2StreamToPath(
    stream: fs2.Stream[IO, Byte],
    destination: Path,
    replaceExisting: Boolean,
  ): IO[Unit] = withReplaceExistingCheck(destination, replaceExisting)(
    stream.through(fs2.io.file.Files[IO].writeAll(fs2.io.file.Path.fromNioPath(destination))).compile.drain,
  )

  // TODO this is wrong
  private def withReplaceExistingCheck(destination: Path, replaceExisting: Boolean)(usePath: IO[Unit]): IO[Unit] =
    if (replaceExisting) {
      usePath
    } else {
      for {
        alreadyExists <- IO(Files.exists(destination))
        a             <- if (alreadyExists) IO.unit else usePath
      } yield a
    }

}
