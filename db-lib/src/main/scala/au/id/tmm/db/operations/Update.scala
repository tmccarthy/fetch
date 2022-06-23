package au.id.tmm.db.operations

import au.id.tmm.db.Session
import au.id.tmm.db.statements.UpdateStatement
import au.id.tmm.db.data.NumRowsAffected
import cats.effect.IO

final class Update private (updateStatement: UpdateStatement) extends DatabaseOp.UsingIO[NumRowsAffected] {
  override def runIn(session: Session): IO[NumRowsAffected] =
    PreparedStatementUtils
      .psFrom(session, updateStatement.parameterisedSql)
      .use { ps =>
        for {
          numRowsAffected <- IO {
            updateStatement.parameterisedSql.arguments.bindToUnsafe(ps)
            NumRowsAffected(ps.executeUpdate())
          }
        } yield numRowsAffected
      }
}

object Update {
  def apply(updateStatement: UpdateStatement): Update = new Update(updateStatement)
}
