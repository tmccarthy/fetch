package au.id.tmm.fetch.cache

import java.nio.file.Path

import au.id.tmm.fetch.cache.sqlite.SqliteStore
import au.id.tmm.fetch.files.BytesSource
import cats.effect.{IO, Resource}
import cats.syntax.traverse._
import io.circe.{Decoder, Encoder, KeyEncoder, parser}
import sttp.model.Uri

// TODO not sure how useful this is
object Stores {

  def localFsUriStore(directory: Path): IO[KVStore[IO, Uri, BytesSource, Path]] =
    for {
      store <- LocalFsStore(directory)
    } yield store.evalContramapKey(KeySchemes.naiveUriAsPath)

  def localStringStore(location: Path): Resource[IO, KVStore[IO, String, String, String]] =
    SqliteStore.at(location)

  def localJsonStore[K : KeyEncoder, V : Encoder : Decoder](location: Path): Resource[IO, KVStore[IO, K, V, V]] =
    SqliteStore.at(location).map { stringStore =>
      new KVStore[IO, K, V, V] {
        override def get(k: K): IO[Option[V]]    = stringStore.get(encodeKey(k)).flatMap(_.traverse(decode))
        override def contains(k: K): IO[Boolean] = stringStore.contains(encodeKey(k))
        override def put(k: K, v: V): IO[V]      = stringStore.put(encodeKey(k), encode(v)).flatMap(decode)
        override def drop(k: K): IO[Unit]        = stringStore.drop(encodeKey(k))

        private def decode(string: String): IO[V] = IO.fromEither(parser.decode[V](string))
        private def encode(v: V): String          = Encoder[V].apply(v).noSpaces
        private def encodeKey(k: K): String       = KeyEncoder[K].apply(k)
      }
    }

}
