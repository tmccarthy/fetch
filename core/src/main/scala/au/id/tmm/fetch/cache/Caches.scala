package au.id.tmm.fetch.cache

import java.nio.file.Path

import cats.effect.IO
import sttp.model.Uri

object Caches {

  def localFsUriCache(directory: Path): Cache[IO, Uri, LocalFsStore.Input, LocalFsStore.Entry] =
    Cache(LocalFsStore(directory).contraFlatMap(KeySchemes.uriAsPath))

}
