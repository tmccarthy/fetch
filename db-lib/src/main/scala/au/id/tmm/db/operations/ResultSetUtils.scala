package au.id.tmm.db.operations

import java.sql.ResultSet

import au.id.tmm.db.statements.FromResultSet
import cats.effect.IO

import scala.collection.immutable.ArraySeq

private[operations] object ResultSetUtils {

  def readResultSet[A](fromResultSet: FromResultSet[A], resultSet: ResultSet): IO[ArraySeq[A]] = IO {
    val builder = ArraySeq.untagged.newBuilder[A]

    while (resultSet.next()) {
      builder.addOne(fromResultSet.fromResultSetUnsafe(resultSet))
    }

    builder.result()
  }

}
