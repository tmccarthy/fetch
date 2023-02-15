package au.id.tmm.fetch.cache

import cats.{Functor, Monad}
import cats.effect.IO
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._

trait KVStore[F[_], K, V_IN, V_OUT] {

  def get(k: K): F[Option[V_OUT]]

  def contains(k: K): F[Boolean]

  def put(k: K, v: V_IN): F[V_OUT]

  def drop(k: K): F[Unit]

  def contramapKey[K1](f: K1 => K): KVStore[F, K1, V_IN, V_OUT] =
    new KVStore[F, K1, V_IN, V_OUT] {
      override def get(k1: K1): F[Option[V_OUT]]  = KVStore.this.get(f(k1))
      override def contains(k1: K1): F[Boolean]   = KVStore.this.contains(f(k1))
      override def put(k1: K1, v: V_IN): F[V_OUT] = KVStore.this.put(f(k1), v)
      override def drop(k1: K1): F[Unit]          = KVStore.this.drop(f(k1))
    }

  def evalContramapKey[K1](f: K1 => F[K])(implicit F: Monad[F]): KVStore[F, K1, V_IN, V_OUT] =
    new KVStore[F, K1, V_IN, V_OUT] {
      override def get(k1: K1): F[Option[V_OUT]]  = f(k1).flatMap(KVStore.this.get)
      override def contains(k1: K1): F[Boolean]   = f(k1).flatMap(KVStore.this.contains)
      override def put(k1: K1, v: V_IN): F[V_OUT] = f(k1).flatMap(KVStore.this.put(_, v))
      override def drop(k1: K1): F[Unit]          = f(k1).flatMap(KVStore.this.drop)
    }

  def contramapValueIn[V_IN_1](f: V_IN_1 => V_IN): KVStore[F, K, V_IN_1, V_OUT] =
    new KVStore[F, K, V_IN_1, V_OUT] {
      override def get(k: K): F[Option[V_OUT]]     = KVStore.this.get(k)
      override def contains(k: K): F[Boolean]      = KVStore.this.contains(k)
      override def put(k: K, v1: V_IN_1): F[V_OUT] = KVStore.this.put(k, f(v1))
      override def drop(k: K): F[Unit]             = KVStore.this.drop(k)
    }

  def evalContramapValueIn[V_IN_1](f: V_IN_1 => F[V_IN])(implicit F: Monad[F]): KVStore[F, K, V_IN_1, V_OUT] =
    new KVStore[F, K, V_IN_1, V_OUT] {
      override def get(k: K): F[Option[V_OUT]]    = KVStore.this.get(k)
      override def contains(k: K): F[Boolean]     = KVStore.this.contains(k)
      override def put(k: K, v: V_IN_1): F[V_OUT] = f(v).flatMap(KVStore.this.put(k, _))
      override def drop(k: K): F[Unit]            = KVStore.this.drop(k)
    }

  def mapValueOut[V_OUT_1](f: V_OUT => V_OUT_1)(implicit F: Functor[F]): KVStore[F, K, V_IN, V_OUT_1] =
    new KVStore[F, K, V_IN, V_OUT_1] {
      override def get(k: K): F[Option[V_OUT_1]]  = KVStore.this.get(k).map(_.map(f))
      override def contains(k: K): F[Boolean]     = KVStore.this.contains(k)
      override def put(k: K, v: V_IN): F[V_OUT_1] = KVStore.this.put(k, v).map(f)
      override def drop(k: K): F[Unit]            = KVStore.this.drop(k)
    }

  def evalMapValueOut[V_OUT_1](f: V_OUT => F[V_OUT_1])(implicit F: Monad[F]): KVStore[F, K, V_IN, V_OUT_1] =
    new KVStore[F, K, V_IN, V_OUT_1] {
      override def get(k: K): F[Option[V_OUT_1]]  = KVStore.this.get(k).flatMap(optionF => optionF.traverse(f))
      override def contains(k: K): F[Boolean]     = KVStore.this.contains(k)
      override def put(k: K, v: V_IN): F[V_OUT_1] = KVStore.this.put(k, v).flatMap(f)
      override def drop(k: K): F[Unit]            = KVStore.this.drop(k)
    }

}

object KVStore {
  type SimpleIO[K, V] = KVStore[IO, K, V, V]
}
