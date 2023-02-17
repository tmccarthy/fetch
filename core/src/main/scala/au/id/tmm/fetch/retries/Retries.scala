package au.id.tmm.fetch.retries

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import au.id.tmm.fetch.retries.Retries.Result
import au.id.tmm.fetch.retries.RetryPolicy.TaskHasTimedOut
import cats.effect.IO
import cats.effect.kernel.Clock

import scala.concurrent.duration.FiniteDuration
import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.control.NonFatal

object Retries {

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
  // TODO do I need to expose the S type param here?
  /**
    * Using the given `RetryPolicy`, waits for the given effect to reach a terminal state.
    *
    * @param policy a policy that describes how the task will be retried
    * @param effect an effect to be retried. Uncaught errors will be treated as though a `Result.Continue` was returned
    * @return an `IO` value representing the successful terminal state of the task, or any returned `Result.Failed`
    *         error, or an `EffectTimedOutException` if the task timed out according to the `RetryPolicy`.
    */
  def retry[A, S](policy: RetryPolicy[S])(effect: IO[Result[A]]): IO[A] = {
    def go(state: S, lastFailure: Option[Throwable]): IO[A] =
      for {
        timedOutOrDelay <- policy.checkTimeoutAndComputeSleep(state)

        a <- timedOutOrDelay match {
          case Right((newState, delay)) =>
            for {
              _ <- IO.sleep(FiniteDuration.apply(delay.toMillis, TimeUnit.MILLISECONDS))

              attemptedResult <- effect.attempt

              result <- attemptedResult match {
                case Right(Result.Continue(cause)) => go(newState, cause)
                case Right(Result.Success(a))      => IO.pure(a)
                case Right(Result.Failed(cause))   => go(newState, Some(cause))
                case Left(NonFatal(t))             => go(newState, Some(t))
                case Left(fatal: Throwable)        => IO.raiseError(fatal)
              }
            } yield result

          case Left(TaskHasTimedOut(debugMessage)) =>
            IO.raiseError(EffectTimedOutException(debugMessage, lastFailure))
        }
      } yield a

    for {
      initialState <- policy.initialState
      a            <- go(initialState, lastFailure = None)
    } yield a
  }

  final case class EffectTimedOutException(
    debugMessage: String,
    lastFailure: Option[Throwable],
  ) extends RuntimeException(s"Awaitable task timed out. $debugMessage", lastFailure.orNull)

}

sealed trait RetryPolicy[S] {
  def initialState: IO[S]

  def checkTimeoutAndComputeSleep(state: S): IO[Either[TaskHasTimedOut, (S, Duration)]]

  def retry[A](effect: IO[Result[A]]): IO[A] = Retries.retry[A, S](this)(effect)
}

object RetryPolicy {
  final case class TaskHasTimedOut(debugMessage: String)
  final case class T0(asInstant: Instant) extends AnyVal

  // TODO this stuff should just be in RetryPolicy. There are no RetryPolicies without a timeout
  trait WithTimeout[S] extends RetryPolicy[S] {
    def timeout: Duration
    def t0FromState(state: S): T0
    override final def checkTimeoutAndComputeSleep(state: S): IO[Either[TaskHasTimedOut, (S, Duration)]] =
      for {
        now <- Clock[IO].realTimeInstant
        elapsed = Duration.between(t0FromState(state).asInstant, now)
        timeoutOrNewStateAndSleep <-
          if (elapsed > timeout) {
            IO.pure(Left(TaskHasTimedOut(s"Timed out after $elapsed")))
          } else {
            computeSleep(state).map(Right.apply)
          }
      } yield timeoutOrNewStateAndSleep

    def computeSleep(state: S): IO[(S, Duration)]

  }

  final case class LinearBackoff(delay: Duration, timeout: Duration) extends RetryPolicy.WithTimeout[T0] {
    override def initialState: IO[T0]                     = Clock[IO].realTimeInstant.map(T0.apply)
    override def t0FromState(state: T0): T0               = state
    override def computeSleep(t0: T0): IO[(T0, Duration)] = IO.pure((t0, delay))
  }

  final case class ExponentialBackoff(
    initialDelay: Duration,
    factor: Double,
    timeout: Duration,
  ) extends RetryPolicy.WithTimeout[ExponentialBackoff.State] {
    override def initialState: IO[ExponentialBackoff.State] =
      Clock[IO].realTimeInstant.map(t0Instant => ExponentialBackoff.State(T0(t0Instant), initialDelay))
    override def t0FromState(state: ExponentialBackoff.State): T0 = state.t0
    override def computeSleep(state: ExponentialBackoff.State): IO[(ExponentialBackoff.State, Duration)] = {
      val nextDelay = Duration.ofMillis((state.nextDelay.toMillis * factor).toLong)

      IO.pure((state.copy(nextDelay = nextDelay), state.nextDelay))
    }
  }

  object ExponentialBackoff {
    final case class State(
      t0: T0,
      nextDelay: Duration,
    )
  }

}
