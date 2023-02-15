package au.id.tmm.fetch.cache

import java.nio.file.{Files, Path, Paths}

import au.id.tmm.fetch.cache.LocalFsCacheTest.{KeyForTest, TestClient, ValueForTest}
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.effect.{IO, Resource}
import munit.CatsEffectSuite
import org.apache.commons.io.FileUtils

import scala.collection.mutable

class LocalFsCacheTest extends CatsEffectSuite {

  private val client: TestClient = new TestClient()

  private val emptyDirectory: Resource[IO, Path] =
    Resource.make {
      IO(Files.createTempDirectory(getClass.getSimpleName))
    } { directory =>
      IO(FileUtils.forceDelete(directory.toFile))
    }

  private val localFsStoreResource: Resource[IO, KVStore.SimpleIO[Path, String]] =
    emptyDirectory.evalMap(Stores.localFsStringStore)

  private val cacheResource: Resource[IO, Cache.SimpleIO[KeyForTest, ValueForTest]] = localFsStoreResource
    .map { fsStore =>
      fsStore
        .evalContramapKey[KeyForTest](k => IO(Paths.get(k.asString + ".txt")))
        .contramapValueIn[ValueForTest](v => v.asString)
        .evalMapValueOut[ValueForTest](s => IO.fromEither(ValueForTest.parse(s)))
    }
    .map(Cache.SimpleIO[KeyForTest, ValueForTest])

  test("first get from cache") {
    cacheResource.use { cache =>
      for {
        obtainedValue <- cache.get(KeyForTest.ResourceA)(IO(client.retrieve(KeyForTest.ResourceA)))
        _ = assertEquals(obtainedValue, ValueForTest.ValueA)
        _ <- assertIO(IO(client.retrieveBuffer.toList), List(KeyForTest.ResourceA))
      } yield ()
    }
  }

  test("second get from cache") {
    cacheResource.use { cache =>
      for {
        _ <- cache.get(KeyForTest.ResourceA)(IO(client.retrieve(KeyForTest.ResourceA)))
        _ <- IO(client.retrieveBuffer.clear())

        _ <- cache.get(KeyForTest.ResourceA)(IO(client.retrieve(KeyForTest.ResourceA)))
      } yield assertEquals(client.retrieveBuffer.toList, List.empty)
    }
  }

}

object LocalFsCacheTest {

  private sealed abstract class KeyForTest(val asString: String)

  private object KeyForTest {
    case object ResourceA extends KeyForTest("ResourceA")
    case object ResourceB extends KeyForTest("ResourceB")
  }

  private sealed abstract class ValueForTest(val asString: String)

  private object ValueForTest {
    def parse(asString: String): ExceptionOr[ValueForTest] = asString match {
      case ValueA.asString => Right(ValueA)
      case ValueB.asString => Right(ValueB)
      case _               => Left(GenericException(asString))
    }

    case object ValueA extends ValueForTest("ValueA")
    case object ValueB extends ValueForTest("ValueB")
  }

  private final class TestClient {
    val retrieveBuffer: mutable.Buffer[KeyForTest] = mutable.Buffer()

    def retrieve(key: KeyForTest): ValueForTest = {
      retrieveBuffer.append(key)

      key match {
        case KeyForTest.ResourceA => ValueForTest.ValueA
        case KeyForTest.ResourceB => ValueForTest.ValueB
      }
    }
  }

}
