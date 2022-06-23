package au.id.tmm.db.operations

import au.id.tmm.db.Session
import au.id.tmm.db.statements.QueryStatement
import cats.effect.IO

import scala.collection.immutable.ArraySeq

final class Query[A] private (queryStatement: QueryStatement[A]) extends DatabaseOp.UsingIO[ArraySeq[A]] {
  override def runIn(session: Session): IO[ArraySeq[A]] =
    PreparedStatementUtils
      .psFrom(session, queryStatement.sql)
      .use { ps =>
        for {
          resultSet <- IO {
            queryStatement.sql.arguments.bindToUnsafe(ps)
            ps.executeQuery()
          }
          result <- ResultSetUtils.readResultSet(queryStatement.fromResultSet, resultSet)
        } yield result
      }
}

object Query {
  def apply[A](queryStatement: QueryStatement[A]): Query[A] = new Query(queryStatement)
}
