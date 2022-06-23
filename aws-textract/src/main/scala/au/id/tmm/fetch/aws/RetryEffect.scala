package au.id.tmm.fetch.aws

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import cats.effect.{IO, Timer}

import scala.concurrent.duration.FiniteDuration

// TODO make this a utility somewhere
object RetryEffect {

  sealed trait Result[+A]

  object Result {
    final case class Finished[A](a: A)            extends Result[A]
    final case class FailedFinished(t: Throwable) extends Result[Nothing]
  }

  def exponentialRetry[A](
    op: IO[Result[A]],
    initialDelay: Duration,
    factor: Long,
    maxWait: Duration,
  )(implicit
    timer: Timer[IO],
  ): IO[A] = {
    def go(
      t0: Instant,
      delay: Duration,
    ): IO[A] =
      for {
        _   <- Timer[IO].sleep(FiniteDuration(delay.toMillis, TimeUnit.MILLISECONDS))
        now <- Timer[IO].clock.instantNow
        elapsed = Duration.between(t0, now)

        result <-
          op.attempt
            .flatMap {
              case Right(Result.Finished(a))       => IO.pure(a)
              case Right(Result.FailedFinished(t)) => IO.raiseError(t)
              case Left(t) => {
                val timeHasRunOut = Ordering[Duration].gt(elapsed, maxWait)

                if (timeHasRunOut) {
                  IO.raiseError(t)
                } else {
                  go(t0, delay.multipliedBy(factor))
                }
              }
            }
      } yield result

    Timer[IO].clock.instantNow.flatMap { t0 =>
      go(t0, initialDelay)
    }
  }

}
