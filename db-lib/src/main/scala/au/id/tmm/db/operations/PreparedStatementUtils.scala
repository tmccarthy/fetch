package au.id.tmm.db.operations

import java.sql.{PreparedStatement, SQLException, Statement}

import au.id.tmm.db.Session
import au.id.tmm.db.statements.Sql
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.applicativeError._

private[operations] object PreparedStatementUtils {

  def psFrom(session: Session, sql: Sql): Resource[IO, PreparedStatement] =
    toResource(wrapErrorsWithSql(IO(session.connection.prepareStatement(sql.raw)), sql))

  def psReturningGeneratedKeysFrom(session: Session, sql: Sql): Resource[IO, PreparedStatement] =
    toResource(
      wrapErrorsWithSql(IO(session.connection.prepareStatement(sql.raw, Statement.RETURN_GENERATED_KEYS)), sql),
    )

  private def wrapErrorsWithSql[A](io: IO[A], sql: Sql): IO[A] =
    io.adaptErr(e => new SQLException(s"Query: ${sql.raw.trim}", e))

  private def toResource(preparedStatement: IO[PreparedStatement]): Resource[IO, PreparedStatement] =
    Resource.make(preparedStatement)(ps => IO(ps.close()))

}
