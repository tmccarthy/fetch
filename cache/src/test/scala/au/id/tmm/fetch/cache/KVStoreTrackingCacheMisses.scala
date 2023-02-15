package au.id.tmm.fetch.cache

import cats.Monad
import cats.effect.{IO, Ref}

/**
  * KVStore that tracks get calls. Useful for testing caches
  */
final class KVStoreTrackingCacheMisses[K, V] private (
  underlying: KVStore[IO, K, V, V],
  cacheMisses: Ref[IO, List[K]],
  puts: Ref[IO, List[K]],
)(implicit
  IO: Monad[IO],
) extends KVStore[IO, K, V, V] {
  def popCacheMisses: IO[List[K]] = cacheMisses.getAndSet(List.empty)

  def popPuts: IO[List[K]] = puts.getAndSet(List.empty)

  override def get(k: K): IO[Option[V]] =
    for {
      v <- underlying.get(k)
      _ <- v match {
        case Some(_) => IO.unit
        case None    => cacheMisses.update(_.appended(k))
      }
    } yield v

  override def contains(k: K): IO[Boolean] = underlying.contains(k)
  override def put(k: K, v: V): IO[V] =
    for {
      v <- underlying.put(k, v)
      _ <- puts.update(_.appended(k))
    } yield v

  override def drop(k: K): IO[Unit] = underlying.drop(k)
}

object KVStoreTrackingCacheMisses {
  def apply[K, V](underlying: KVStore[IO, K, V, V]): IO[KVStoreTrackingCacheMisses[K, V]] =
    for {
      cacheMisses <- Ref[IO].of(List.empty[K])
      puts        <- Ref[IO].of(List.empty[K])
    } yield new KVStoreTrackingCacheMisses(underlying, cacheMisses, puts)

}
