package au.id.tmm.fetch.cache

import java.nio.file.Path

import au.id.tmm.fetch.files.BytesSource
import cats.effect.IO
import sttp.model.Uri

object Caches {

  def localFsUriCache(directory: Path): Cache[IO, Uri, BytesSource, Path] =
    Cache(LocalFsStore(directory).contraFlatMapKey(KeySchemes.uriAsPath))

}
