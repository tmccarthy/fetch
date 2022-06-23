package au.id.tmm.db.statements

import au.id.tmm.db.syntax._
import munit.FunSuite

class SqlTest extends FunSuite {

  test("empty") {
    assertEquals(sql"", Sql(""))
  }

  test("embed argument") {
    assertEquals(sql"SELECT ${1}", Sql("SELECT ?", Arguments.of(Argument(1))))
  }

  test("embed symbol") {
    val table      = TableName("the_table")
    val columnName = ColumnName("id")

    assertEquals(sql"SELECT $columnName FROM $table", Sql("SELECT id FROM the_table"))
  }

  test("embed column on table") {
    val tableColumn = ColumnOnTable(TableName("the_table"), ColumnName("id"))
    assertEquals(sql"SELECT $tableColumn FROM ${tableColumn.table}", Sql("SELECT the_table.id FROM the_table"))
  }

  // TODO probably need an alias type?

  test("embed another fragment") {
    val trueFragment = sql"1 = 1"
    val idFragment   = sql"id = ${5}"

    assertEquals(
      sql"SELECT * FROM table WHERE TRUE AND $trueFragment AND $idFragment",
      sql"SELECT * FROM table WHERE TRUE AND 1 = 1 AND id = ${5}",
    )
  }

  test("embed another fragment complex") {
    val selectFragment = sql"${5} + id AS id_plus_five"
    val scoreFragment  = sql"score > ${95}"

    assertEquals(
      sql"SELECT $selectFragment, * FROM table WHERE $scoreFragment",
      sql"SELECT ${5} + id AS id_plus_five, * FROM table WHERE score > ${95}",
    )
  }

  // TODO need a solution to this
  test("merge fragments with unbound argument".fail) {
    val selectFragment = sql"? + id AS id_plus_five"
    val scoreFragment  = sql"score > ${95}"

    assertEquals(
      sql"SELECT $selectFragment, * FROM table WHERE $scoreFragment",
      Sql(
        "SELECT ? + id AS id_plus_five, * FROM table WHERE score > ?",
        Arguments.of(Argument(null.asInstanceOf[String]), Argument(95)),
      ),
    )
  }

  test("mkSql") {
    val joined = List(
      sql"SELECT * FROM table_1",
      sql"SELECT * FROM table_2",
      sql"SELECT * FROM table_3",
    ).mkSql(sql"(", sql") UNION (", sql")")

    val expected = sql"(SELECT * FROM table_1) UNION (SELECT * FROM table_2) UNION (SELECT * FROM table_3)"

    assertEquals(joined, expected)
  }

  test("mkSql empty") {
    assertEquals(List.empty[Sql].mkSql(sql"(", sql",", sql")"), sql"()")
  }

}
