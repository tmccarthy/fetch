package au.id.tmm.fetch.aws.dynamodb

import cats.effect.IO
import io.circe.Json
import io.circe.syntax.EncoderOps
import munit.CatsEffectSuite
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}

class DynamoDbStoreTest extends CatsEffectSuite {

  private val storeFixture: Fixture[DynamoStore] = ResourceSuiteLocalFixture(
    "dynamo-db-store",
    DynamoDbDockerTest.localDynamoDbUri.flatMap { uri =>
      DynamoStore(
        TableName("test-table"),
        configureClient = clientBuilder =>
          clientBuilder
            .endpointOverride(uri)
            .credentialsProvider(DynamoDbDockerTest.testCredentials)
            .region(DynamoDbDockerTest.testRegion),
      )
    },
  )

  override def munitFixtures: Seq[Fixture[_]] = List(storeFixture)

  private def makeJsonValue(id: Int): Json = Json.obj("field" -> id.asJson)

  test("empty get") {
    for {
      store <- IO(storeFixture())
      v     <- store.get("key")
    } yield assertEquals(v, None)
  }

  test("empty contains") {
    for {
      store <- IO(storeFixture())
      v     <- store.contains("key")
    } yield assertEquals(v, false)
  }

  test("put get") {
    for {
      store    <- IO(storeFixture())
      _        <- store.put("key", makeJsonValue(0))
      obtained <- store.get("key")
    } yield assertEquals(obtained, Some(makeJsonValue(0)))
  }

  test("put contains") {
    for {
      store    <- IO(storeFixture())
      _        <- store.put("key", makeJsonValue(0))
      obtained <- store.contains("key")
    } yield assertEquals(obtained, true)
  }

  test("put overwrite get") {
    for {
      store    <- IO(storeFixture())
      _        <- store.put("key", makeJsonValue(1))
      _        <- store.put("key", makeJsonValue(2))
      obtained <- store.get("key")
    } yield assertEquals(obtained, Some(makeJsonValue(2)))
  }

  test("put drop get") {
    for {
      store    <- IO(storeFixture())
      _        <- store.put("key", makeJsonValue(1))
      _        <- store.drop("key")
      obtained <- store.get("key")
    } yield assertEquals(obtained, None)
  }

  test("empty drop") {
    for {
      store    <- IO(storeFixture())
      _        <- store.drop("key")
      obtained <- store.get("key")
    } yield assertEquals(obtained, None)
  }

}
