package au.id.tmm.fetch.cache

import cats.Monad
import cats.effect.{IO, Ref}
import cats.syntax.flatMap._
import cats.syntax.functor._

/**
  * KVStore that operates on an in-memory Map
  */
final class InMemoryKVStore[F[_], K, V_IN, V_OUT] private (
  mapRef: Ref[F, Map[K, V_OUT]],
  evalMapV: V_IN => F[V_OUT],
)(implicit
  F: Monad[F],
) extends KVStore[F, K, V_IN, V_OUT] {
  override def get(k: K): F[Option[V_OUT]] =
    for {
      map <- mapRef.get
    } yield map.get(k)

  override def contains(k: K): F[Boolean] =
    for {
      map <- mapRef.get
    } yield map.contains(k)

  override def put(k: K, vIn: V_IN): F[V_OUT] =
    for {
      vOut <- evalMapV(vIn)
      _    <- mapRef.update(_.updated(k, vOut))
    } yield vOut

  override def drop(k: K): F[Unit] =
    mapRef.update(_.removed(k))
}

object InMemoryKVStore {
  def apply[F[_] : Ref.Make : Monad, K, V_IN, V_OUT](
    evalMapV: V_IN => F[V_OUT],
  ): F[InMemoryKVStore[F, K, V_IN, V_OUT]] =
    for {
      mapRef <- Ref[F].of(Map.empty[K, V_OUT])
    } yield new InMemoryKVStore(mapRef, evalMapV)

  type SimpleIO[K, V] = InMemoryKVStore[IO, K, V, V]
  def SimpleIO[K, V]: IO[SimpleIO[K, V]] = apply[IO, K, V, V](IO.pure)
}
