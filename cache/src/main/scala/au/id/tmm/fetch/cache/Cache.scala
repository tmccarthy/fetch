package au.id.tmm.fetch.cache

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._

// TODO ttl?
final class Cache[F[_], K, V_IN, V_OUT] private (store: KVStore[F, K, V_IN, V_OUT])(implicit F: Monad[F]) {

  def get(k: K)(fetch: F[V_IN]): F[V_OUT] =
    for {
      maybeV <- store.get(k)
      v <- maybeV match {
        case Some(v) => F.pure(v)
        case None =>
          for {
            vIn <- fetch
            v   <- store.put(k, vIn)
          } yield v
      }
    } yield v

}

object Cache {

  def apply[F[_], K, V_IN, V_OUT](store: KVStore[F, K, V_IN, V_OUT])(implicit F: Monad[F]): Cache[F, K, V_IN, V_OUT] =
    new Cache(store)

}
