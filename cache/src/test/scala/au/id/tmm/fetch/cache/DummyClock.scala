package au.id.tmm.fetch.cache

import java.time.{Duration, Instant, Period, ZoneOffset}

import cats.effect.{Clock, Ref}
import cats.syntax.functor._
import cats.{Applicative, Monad}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

// TODO probably move to tmmUtils
final class DummyClock[F[_] : Monad] private (nowRef: Ref[F, Instant]) extends Clock[F] {
  override def applicative: Applicative[F]  = implicitly
  override def monotonic: F[FiniteDuration] = realTime
  override def realTime: F[FiniteDuration] = nowRef.get.map { i =>
    FiniteDuration(Duration.between(Instant.EPOCH, i).toNanos, NANOSECONDS)
  }

  def now: F[Instant]                        = nowRef.get
  def increment(duration: Duration): F[Unit] = nowRef.update(i => i.plus(duration))
  def increment(period: Period): F[Unit]     = nowRef.update(i => i.atZone(ZoneOffset.UTC).plus(period).toInstant)
  def increment(duration: FiniteDuration): F[Unit] =
    nowRef.update(i => i.plus(duration.length, duration.unit.toChronoUnit))
  def tick: F[Unit] = nowRef.update(i => i.plus(Duration.ofNanos(1)))
}

object DummyClock {
  def atEpoch[F[_] : Monad : Ref.Make]: F[DummyClock[F]] = DummyClock(Instant.EPOCH)

  def apply[F[_] : Monad : Ref.Make](t0: Instant): F[DummyClock[F]] =
    for {
      now <- Ref[F].of(t0)
    } yield new DummyClock(now)
}
