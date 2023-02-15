package au.id.tmm.fetch.cache

import java.nio.file.{Files, Path, Paths}

import cats.effect.{IO, Resource}
import munit.CatsEffectSuite
import org.apache.commons.io.FileUtils

class LocalFsCacheTest extends CatsEffectSuite {

  private val client: TestClient = new TestClient()

  private val emptyDirectory: Resource[IO, Path] =
    Resource.make {
      IO(Files.createTempDirectory(getClass.getSimpleName))
    } { directory =>
      IO(FileUtils.forceDelete(directory.toFile))
    }

  private val storeResource: Resource[IO, KVStoreTrackingCacheMisses[KeyForTests, ValueForTests]] =
    emptyDirectory
      .evalMap(Stores.localFsStringStore)
      .map { fsStore =>
        fsStore
          .evalContramapKey[KeyForTests](k => IO(Paths.get(k.asString + ".txt")))
          .contramapValueIn[ValueForTests](v => v.asString)
          .evalMapValueOut[ValueForTests](s => IO.fromEither(ValueForTests.parse(s)))
      }
      .evalMap(KVStoreTrackingCacheMisses.apply[KeyForTests, ValueForTests])

  test("first get from cache") {
    storeResource.use { store =>
      val cache = Cache(store)

      for {
        obtainedValue <- cache.get(KeyForTests.ResourceA)(IO(client.retrieve(KeyForTests.ResourceA)))
        _ = assertEquals(obtainedValue, ValueForTests.ValueA)
        _ <- assertIO(store.popPuts, List(KeyForTests.ResourceA))
      } yield ()
    }
  }

  test("second get from cache") {
    storeResource.use { store =>
      val cache = Cache(store)

      for {
        _ <- cache.get(KeyForTests.ResourceA)(IO(client.retrieve(KeyForTests.ResourceA)))
        _ <- store.popPuts

        _ <- cache.get(KeyForTests.ResourceA)(IO(client.retrieve(KeyForTests.ResourceA)))
        _ <- assertIO(store.popPuts, List.empty)
      } yield ()
    }
  }

}
