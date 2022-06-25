package au.id.tmm.fetch.cache.sqlite

import java.nio.file.{Files, Path}

import cats.effect.kernel.Resource
import cats.effect.{IO, SyncIO}
import munit.CatsEffectSuite

class SqliteStoreTest extends CatsEffectSuite {

  private val dbFilePath: Resource[IO, Path] =
    Resource.make(IO(Files.createTempDirectory(getClass.getSimpleName).resolve("test.db"))) { path =>
      for {
        _ <- IO(Files.deleteIfExists(path))
        _ <- IO(Files.deleteIfExists(path.getParent))
      } yield ()
    }

  private val storeFixture: SyncIO[FunFixture[SqliteStore]] = ResourceFixture {
    for {
      dbFilePath <- dbFilePath
      store <- SqliteStore.at(dbFilePath)
    } yield store
  }

  storeFixture.test("empty get") { store =>
    assertIO(store.get("key"), None)
  }

  storeFixture.test("empty contains") { store =>
    assertIO(store.contains("key"), false)
  }

  storeFixture.test("put get") { store =>
    for {
      _        <- store.put("key", "value")
      obtained <- store.get("key")
    } yield assertEquals(obtained, Some("value"))
  }

  storeFixture.test("put contains") { store =>
    for {
      _        <- store.put("key", "value")
      obtained <- store.contains("key")
    } yield assertEquals(obtained, true)
  }

  storeFixture.test("put overwrite get") { store =>
    for {
      _        <- store.put("key", "value1")
      _        <- store.put("key", "value2")
      obtained <- store.get("key")
    } yield assertEquals(obtained, Some("value2"))
  }

  storeFixture.test("put drop get") { store =>
    for {
      _        <- store.put("key", "value1")
      _        <- store.drop("key")
      obtained <- store.get("key")
    } yield assertEquals(obtained, None)
  }

  storeFixture.test("empty drop") { store =>
    for {
      _        <- store.drop("key")
      obtained <- store.get("key")
    } yield assertEquals(obtained, None)
  }

  ResourceFixture(dbFilePath).test("put close reopen") { dbFilePath =>
    for {
      _ <- SqliteStore.at(dbFilePath).use { store =>
        store.put("key", "value")
      }
      obtained <- SqliteStore.at(dbFilePath).use { store =>
        store.get("key")
      }
    } yield assertEquals(obtained, Some("value"))
  }

  ResourceFixture(dbFilePath).test("open store on directory") { dbFilePath =>
    SqliteStore.at(dbFilePath.getParent).use_.attempt.map { attempted =>
      assert(attempted.isLeft)
    }
  }

  ResourceFixture(dbFilePath).test("open store on existing file") { dbFilePath =>
    for {
      _ <- IO(Files.writeString(dbFilePath, "test"))
      attempted <- SqliteStore.at(dbFilePath)
        .use_
        .attempt
    } yield assert(attempted.isLeft)
  }

}
