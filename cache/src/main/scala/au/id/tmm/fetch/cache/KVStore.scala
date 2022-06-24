package au.id.tmm.fetch.cache

import cats.Monad
import cats.syntax.traverse._
import cats.syntax.flatMap._

trait KVStore[F[_], K, V_IN, V_OUT] {

  def get(k: K): F[Option[V_OUT]]

  def put(k: K, v: V_IN): F[V_OUT]

  def contains(k: K): F[Boolean]

  def contraFlatMapKey[K1](f: K1 => F[K])(implicit F: Monad[F]): KVStore[F, K1, V_IN, V_OUT] =
    new KVStore[F, K1, V_IN, V_OUT] {
      override def get(k: K1): F[Option[V_OUT]] = f(k).flatMap(KVStore.this.get)

      override def put(k: K1, v: V_IN): F[V_OUT] = f(k).flatMap(KVStore.this.put(_, v))

      override def contains(k: K1): F[Boolean] = f(k).flatMap(KVStore.this.contains)
    }

  def flatMapValue[V_OUT_1](f: V_OUT => F[V_OUT_1])(implicit F: Monad[F]): KVStore[F, K, V_IN, V_OUT_1] =
    new KVStore[F, K, V_IN, V_OUT_1] {
      override def get(k: K): F[Option[V_OUT_1]] = KVStore.this.get(k).flatMap(optionF => optionF.traverse(f))

      override def put(k: K, v: V_IN): F[V_OUT_1] = KVStore.this.put(k, v).flatMap(f)

      override def contains(k: K): F[Boolean] = KVStore.this.contains(k)
    }

}

object KVStore {}
