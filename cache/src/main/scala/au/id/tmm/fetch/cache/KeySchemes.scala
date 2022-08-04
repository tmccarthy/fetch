package au.id.tmm.fetch.cache

import java.nio.file.{Path, Paths}

import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.data.NonEmptyList
import cats.effect.IO
import org.apache.commons.io.FilenameUtils
import sttp.model.Uri

object KeySchemes {

  def naiveUriAsPathComponents(uri: Uri): ExceptionOr[NonEmptyList[String]] =
    for {
      host <- uri.host.toRight(GenericException(s"Host missing from $uri"))
      fileName = uri.path.lastOption.map(FilenameUtils.getName).filterNot(_.isBlank).getOrElse("root")
    } yield NonEmptyList.of(host, fileName)

  def naiveUriAsPath(uri: Uri): IO[Path] =
    for {
      components <- IO.fromEither(naiveUriAsPathComponents(uri))
      path       <- IO(Paths.get(components.head, components.tail: _*))
    } yield path

}
