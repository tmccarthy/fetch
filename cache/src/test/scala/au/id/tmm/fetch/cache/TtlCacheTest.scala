package au.id.tmm.fetch.cache

import java.time.{Duration, Period}

import cats.Monad
import cats.effect.IO
import munit.CatsEffectSuite

class TtlCacheTest extends CatsEffectSuite {

  private val makeStore: IO[KVStoreTrackingCacheMisses[KeyForTests, Cache.WithTtl.Timestamped[ValueForTests]]] =
    for {
      baseStore                <- InMemoryKVStore.SimpleIO[KeyForTests, Cache.WithTtl.Timestamped[ValueForTests]]
      storeTrackingCacheMisses <- KVStoreTrackingCacheMisses(baseStore)
    } yield storeTrackingCacheMisses

  test("get expired") {
    for {
      store <- makeStore
      clock <- DummyClock.atEpoch[IO]
      cache = Cache.WithTtl(Duration.ofHours(1), store)(Monad[IO], clock)

      _ <- cache.get(KeyForTests.ResourceA)(IO.pure(ValueForTests.ValueA))
      _ <- assertIO(store.popPuts, List(KeyForTests.ResourceA))

      _ <- cache.get(KeyForTests.ResourceA)(IO.pure(ValueForTests.ValueA))
      _ <- assertIO(store.popPuts, List.empty)

      _ <- clock.increment(Period.ofYears(1))

      _ <- cache.get(KeyForTests.ResourceA)(IO.pure(ValueForTests.ValueA))
      _ <- assertIO(store.popPuts, List(KeyForTests.ResourceA))
    } yield ()
  }

}

object TtlCacheTest {}
