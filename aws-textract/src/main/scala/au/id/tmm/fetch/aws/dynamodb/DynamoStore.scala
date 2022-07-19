package au.id.tmm.fetch.aws.dynamodb

import java.time.Duration

import au.id.tmm.fetch.aws.dynamodb.DynamoStore.{dynamoKeyName, dynamoValueName}
import au.id.tmm.fetch.aws.{makeClientAsyncConfiguration, toIO}
import au.id.tmm.fetch.cache.KVStore
import au.id.tmm.fetch.retries.RetryEffect
import au.id.tmm.utilities.errors.GenericException
import cats.effect.{IO, Resource}
import io.circe.Json
import software.amazon.awssdk.services.dynamodb.model.TableStatus.CREATING
import software.amazon.awssdk.services.dynamodb.model._
import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbAsyncClientBuilder}

import scala.jdk.CollectionConverters.MapHasAsJava

final class DynamoStore private (
  client: DynamoDbAsyncClient,
  tableName: TableName,
) extends KVStore[IO, String, Json, Json] {
  override def get(k: String): IO[Option[Json]] = {
    val req = GetItemRequest
      .builder()
      .tableName(tableName.asString)
      .key(makeAttributeMap(key = k))
      .build()

    //noinspection SimplifyBooleanMatch
    for {
      response <- IO.fromCompletableFuture(IO(client.getItem(req)))
      value <-
        response.hasItem match {
          case false => IO.pure(None)
          case true =>
            Option(response.item().get(dynamoValueName)) match {
              case None => IO.raiseError(GenericException(s"No value for key $dynamoValueName"))
              case Some(attributeValue) =>
                Option(attributeValue.s()) match {
                  case None              => IO.raiseError(GenericException(s"Not a string attribute"))
                  case Some(stringValue) => IO.fromEither(io.circe.parser.parse(stringValue)).map(Some.apply)
                }
            }
        }
    } yield value
  }

  override def contains(k: String): IO[Boolean] = {
    val req = GetItemRequest
      .builder()
      .tableName(tableName.asString)
      .key(makeAttributeMap(key = k))
      .projectionExpression(dynamoKeyName)
      .build()

    for {
      response <- IO.fromCompletableFuture(IO(client.getItem(req)))
    } yield response.hasItem
  }

  override def put(k: String, v: Json): IO[Json] = {
    val req = PutItemRequest
      .builder()
      .tableName(tableName.asString)
      .item(makeAttributeMap(key = k, value = Some(v.noSpaces)))
      .build()

    for {
      _ <- IO.fromCompletableFuture(IO(client.putItem(req)))
    } yield v
  }

  override def drop(k: String): IO[Unit] = {
    val req = DeleteItemRequest
      .builder()
      .tableName(tableName.asString)
      .key(makeAttributeMap(key = k))
      .build()

    for {
      _ <- IO.fromCompletableFuture(IO(client.deleteItem(req)))
    } yield ()
  }

  private def makeAttributeMap(key: String, value: Option[String] = None): java.util.Map[String, AttributeValue] = {
    val map = Map.newBuilder[String, AttributeValue]

    map.addOne(dynamoKeyName -> AttributeValue.builder().s(key).build())

    value.foreach(v => map.addOne(dynamoValueName -> AttributeValue.builder().s(v).build()))

    map.result().asJava
  }

}

object DynamoStore {

  private val dynamoKeyName: String   = "entry_key"
  private val dynamoValueName: String = "entry_value"

  def apply(tableName: TableName): Resource[IO, DynamoStore] = apply(tableName, identity)

  /**
    * This is only here for testing
    */
  def apply(
    tableName: TableName,
    configureClient: DynamoDbAsyncClientBuilder => DynamoDbAsyncClientBuilder,
  ): Resource[IO, DynamoStore] =
    for {
      client <- dynamoClientResource(configureClient)
      _ <- Resource.liftK {
        for {
          tableExists <- tableExists(client, tableName)
          _           <- if (tableExists) IO.unit else makeTable(client, tableName)
        } yield ()
      }
    } yield new DynamoStore(client, tableName)

  private def dynamoClientResource(
    configureClient: DynamoDbAsyncClientBuilder => DynamoDbAsyncClientBuilder,
  ): Resource[IO, DynamoDbAsyncClient] =
    Resource.fromAutoCloseable {
      for {
        clientAsyncConfig <- makeClientAsyncConfiguration
      } yield {
        val clientBuilder = DynamoDbAsyncClient.builder()

        configureClient(clientBuilder)

        clientBuilder
          .asyncConfiguration(clientAsyncConfig)
          .build()
      }
    }

  private def tableExists(client: DynamoDbAsyncClient, tableName: TableName): IO[Boolean] =
    describeTable(client, tableName).attempt.flatMap {
      case Right(_)                           => IO.pure(true)
      case Left(_: ResourceNotFoundException) => IO.pure(false)
      case Left(e)                            => IO.raiseError(e)
    }

  private def makeTable(client: DynamoDbAsyncClient, tableName: TableName): IO[Unit] =
    for {
      _ <- createTable(client, tableName)
      _ <- waitForTableCreation(client, tableName)
    } yield ()

  private def createTable(client: DynamoDbAsyncClient, tableName: TableName): IO[Unit] = {
    val createTableRequest =
      CreateTableRequest
        .builder()
        .tableName(tableName.asString)
        .billingMode(BillingMode.PAY_PER_REQUEST)
        // TODO check these
        .attributeDefinitions(
          AttributeDefinition
            .builder()
            .attributeName(dynamoKeyName)
            .attributeType(ScalarAttributeType.S)
            .build(),
        )
        .keySchema(KeySchemaElement.builder().attributeName(dynamoKeyName).keyType(KeyType.HASH).build())
        .build()

    toIO(IO(client.createTable(createTableRequest))).as(())
  }

  private def waitForTableCreation(client: DynamoDbAsyncClient, tableName: TableName): IO[Unit] =
    RetryEffect.exponentialRetry(
      initialDelay = Duration.ofSeconds(10),
      factor = 1,
      maxWait = Duration.ofMinutes(1),
    ) {
      for {
        describeTableResponse <- describeTable(client, tableName)

        result <- Option(describeTableResponse.table).map(_.tableStatus) match {
          case Some(CREATING) => IO.raiseError(GenericException("Table still creating"))
          case Some(_)        => IO.pure(RetryEffect.Result.Finished(()))
          case None           => IO.pure(RetryEffect.Result.FailedFinished(GenericException("Table not created")))
        }
      } yield result
    }

  private def describeTable(client: DynamoDbAsyncClient, tableName: TableName): IO[DescribeTableResponse] = {
    val request = DescribeTableRequest.builder().tableName(tableName.asString).build()

    toIO(IO(client.describeTable(request)))
  }

}
