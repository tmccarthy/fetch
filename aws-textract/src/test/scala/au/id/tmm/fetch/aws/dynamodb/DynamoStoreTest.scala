package au.id.tmm.fetch.aws.dynamodb

import java.net.{ServerSocket, URI}

import au.id.tmm.fetch.aws.dynamodb.DynamoStoreTest.{LocalDynamoServerConfig, localDynamoServerResource}
import cats.effect.{IO, Resource, SyncIO}
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner
import munit.CatsEffectSuite

class DynamoStoreTest extends CatsEffectSuite {

  private val dynamoServer: Fixture[LocalDynamoServerConfig] = ResourceSuiteLocalFixture("local-dynamodb", localDynamoServerResource)

  override def munitFixtures: Seq[Fixture[_]] = List(dynamoServer)

  private val dynamoStore: SyncIO[FunFixture[DynamoStore]] = ResourceFixture {
    Resource.liftK(IO.realTimeInstant.map(_.getNano)).flatMap { seed =>
      DynamoStore(
        tableName = TableName(s"table_$seed"),
        overrideDynamoEndpoint = Some(new URI(s"http://localhost:${dynamoServer().port}")),
      )
    }
  }

  dynamoStore.test("empty get") { store =>
    assertIO(store.get("key"), None)
  }

}

object DynamoStoreTest {

  // https://stackoverflow.com/a/37780083/1951001

  private val findAvailablePort: IO[Int] =
    Resource.fromAutoCloseable(IO(new ServerSocket(0)))
      .use(s => IO(s.getLocalPort))

  private val localDynamoServerResource: Resource[IO, LocalDynamoServerConfig] =
    Resource.make(
      acquire =
        for {
          availablePort <- findAvailablePort
          server <- IO(ServerRunner.createServerFromCommandLineArgs(Array("-inMemory", "-port", availablePort.toString)))
          _ <- IO(server.start())
        } yield (LocalDynamoServerConfig(availablePort), server)
    )(
      release = {
        case (_, server) => IO(server.stop())
      }
    ).map {
      case (config, _) => config
    }

  final case class LocalDynamoServerConfig(
    port: Int,
  )

}