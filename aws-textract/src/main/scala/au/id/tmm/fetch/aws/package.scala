package au.id.tmm.fetch

import java.util.concurrent.{CompletableFuture, Executor}

import cats.effect.IO
import cats.effect.kernel.Async
import software.amazon.awssdk.core.client.config.{ClientAsyncConfiguration, SdkAdvancedAsyncClientOption}

package object aws {

  private[aws] def toIO[A](completableFuture: IO[CompletableFuture[A]]): IO[A] =
    Async[IO].fromCompletableFuture(completableFuture)

  private[aws] val makeClientAsyncConfiguration: IO[ClientAsyncConfiguration] =
    IO.executionContext.map {
      case executor: Executor =>
        ClientAsyncConfiguration
          .builder()
          .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executor)
          .build()
      case _ => ClientAsyncConfiguration.builder().build()
    }

}
