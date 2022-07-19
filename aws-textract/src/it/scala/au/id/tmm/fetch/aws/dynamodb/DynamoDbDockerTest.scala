package au.id.tmm.fetch.aws.dynamodb

import java.io.Closeable
import java.net.{ServerSocket, URI}
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

import au.id.tmm.fetch.retries.RetryEffect
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.effect.{IO, Resource}
import cats.syntax.applicativeError._
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.AsyncDockerCmd
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.{ExposedPort, HostConfig, PortBinding, Ports}
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import fs2.io.IOException
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest

// TODO move this stuff (especially the docker stuff) into tmmUtils-testing
object DynamoDbDockerTest {
  val testRegion: Region = Region.AP_SOUTHEAST_2

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val docker: Resource[IO, DockerClient] = Resource.fromAutoCloseable(
    for {
      clientConfig <- IO.pure(DefaultDockerClientConfig.createDefaultConfigBuilder().build())
      httpClient <- IO(
        new ApacheDockerHttpClient.Builder()
          .dockerHost(clientConfig.getDockerHost)
          .sslConfig(clientConfig.getSSLConfig)
          .maxConnections(100)
          .connectionTimeout(Duration.ofSeconds(10))
          .responseTimeout(Duration.ofSeconds(5))
          .build(),
      )
      client <- IO(DockerClientImpl.getInstance(clientConfig, httpClient))
      _ <- RetryEffect
        .exponentialRetry(Duration.ofSeconds(2), 1, Duration.ofSeconds(10)) {
          for {
            _ <- IO(client.pingCmd().exec())
          } yield RetryEffect.Result.Finished(())
        }
        .adaptErr { case e =>
          new IOException("Docker isn't running", e)
        }
    } yield client,
  )

  private val freePort: IO[Int] = Resource
    .make(IO(new ServerSocket(0)))(s => IO(s.close()))
    .use(s => IO(s.getLocalPort))

  private def createdDockerContainer(client: DockerClient): Resource[IO, (Port, ContainerId)] = {
    val dynamoPortOnContainer = ExposedPort.tcp(8000)

    Resource.make[IO, (Port, ContainerId)](
      for {
        pullResponse <- toIO(client.pullImageCmd("docker.io/amazon/dynamodb-local").withTag("latest"))
        _            <- IO.fromEither(Either.cond(pullResponse.isPullSuccessIndicated, (), GenericException("Pull failed")))
        portToUse    <- freePort
        response <- IO {
          client
            .createContainerCmd("docker.io/amazon/dynamodb-local")
            .withExposedPorts(dynamoPortOnContainer)
            .withHostConfig(
              HostConfig
                .newHostConfig()
                .withPortBindings(new PortBinding(Ports.Binding.bindPort(portToUse), dynamoPortOnContainer)),
            )
            .exec()
        }
      } yield (Port(portToUse), ContainerId(response.getId)),
    ) { case (_, containerId) =>
      IO(client.removeContainerCmd(containerId.asString).withForce(true).exec())
        .as(())
        .onError(t => IO(logger.error("Exception when removing container", t)))
    }
  }

  private def runningDockerContainer(client: DockerClient, containerId: ContainerId): Resource[IO, Unit] =
    Resource.make[IO, Unit](
      IO(client.startContainerCmd(containerId.asString).exec()).as(()),
    )(_ =>
      IO(client.stopContainerCmd(containerId.asString).exec())
        .as(())
        .recover { case _: NotModifiedException =>
          ()
        }
        .onError(t => IO(logger.error("Exception when stopping container", t))),
    )

  private def dynamoInstanceUri(port: Port): IO[URI] =
    for {
      dynamoURI <- IO.fromEither(ExceptionOr.catchIn(URI.create(s"http://0.0.0.0:${port.asInt}")))
      _ <- dynamoClient(dynamoURI).use[Unit] { dynamoDbClient =>
        RetryEffect.linearRetry_[Unit](Duration.ofSeconds(1), Duration.ofSeconds(10)) {
          pingDynamo(dynamoDbClient)
        }
      }
    } yield dynamoURI

  private def dynamoClient(dynamoUri: URI): Resource[IO, DynamoDbClient] =
    Resource.fromAutoCloseable(
      IO {
        DynamoDbClient
          .builder()
          .endpointOverride(dynamoUri)
          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
          .region(DynamoDbDockerTest.testRegion)
          .build()
      },
    )

  private def pingDynamo(dynamoDbClient: DynamoDbClient): IO[Unit] =
    for {
      listTablesReq <- IO.pure(ListTablesRequest.builder().limit(1).build())
      _             <- IO(dynamoDbClient.listTables(listTablesReq)).as(())
    } yield ()

  private def toIO[R, C <: AsyncDockerCmd[C, R]](dockerCommand: AsyncDockerCmd[C, R]): IO[R] =
    IO.async_ { cb: (Either[Throwable, R] => Unit) =>
      val latestR: AtomicReference[Option[R]] = new AtomicReference[Option[R]](None)

      dockerCommand.exec[ResultCallback[R]](new ResultCallback[R] {
        override def onStart(closeable: Closeable): Unit = ()

        override def onNext(r: R): Unit = latestR.set(Some(r))

        override def onError(throwable: Throwable): Unit = cb(Left(throwable))

        override def onComplete(): Unit = latestR.get() match {
          case Some(r) => cb(Right(r))
          case None    => cb(Left(GenericException("No response received")))
        }

        override def close(): Unit = ()
      })

      ()
    }

  final case class ContainerId(asString: String) extends AnyVal
  final case class Port(asInt: Int)              extends AnyVal

  val localDynamoDbUri: Resource[IO, URI] =
    for {
      dockerClient        <- docker
      (port, containerId) <- createdDockerContainer(dockerClient)
      _                   <- runningDockerContainer(dockerClient, containerId)
      uri                 <- Resource.liftK(dynamoInstanceUri(port))
    } yield uri
}
