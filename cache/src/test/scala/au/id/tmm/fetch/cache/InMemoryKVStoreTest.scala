package au.id.tmm.fetch.cache

import cats.effect.{Resource, SyncIO}
import munit.CatsEffectSuite

class InMemoryKVStoreTest extends CatsEffectSuite {

  private val storeFixture: SyncIO[FunFixture[InMemoryKVStore.SimpleIO[String, String]]] = ResourceFixture {
    Resource.eval(InMemoryKVStore.SimpleIO[String, String])
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

}
