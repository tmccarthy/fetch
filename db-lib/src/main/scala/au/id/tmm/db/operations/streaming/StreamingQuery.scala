package au.id.tmm.db.operations.streaming

import au.id.tmm.db.Session
import au.id.tmm.db.operations.PreparedStatementUtils
import au.id.tmm.db.statements.QueryStatement
import cats.effect.IO

final class StreamingQuery[A] private (queryStatement: QueryStatement[A]) extends StreamingDatabaseOp[A] {
  override def runIn(session: Session): fs2.Stream[IO, A] =
    for {
      ps <- fs2.Stream.resource(PreparedStatementUtils.psFrom(session, queryStatement.sql))
      resultSet <- fs2.Stream.eval(IO {
        queryStatement.sql.arguments.bindToUnsafe(ps)
        ps.executeQuery()
      })
      a <- StreamingResultSetUtils.streamResultSet[A](queryStatement.fromResultSet, resultSet)
    } yield a
}

object StreamingQuery {
  def apply[A](queryStatement: QueryStatement[A]): StreamingQuery[A] = new StreamingQuery(queryStatement)
}
