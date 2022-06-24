package au.id.tmm.fetch.cache

import java.nio.file.{Path, Paths}

import au.id.tmm.utilities.errors.GenericException
import cats.effect.IO
import org.apache.commons.io.FilenameUtils
import sttp.model.Uri

object KeySchemes {

  def uriAsPath(uri: Uri): IO[Path] =
    for {
      host <- IO.fromOption(uri.host)(GenericException(s"Host missing from $uri"))
    } yield Paths.get(host, FilenameUtils.getName(uri.path.last))

}
