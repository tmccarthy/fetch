package au.id.tmm.db.helpers

import au.id.tmm.db.helpers.PostgresHelperTest.Person
import au.id.tmm.db.statements.{ColumnName, TableName}
import au.id.tmm.db.syntax.SqlStringContext
import munit.FunSuite

class PostgresHelperTest extends FunSuite {

  test("upsert statement") {
    val updateStatement = PostgresSql.upsert[Person](ColumnName("id")).toUpdateStatement(Person("Jane", "Doe", 42))

    val expected =
      sql"""INSERT INTO Person(given_name, surname, age) VALUES (${"Jane"}, ${"Doe"}, ${42}) ON CONFLICT (id) DO UPDATE SET given_name = ${"Jane"}, surname = ${"Doe"}, age = ${42};"""

    assertEquals(updateStatement.parameterisedSql, expected)
  }

}

object PostgresHelperTest {
  final case class Person(
    givenName: String,
    surname: String,
    age: Int,
  )

  object Person {
    implicit val insertable: Insertable[Person] = Insertable[Person](
      TableName("Person"),
      Insertable.Column[Person, String](ColumnName("given_name"), p => p.givenName),
      Insertable.Column[Person, String](ColumnName("surname"), p => p.surname),
      Insertable.Column[Person, Int](ColumnName("age"), p => p.age),
    )
  }

}
