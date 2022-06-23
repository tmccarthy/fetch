package au.id.tmm.fetch

import java.util.concurrent.{CompletableFuture, CompletionException}

import cats.effect.IO

package object aws {

  private[aws] def toIO[A](completableFuture: CompletableFuture[A]): IO[A] =
//    IO.cancelable[A] { cb: (Either[Throwable, A] => Unit) =>
//      completableFuture.handle[Unit] { (a: A, e: Throwable) =>
//        (a, e) match {
//          case (null, e: CancellationException) => ()
//          case (null, e: CompletionException)   => cb(Left(e.getCause))
//          case (null, e)                        => cb(Left(e))
//          case (a, _)                           => cb(Right(a))
//        }
//      }
//
//      IO(completableFuture.cancel(true))
//    }
    IO.async[A] { cb: (Either[Throwable, A] => Unit) =>
      completableFuture.handle[Unit] { (a: A, e: Throwable) =>
        (a, e) match {
          case (null, e: CompletionException) => cb(Left(e.getCause))
          case (null, e)                      => cb(Left(e))
          case (a, _)                         => cb(Right(a))
        }
      }
    }

}
