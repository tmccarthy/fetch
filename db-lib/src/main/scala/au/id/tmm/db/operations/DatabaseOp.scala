package au.id.tmm.db.operations

import au.id.tmm.db.{Database, Session}
import cats.effect.IO

trait DatabaseOp[F[_], A] {

  def runIn(session: Session): F[A]

  def runOn(database: Database): F[A]

}

object DatabaseOp {
  trait UsingIO[A] extends DatabaseOp[IO, A] {
    override def runOn(database: Database): IO[A] = database.inTransaction(runIn)
  }

  def apply[A](f: Session => IO[A]): DatabaseOp.UsingIO[A] = session => f(session)
}
