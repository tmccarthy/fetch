package au.id.tmm.db.data

import java.sql.ResultSet

import au.id.tmm.db.statements._
import au.id.tmm.db.syntax._

final case class WithDbId[A](
  id: DbId[A],
  row: A,
)

object WithDbId {
  def fromResultSet[A : FromResultSet](idColumnName: ColumnName): FromResultSet[WithDbId[A]] =
    (resultSet: ResultSet) =>
      WithDbId[A](
        DbId(resultSet.getColumn[Long](idColumnName)),
        FromResultSet[A].fromResultSetUnsafe(resultSet),
      )

  implicit def toArguments[A : ToArguments]: ToArguments[WithDbId[A]] = a => {
    Arguments(ToArguments[A].toArguments(a.row).asSeq.prepended(Argument(a.id)))
  }

  // TODO needs insertable instance

}
