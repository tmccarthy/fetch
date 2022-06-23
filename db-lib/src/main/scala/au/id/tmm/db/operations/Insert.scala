package au.id.tmm.db.operations

import au.id.tmm.db.Session
import au.id.tmm.db.data.DbId
import au.id.tmm.db.statements.{FromResultSet, InsertStatement, UpdateStatement}
import cats.effect.IO

import scala.collection.immutable.ArraySeq

/**
  * Like [[Update]], but returns any generated DB ids in a way that might be useful when running `INSERT` statements.
  * Note that an `INSERT` may return an empty list of [[DbId]] values if there's an `ON CONFLICT DO NOTHING` clause.
  */
// TODO really unsure on the modelling here. Should probably either return an Option[DbId[_]] or carry a List[A] and
//  insert them all
final class Insert[A] private (updateStatement: UpdateStatement) extends DatabaseOp.UsingIO[ArraySeq[DbId[A]]] {
  override def runIn(session: Session): IO[ArraySeq[DbId[A]]] =
    PreparedStatementUtils
      .psReturningGeneratedKeysFrom(session, updateStatement.parameterisedSql)
      .use { ps =>
        for {
          generatedKeysResultSet <- IO {
            updateStatement.parameterisedSql.arguments.bindToUnsafe(ps)
            ps.executeUpdate()
            ps.getGeneratedKeys
          }
          generatedKeys <- ResultSetUtils.readResultSet(FromResultSet.forSingleColumn[DbId[A]], generatedKeysResultSet)
        } yield generatedKeys
      }
}

object Insert {
  def apply[A](updateStatement: UpdateStatement): Insert[A] = new Insert(updateStatement)

  def using[A](insertStatement: InsertStatement[A])(a: A) = new Insert[A](insertStatement.toUpdateStatement(a))
}
