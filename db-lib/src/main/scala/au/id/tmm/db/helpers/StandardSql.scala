package au.id.tmm.db.helpers

import au.id.tmm.db.statements.{Arguments, InsertStatement, TableName, ToArguments}
import au.id.tmm.db.syntax._

import scala.collection.immutable.ArraySeq

object StandardSql {

  def insertStatement[A : Insertable]: InsertStatement[A] = {
    val tableName: TableName                    = Insertable[A].tableName
    val columns: ArraySeq[Insertable.Column[A]] = Insertable[A].columns

    val sql = sql"INSERT INTO $tableName (" +
      columns.map(_.name).joinWithCommas +
      sql") VALUES (" +
      ArraySeq.fill(columns.length)(sql"?").joinWithCommas +
      sql");"

    val toArguments: ToArguments[A] = a => Arguments(columns.map(c => c.extractor.toArgument(a)))

    InsertStatement(sql, toArguments)
  }

}
