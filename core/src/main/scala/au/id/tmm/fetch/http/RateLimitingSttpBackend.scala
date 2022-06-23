package au.id.tmm.fetch.http

import au.id.tmm.utilities.errors.GenericException
import cats.Show
import cats.effect.kernel.GenTemporal
import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig, RateLimiterRegistry}
import sttp.capabilities
import sttp.client3.{DelegateSttpBackend, Request, Response, SttpBackend}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

final class RateLimitingSttpBackend[F[_], P, C : Show] private (
  delegate: SttpBackend[F, P],
  contextPerRequest: Request[_, _] => Option[C],
  policyPerContext: C => RateLimiterConfig,
)(implicit
  F: GenTemporal[F, Throwable],
) extends DelegateSttpBackend[F, P](delegate) {

  private val rateLimiterRegistry = RateLimiterRegistry.ofDefaults()

  private def rateLimiterFor(request: Request[_, _]): Option[RateLimiter] =
    for {
      context <- contextPerRequest(request)
      rateLimiterName = Show[C].show(context)
      rateLimiter     = rateLimiterRegistry.rateLimiter(rateLimiterName, () => policyPerContext(context))
    } yield rateLimiter

  override def send[T, R >: P with capabilities.Effect[F]](request: Request[T, R]): F[Response[T]] =
    rateLimiterFor(request) match {
      case Some(rateLimiter) =>
        rateLimiter.reservePermission() match {
          case failed if failed < 0 => F.raiseError(GenericException("Rate limiter timeout"))
          case 0                    => delegate.send(request)
          case timeToWait           => F.delayBy(delegate.send(request), FiniteDuration(timeToWait, NANOSECONDS))
        }
      case None => delegate.send(request)
    }

}

object RateLimitingSttpBackend {
  def apply[F[_], P, C : Show](
    delegate: SttpBackend[F, P],
    contextPerRequest: Request[_, _] => Option[C],
    policyPerContext: C => RateLimiterConfig,
  )(implicit
    F: GenTemporal[F, Throwable],
  ): RateLimitingSttpBackend[F, P, C] =
    new RateLimitingSttpBackend[F, P, C](delegate, contextPerRequest, policyPerContext)
}
