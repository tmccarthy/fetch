package au.id.tmm.fetch.cache

import java.time.{Duration, Instant}

import cats.Monad
import cats.effect.{Clock, IO}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.{Codec, Decoder, Encoder}

import scala.math.Ordering.Implicits.infixOrderingOps

sealed trait Cache[F[_], K, V_IN, V_OUT] {
  def get(k: K)(fetch: F[V_IN]): F[V_OUT]
}

object Cache {

  def apply[F[_], K, V_IN, V_OUT](store: KVStore[F, K, V_IN, V_OUT])(implicit F: Monad[F]): Cache[F, K, V_IN, V_OUT] =
    new Cache.Plain[F, K, V_IN, V_OUT](store)

  type SimpleIO[K, V] = Cache[IO, K, V, V]
  def SimpleIO[K, V](store: KVStore.SimpleIO[K, V]): Cache.SimpleIO[K, V] = apply(store)

  private final class Plain[F[_], K, V_IN, V_OUT](store: KVStore[F, K, V_IN, V_OUT])(implicit F: Monad[F])
      extends Cache[F, K, V_IN, V_OUT] {
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

  private final class WithTtl[F[_], K, V_IN, V_OUT](
    ttl: Duration,
    store: KVStore[F, K, Cache.WithTtl.Timestamped[V_IN], Cache.WithTtl.Timestamped[V_OUT]],
  )(implicit
    F: Monad[F],
    clock: Clock[F],
  ) extends Cache[F, K, V_IN, V_OUT] {
    override def get(k: K)(fetch: F[V_IN]): F[V_OUT] =
      for {
        maybeTimestampAndV <- store.get(k)
        v <- maybeTimestampAndV match {
          case Some(Cache.WithTtl.Timestamped(timestamp, existingValue)) =>
            for {
              now <- clock.realTimeInstant
              isExpired = Duration.between(timestamp, now) > ttl
              v <- if (isExpired) doUpdate(k, fetch) else F.pure(existingValue)
            } yield v
          case None => doUpdate(k, fetch)
        }
      } yield v

    private def doUpdate(k: K, fetch: F[V_IN]): F[V_OUT] =
      for {
        timestamp       <- clock.realTimeInstant
        vIn             <- fetch
        timestampedNewV <- store.put(k, Cache.WithTtl.Timestamped(timestamp, vIn))
      } yield timestampedNewV.a
  }

  object WithTtl {
    def apply[F[_], K, V_IN, V_OUT](
      ttl: Duration,
      store: KVStore[F, K, Cache.WithTtl.Timestamped[V_IN], Cache.WithTtl.Timestamped[V_OUT]],
    )(implicit
      F: Monad[F],
      clock: Clock[F],
    ): Cache[F, K, V_IN, V_OUT] =
      new Cache.WithTtl(ttl, store)

    final case class Timestamped[A](timestamp: Instant, a: A)

    object Timestamped {
      implicit def codec[A : Encoder : Decoder]: Codec[Timestamped[A]] =
        Codec.from(
          Decoder.forProduct2("timestamp", "value")(Timestamped.apply[A]),
          Encoder.forProduct2("timestamp", "value")(t => (t.timestamp, t.a)),
        )
    }
  }

}
