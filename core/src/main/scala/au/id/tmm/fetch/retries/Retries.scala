package au.id.tmm.fetch.retries

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import au.id.tmm.fetch.retries.Retries.Result
import cats.effect.IO
import cats.effect.kernel.Clock
import cats.{Applicative, Functor}

import scala.concurrent.duration.FiniteDuration
import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.control.NonFatal

object Retries {

  /**
    * Represents the time that a task was first registered
    */
  final case class T0(asInstant: Instant) extends AnyVal

  object T0 {
    def now[F[_] : Clock : Functor]: F[T0] = Functor[F].map(Clock[F].realTimeInstant)(apply)
  }

  sealed trait Result[+A]

  object Result {

    /**
      * Represents the result of an retryable task which has not yet reached its terminal state.
      *
      * @param cause the exception throw by this non-terminal task, if any
      */
    final case class Continue(cause: Option[Throwable]) extends Result[Nothing]

    /**
      * Represents the successful completion of an retryable task
      */
    final case class Success[A](a: A) extends Result[A]

    /**
      * Represents the unsuccessful completion of an retryable task.
      * @param cause the `Throwable` which represents the
      */
    final case class Failed(cause: Throwable) extends Result[Nothing]
  }

  // TODO generalise in F[_]
  /**
    * Using the given `RetryPolicy`, waits for the given effect to reach a terminal state.
    *
    * @param policy a policy that describes how the task will be retried
    * @param effect an effect to be retried. Uncaught errors will be treated as though a `Result.Continue` was returned
    * @return an `IO` value representing the successful terminal state of the task, or any returned `Result.Failed`
    *         error, or an `EffectTimedOutException` if the task timed out according to the `RetryPolicy`.
    */
  def retry[A, S](policy: RetryPolicy[S])(effect: IO[Result[A]]): IO[A] = {
    def go(
      t0: T0,
      state: S,
      lastFailure: Option[Throwable],
    ): IO[A] =
      for {
        now <- Clock[IO].realTimeInstant
        elapsed = Duration.between(t0.asInstant, now)
        a <-
          if (elapsed > policy.timeout) {
            for {
              (newState, delay) <- policy.computeSleepAndNextState(state)
              _                 <- IO.sleep(FiniteDuration.apply(delay.toMillis, TimeUnit.MILLISECONDS))

              attemptedResult <- effect.attempt

              result <- attemptedResult match {
                case Right(Result.Continue(cause)) => go(t0, newState, cause)
                case Right(Result.Success(a))      => IO.pure(a)
                case Right(Result.Failed(cause))   => go(t0, newState, Some(cause))
                case Left(NonFatal(t))             => go(t0, newState, Some(t))
                case Left(fatal: Throwable)        => IO.raiseError(fatal)
              }
            } yield result
          } else {
            IO.raiseError(EffectTimedOutException(s"Awaitable task timed out after $elapsed", lastFailure))
          }
      } yield a

    for {
      (t0, initialState) <- Applicative[IO].tuple2(T0.now[IO], policy.initialState)
      a                  <- go(t0, initialState, lastFailure = None)
    } yield a
  }

  final case class EffectTimedOutException(
    debugMessage: String,
    lastFailure: Option[Throwable],
  ) extends RuntimeException(s"Awaitable task timed out. $debugMessage", lastFailure.orNull)

}

sealed trait RetryPolicy[S] {
  def timeout: Duration
  def initialState: IO[S]

  def computeSleepAndNextState(state: S): IO[(S, Duration)]

  def retry[A](effect: IO[Result[A]]): IO[A] = Retries.retry[A, S](this)(effect)
}

object RetryPolicy {
  final case class TaskHasTimedOut(debugMessage: String)

  final case class LinearBackoff(delay: Duration, timeout: Duration) extends RetryPolicy[Unit] {
    override def initialState: IO[Unit]                                  = IO.unit
    override def computeSleepAndNextState(x: Unit): IO[(Unit, Duration)] = IO.pure(((), delay))
  }

  final case class ExponentialBackoff(
    initialDelay: Duration,
    factor: Double,
    timeout: Duration,
  ) extends RetryPolicy[ExponentialBackoff.State] {
    override def initialState: IO[ExponentialBackoff.State] = IO.pure(ExponentialBackoff.State(initialDelay))

    override def computeSleepAndNextState(state: ExponentialBackoff.State): IO[(ExponentialBackoff.State, Duration)] = {
      val nextDelay = Duration.ofMillis((state.nextDelay.toMillis * factor).toLong)
      IO.pure((state.copy(nextDelay = nextDelay), state.nextDelay))
    }
  }

  object ExponentialBackoff {
    final case class State(
      nextDelay: Duration,
    )
  }

}
