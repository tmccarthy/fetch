package au.id.tmm.fetch.files

import cats.effect.{IO, Resource}

private[fetch] object Closeables {

  def resourceFrom[A <: AutoCloseable](closeable: IO[A]): Resource[IO, A] =
    Resource.make(closeable)(is => IO(is.close()))

}
