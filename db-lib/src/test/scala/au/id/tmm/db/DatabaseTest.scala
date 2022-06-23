package au.id.tmm.db

import java.net.URI
import java.nio.file.Files

import au.id.tmm.db.DatabaseTest.{Person, makePersonTable}
import au.id.tmm.db.data.{DbId, NumRowsAffected}
import au.id.tmm.db.helpers.{Insertable, StandardSql}
import au.id.tmm.db.statements.ToArguments._
import au.id.tmm.db.statements.{ColumnName, TableName, UpdateStatement}
import au.id.tmm.db.syntax._
import cats.effect.kernel.Resource
import cats.effect.{IO, SyncIO}
import munit.CatsEffectSuite

import scala.collection.immutable.ArraySeq

class DatabaseTest extends CatsEffectSuite {

  private val dbFixture: SyncIO[FunFixture[Database]] = ResourceFixture {
    for {
      dbFilePath <- Resource.make(IO(Files.createTempDirectory(getClass.getSimpleName).resolve("test.db"))) { path =>
        for {
          _ <- IO(Files.deleteIfExists(path))
          _ <- IO(Files.deleteIfExists(path.getParent))
        } yield ()
      }
      db <- Database(
        new URI("jdbc:sqlite:" + dbFilePath.toString),
        "",
        "",
        Map.empty,
      )
    } yield db
  }

  dbFixture.test("ping") { db =>
    db.query[Int](sql"SELECT 1;".asQueryStatement)
      .assertEquals(ArraySeq(1))
  }

  dbFixture.test("update") { db =>
    for {
      _ <- db
        .update(makePersonTable)
        .assertEquals(NumRowsAffected(0))

      _ <- db
        .update(sql"""INSERT INTO Person VALUES ('Jane', 'Doe', 42);""".asUpdateStatement)
        .assertEquals(NumRowsAffected(1))
    } yield ()
  }

  dbFixture.test("bulk insert") { db =>
    for {
      _ <- db.update(makePersonTable)

      data = fs2.Stream
        .emits(
          List(
            ("jane", "doe", Some(42)),
            ("john", "smith", None),
          ),
        )
        .repeatN(5000)

      rowsUpdated <- db.batchInsert(
        sql"INSERT INTO Person VALUES (?, ?, ?)".asInsertStatement[(String, String, Option[Int])],
        data,
      )

      _ = assertEquals(rowsUpdated, NumRowsAffected(10_000L))
    } yield ()
  }

  dbFixture.test("big query") { db =>
    val numRows: Long = 10_123

    for {
      _ <- db.update(makePersonTable)

      data = fs2.Stream
        .emit("jane", "doe", Some(42))
        .repeatN(numRows)

      rowsUpdated <- db.batchInsert(
        sql"INSERT INTO Person VALUES (?, ?, ?)".asInsertStatement[(String, String, Option[Int])],
        data,
      )

      _ = assertEquals(rowsUpdated, NumRowsAffected(numRows))

      _ <- db
        .streamingQuery[(String, String, Option[Int])](sql"SELECT * FROM Person;".asQueryStatement)
        .compile
        .count
        .assertEquals(numRows)

    } yield ()
  }

  dbFixture.test("insert") { db =>
    for {
      _ <- db.update(makePersonTable)

      generatedId <- db.insert[Person](StandardSql.insertStatement)(
        Person("Jane", "Doe", 42),
      )

      _ = assertEquals(generatedId, ArraySeq(DbId(1)))
    } yield ()
  }

}

object DatabaseTest {

  private val makePersonTable: UpdateStatement =
    sql"CREATE TABLE Person(given_name VARCHAR, surname VARCHAR, age INT);".asUpdateStatement

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
