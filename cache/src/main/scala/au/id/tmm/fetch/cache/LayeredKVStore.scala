package au.id.tmm.fetch.cache

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits.toTraverseOps
import cats.syntax.flatMap._
import cats.syntax.functor._

// TODO tests
final case class LayeredKVStore[F[_] : Monad, K, V_IN, V_OUT](
  refeed: V_OUT => V_IN,
  underlying: NonEmptyList[KVStore[F, K, V_IN, V_OUT]],
) extends KVStore[F, K, V_IN, V_OUT] {

  private val F: Monad[F] = Monad[F]

  override def get(k: K): F[Option[V_OUT]] = {
    def go(
      storesMissingValue: List[KVStore[F, K, V_IN, V_OUT]],
      remainingStoresToCheck: List[KVStore[F, K, V_IN, V_OUT]],
    ): F[Option[V_OUT]] =
      remainingStoresToCheck match {
        case Nil => F.pure(None)
        case thisStore :: nextRemainingStoresToCheck =>
          for {
            maybeV <- thisStore.get(k)
            result <- maybeV match {
              case None => go(storesMissingValue :+ thisStore, nextRemainingStoresToCheck)
              case Some(vOut) =>
                for {
                  vIn <- F.pure(refeed(vOut))
                  _   <- storesMissingValue.traverse(_.put(k, vIn))
                } yield Some(vOut)
            }
          } yield result
      }

    go(storesMissingValue = Nil, remainingStoresToCheck = underlying.toList)
  }

  override def contains(k: K): F[Boolean] = get(k).map(_.isDefined)

  override def put(k: K, v: V_IN): F[V_OUT] =
    underlying
      .traverse { store =>
        store.put(k, v)
      }
      .map(_.head)

  override def drop(k: K): F[Unit] =
    underlying
      .traverse { store =>
        store.drop(k)
      }
      .map(_.head)

}
