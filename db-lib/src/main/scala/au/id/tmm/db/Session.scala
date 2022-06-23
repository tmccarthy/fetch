package au.id.tmm.db

import java.sql.Connection

import au.id.tmm.db.operations._
import cats.effect.IO

class Session private[db] (private[db] val connection: Connection) extends FluentDbOps {

  def run[F[_], A](dbOp: DatabaseOp[F, A]): F[A] = dbOp.runIn(this)

  val rollback: IO[Unit] = IO(connection.rollback())

  val commit: IO[Unit] = IO(connection.commit())

}

object Session {

  sealed abstract class TransactionIsolation(private[db] val jdbcFlag: Int)

  object TransactionIsolation {
    case object ReadUncommitted extends TransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED)
    case object ReadCommitted   extends TransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    case object RepeatableRead  extends TransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
    case object Serializable    extends TransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
  }

  sealed abstract class CommitBehaviour

  object CommitBehaviour {
    case object AutoCommit               extends CommitBehaviour
    case object CommitAtEndOfTransaction extends CommitBehaviour
  }

  sealed abstract class ReadMode

  object ReadMode {
    case object ReadOnly  extends ReadMode
    case object ReadWrite extends ReadMode
  }

}
