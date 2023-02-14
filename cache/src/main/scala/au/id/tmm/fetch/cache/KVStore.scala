package au.id.tmm.fetch.cache

import cats.Monad
import cats.effect.IO
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._

trait KVStore[F[_], K, V_IN, V_OUT] {

  def get(k: K): F[Option[V_OUT]]

  def contains(k: K): F[Boolean]

  def put(k: K, v: V_IN): F[V_OUT]

  def drop(k: K): F[Unit]

  // TODO could be done to be weaker in F
  def contramapKey[K1](f: K1 => K)(implicit F: Monad[F]): KVStore[F, K1, V_IN, V_OUT] =
    evalContramapKey(k1 => F.pure(f(k1)))

  def evalContramapKey[K1](f: K1 => F[K])(implicit F: Monad[F]): KVStore[F, K1, V_IN, V_OUT] =
    new KVStore[F, K1, V_IN, V_OUT] {
      override def get(k: K1): F[Option[V_OUT]] = f(k).flatMap(KVStore.this.get)

      override def contains(k: K1): F[Boolean] = f(k).flatMap(KVStore.this.contains)

      override def put(k: K1, v: V_IN): F[V_OUT] = f(k).flatMap(KVStore.this.put(_, v))

      override def drop(k: K1): F[Unit] = f(k).flatMap(KVStore.this.drop)
    }

  // TODO could be done to be weaker in F
  def contramapValueIn[V_IN_1](f: V_IN_1 => V_IN)(implicit F: Monad[F]): KVStore[F, K, V_IN_1, V_OUT] =
    evalContramapValueIn(v1 => F.pure(f(v1)))

  def evalContramapValueIn[V_IN_1](f: V_IN_1 => F[V_IN])(implicit F: Monad[F]): KVStore[F, K, V_IN_1, V_OUT] =
    new KVStore[F, K, V_IN_1, V_OUT] {
      override def get(k: K): F[Option[V_OUT]] = KVStore.this.get(k)

      override def contains(k: K): F[Boolean] = KVStore.this.contains(k)

      override def put(k: K, v: V_IN_1): F[V_OUT] = f(v).flatMap(KVStore.this.put(k, _))

      override def drop(k: K): F[Unit] = KVStore.this.drop(k)
    }

  // TODO could be done to be weaker in F
  def mapValueOut[V_OUT_1](f: V_OUT => V_OUT_1)(implicit F: Monad[F]): KVStore[F, K, V_IN, V_OUT_1] =
    evalMapValueOut(vOut => F.pure(f(vOut)))

  def evalMapValueOut[V_OUT_1](f: V_OUT => F[V_OUT_1])(implicit F: Monad[F]): KVStore[F, K, V_IN, V_OUT_1] =
    new KVStore[F, K, V_IN, V_OUT_1] {
      override def get(k: K): F[Option[V_OUT_1]] = KVStore.this.get(k).flatMap(optionF => optionF.traverse(f))

      override def contains(k: K): F[Boolean] = KVStore.this.contains(k)

      override def put(k: K, v: V_IN): F[V_OUT_1] = KVStore.this.put(k, v).flatMap(f)

      override def drop(k: K): F[Unit] = KVStore.this.drop(k)
    }

}

object KVStore {
  type SimpleIO[K, V] = KVStore[IO, K, V, V]
}
