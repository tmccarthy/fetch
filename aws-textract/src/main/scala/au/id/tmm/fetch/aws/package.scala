package au.id.tmm.fetch

import java.util.concurrent.CompletableFuture

import cats.effect.IO
import cats.effect.kernel.Async

package object aws {

  private[aws] def toIO[A](completableFuture: IO[CompletableFuture[A]]): IO[A] =
    Async[IO].fromCompletableFuture(completableFuture)

}
