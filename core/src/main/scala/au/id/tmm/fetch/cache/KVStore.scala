package au.id.tmm.fetch.cache

import cats.Functor
import cats.implicits.toFunctorOps

trait KVStore[F[_], -K, V_IN, V_OUT] {

  def get(k: K): F[Option[V_OUT]]

  def put(k: K, v: V_IN): F[V_OUT]

  def contains(k: K): F[Boolean]

  def contraFlatMap[K1](f: K1 => F[K])(implicit F: Functor[F]): KVStore[F, K1, V_IN, V_OUT] = new KVStore[F, K1, V_IN, V_OUT] {
    override def get(k: K1): F[Option[V_OUT]] = f(k).map(KVStore.this.get)

    override def put(k: K1, v: V_IN): F[V_OUT] = f(k).map(KVStore.this.put(_, v))

    override def contains(k: K1): F[Boolean] = f(k).map(KVStore.this.contains)
  }

}

object KVStore {
}