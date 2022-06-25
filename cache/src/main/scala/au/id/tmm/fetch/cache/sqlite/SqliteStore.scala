package au.id.tmm.fetch.cache.sqlite

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import au.id.tmm.db.Database
import au.id.tmm.db.statements.{ColumnName, Sql, TableName, UpdateStatement}
import au.id.tmm.db.syntax.SqlStringContext
import au.id.tmm.fetch.cache.KVStore
import au.id.tmm.fetch.cache.sqlite.SqliteStore.{keyColumn, table, valColumn}
import au.id.tmm.fetch.files.Text
import au.id.tmm.utilities.errors.GenericException
import cats.effect.{IO, Resource}

import scala.collection.immutable.ArraySeq

class SqliteStore private (private val database: Database) extends KVStore[IO, String, String, String] {
  override def get(k: String): IO[Option[String]] =
    database.queryOneOrNoElement(sql"SELECT $valColumn FROM $table WHERE $keyColumn = $k".asQueryStatement)

  override def contains(k: String): IO[Boolean] =
    database
      .queryOneOrNoElement(sql"SELECT 1 FROM $table WHERE $keyColumn = $k".asQueryStatement[Int])
      .map(_.isDefined)

  override def put(k: String, v: String): IO[String] = database.inTransaction { session =>
    for {
      affectedRows <- session.insert(sql"""INSERT INTO $table($keyColumn, $valColumn)
             VALUES ($k, $v)
             ON CONFLICT($keyColumn) DO UPDATE
                 SET $valColumn = $v
             WHERE $keyColumn = $k""".asUpdateStatement)
      updatedId <- affectedRows.size match {
        case 1       => IO.pure(affectedRows.head)
        case badSize => IO.raiseError(GenericException(s"Expected exactly 1 updated row, but was $badSize"))
      }
      // TODO for some reason this doesn't work when looking up by the id column
      valueInDb <- session.queryOnlyElement[String](
        sql"SELECT $valColumn FROM $table WHERE $keyColumn = $k".asQueryStatement,
      )
    } yield valueInDb
  }

  override def drop(k: String): IO[Unit] =
    database.update(sql"DELETE FROM $table WHERE $keyColumn = $k".asUpdateStatement).as(())

}

object SqliteStore {
  private val valColumn = ColumnName("val")
  private val keyColumn = ColumnName("key")
  private val table     = TableName("kv_store")

  def at(path: Path): Resource[IO, SqliteStore] =
    for {
      isExistingStore <- Resource.liftK(isExistingStore(path))
      db              <- databaseAt(path)
      _ <- Resource.liftK {
        if (isExistingStore) sanityCheck(db) else runSetupScript(db)
      }
    } yield new SqliteStore(db)

  private def databaseAt(path: Path): Resource[IO, Database] = Database(
    new URI("jdbc:sqlite:" + path.toString),
    username = "",
    password = "",
    dataSourceProperties = Map.empty,
  )

  private def runSetupScript(database: Database): IO[Unit] =
    for {
      rawSql <- Text.string(IO(getClass.getResourceAsStream("sqlite_store_setup.sql")), StandardCharsets.UTF_8)
      updateStatement = UpdateStatement(Sql(rawSql))
      _ <- database.update(updateStatement)
    } yield ()

  private def sanityCheck(database: Database): IO[Unit] =
    for {
      tableTuple <- database.query[(String, TableName)](
        sql"SELECT `type`, name FROM sqlite_schema WHERE type = 'table' AND name = ${table.asString}".asQueryStatement,
      )
      _ <- IO.raiseUnless(tableTuple == ArraySeq(("table", table)))(GenericException("Failed sanity check"))
    } yield ()

  private def isExistingStore(path: Path): IO[Boolean] = IO(Files.exists(path))

}
