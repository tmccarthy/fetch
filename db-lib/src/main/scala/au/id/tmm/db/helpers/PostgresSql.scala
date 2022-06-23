package au.id.tmm.db.helpers

import au.id.tmm.db.statements.{Arguments, ColumnName, InsertStatement, Sql, TableName}
import au.id.tmm.db.syntax._

import scala.collection.immutable.ArraySeq

object PostgresSql {

  def upsert[A : Insertable](conflictColumn: ColumnName, otherConflictColumns: ColumnName*): InsertStatement[A] = {
    val conflictColumns: ArraySeq[ColumnName] = otherConflictColumns.to(ArraySeq).prepended(conflictColumn)

    val tableName: TableName                    = Insertable[A].tableName
    val columns: ArraySeq[Insertable.Column[A]] = Insertable[A].columns

    val conflictColumnsSet: Set[ColumnName] = conflictColumns.toSet

    val insertFields: Sql    = columns.map(_.name).joinWithCommas
    val valueParameters: Sql = ArraySeq.fill(columns.length)(Sql("?")).joinWithCommas
    val conflictFields: Sql  = conflictColumns.joinWithCommas
    val setExpressions: Sql = columns
      .filter(c => !conflictColumns.contains(c.name))
      .map { c =>
        sql"${c.name} = ?"
      }
      .joinWithCommas

    InsertStatement(
      sql"""INSERT INTO $tableName($insertFields) VALUES ($valueParameters) ON CONFLICT ($conflictFields) DO UPDATE SET $setExpressions;""",
      a => {
        val extractors =
          columns.map(_.extractor) ++ columns.filter(c => !conflictColumnsSet.contains(c.name)).map(_.extractor)

        Arguments(extractors.map(_.toArgument(a)))
      },
    )
  }

}
