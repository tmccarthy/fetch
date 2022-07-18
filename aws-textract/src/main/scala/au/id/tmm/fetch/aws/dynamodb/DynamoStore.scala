package au.id.tmm.fetch.aws.dynamodb

import java.net.URI
import java.time.Duration

import au.id.tmm.fetch.aws.{makeClientAsyncConfiguration, toIO}
import au.id.tmm.fetch.cache.KVStore
import au.id.tmm.fetch.retries.RetryEffect
import au.id.tmm.utilities.errors.GenericException
import cats.effect.{IO, Resource}
import io.circe.Json
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.TableStatus.CREATING
import software.amazon.awssdk.services.dynamodb.model._

final class DynamoStore private (
  client: DynamoDbAsyncClient,
  tableName: TableName,
) extends KVStore[IO, String, Json, Json] {
  override def get(k: String): IO[Option[Json]]  = ???
  override def contains(k: String): IO[Boolean]  = ???
  override def put(k: String, v: Json): IO[Json] = ???
  override def drop(k: String): IO[Unit]         = ???
}

object DynamoStore {

  def apply(tableName: TableName): Resource[IO, DynamoStore] = apply(tableName, None)

  /**
    * This is only here for testing
    */
  def apply(tableName: TableName, overrideDynamoEndpoint: Option[URI]): Resource[IO, DynamoStore] =
    for {
      client <- dynamoClientResource(overrideDynamoEndpoint)
      _ <- Resource.liftK {
        for {
          tableExists <- tableExists(client, tableName)
          _           <- if (tableExists) IO.unit else makeTable(client, tableName)
        } yield ()
      }
    } yield new DynamoStore(client, tableName)

  private def dynamoClientResource(overrideDynamoEndpoint: Option[URI]): Resource[IO, DynamoDbAsyncClient] =
    Resource.fromAutoCloseable {
      for {
        clientAsyncConfig <- makeClientAsyncConfiguration
      } yield {
        val clientBuilder = DynamoDbAsyncClient.builder()

        overrideDynamoEndpoint.foreach(clientBuilder.endpointOverride)

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
            .attributeName("key")
            .attributeType(ScalarAttributeType.S)
            .build(),
        )
        .keySchema(KeySchemaElement.builder().attributeName("key").keyType(KeyType.HASH).build())
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
