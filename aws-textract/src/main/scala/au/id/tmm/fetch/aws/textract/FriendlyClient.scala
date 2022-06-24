package au.id.tmm.fetch.aws.textract

import java.net.URI
import java.nio.file.{Files, Path}
import java.time.Duration

import au.id.tmm.fetch.aws.textract.FriendlyClient.JobIdCache.UsingDynamoDb.{
  makeTableIfNoneDefined,
  waitForTableCreated,
}
import au.id.tmm.fetch.aws.textract.FriendlyClient.{CachedJobHasExpired, Document, DocumentContent, logger}
import au.id.tmm.fetch.aws.textract.model.AnalysisResult
import au.id.tmm.fetch.aws.{RetryEffect, S3Key, toIO}
import au.id.tmm.digest4s.binarycodecs.syntax._
import au.id.tmm.digest4s.digest.syntax._
import au.id.tmm.digest4s.digest.{MD5Digest, SHA512Digest}
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.effect.{IO, Resource, Timer}
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.traverse.toTraverseOps
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.client.config.{ClientAsyncConfiguration, SdkAdvancedAsyncClientOption}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.textract.model.{DocumentLocation, InvalidJobIdException, OutputConfig, S3Object}
import sttp.client3._
import sttp.model.{HeaderNames, Uri => SttpUri}

import scala.collection.immutable.ArraySeq
import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters._

final class FriendlyClient(
  cache: FriendlyClient.JobIdCache,
  s3Bucket: String,
  s3WorkingDirectoryPrefix: S3Key,
  httpClient: SttpBackend[IO, Any],
  s3Client: S3AsyncClient,
  analysisClient: AwsTextractAnalysisClient,
)(implicit
  timer: Timer[IO],
) {

  def runAnalysisFor(documentLocation: FriendlyClient.Document): IO[AnalysisResult] =
    for {
      content       <- contentOf(documentLocation)
      possibleJobId <- cache.getFor(content.sha512Digest)
      result <- possibleJobId match {
        case Some(jobId) =>
          for {
            _                  <- IO(logger.info(s"Found cached job for document $documentLocation. JobId ${jobId.asString}"))
            jobResultOrExpired <- retrieveCachedJobIdResult(jobId)
            jobResult <- jobResultOrExpired.map(IO.pure).getOrElse {
              for {
                _              <- IO(logger.info(s"Cached job for document $documentLocation has expired. Rerunning"))
                _              <- cache.drop(content.sha512Digest)
                analysisResult <- runAnalysis(content)
                _              <- cache.update(content.sha512Digest, analysisResult.jobId)
              } yield analysisResult
            }
          } yield jobResult
        case None =>
          for {
            analysisResult <- runAnalysis(content)
            _              <- cache.update(content.sha512Digest, analysisResult.jobId)
          } yield analysisResult
      }
    } yield result

  private def contentOf(document: Document): IO[DocumentContent] =
    document match {
      case local: Document.Local   => contentOf(local)
      case remote: Document.Remote => contentOf(remote)
    }

  private def contentOf(localDocument: Document.Local): IO[DocumentContent] =
    IO(Files.readAllBytes(localDocument.path)).map { bytes =>
      DocumentContent(
        localDocument.path.getFileName.toString,
        new ArraySeq.ofByte(bytes),
        contentType = None,
        bytes.sha512,
        bytes.md5,
      )
    }
  private def contentOf(remoteDocument: Document.Remote): IO[DocumentContent] =
    for {
      response: Response[Either[String, Array[Byte]]] <-
        basicRequest
          .response(asByteArray)
          .get(SttpUri(remoteDocument.uri))
          .send(httpClient)

      contentType: Option[String] = response.header(HeaderNames.ContentType)

      bytes <- IO.fromEither {
        response.body
          .map(bytes => new ArraySeq.ofByte(bytes))
          .left
          .map(errorResponse =>
            GenericException(s"Http response code was ${response.code.code}, message was $errorResponse"),
          )
      }

      fileName <- IO.fromEither {
        ExceptionOr.catchIn {
          Path.of(remoteDocument.uri.getPath).getFileName.toString
        }
      }

    } yield DocumentContent(
      fileName,
      bytes,
      contentType,
      bytes.sha512,
      bytes.md5,
    )

  private def retrieveCachedJobIdResult(jobId: TextractJobId): IO[Either[CachedJobHasExpired.type, AnalysisResult]] =
    analysisClient.getAnalysisResult(jobId).attemptNarrow[InvalidJobIdException].map {
      case Left(e)       => Left(CachedJobHasExpired)
      case Right(result) => Right(result)
    }

  private def runAnalysis(
    documentContent: DocumentContent,
  ): IO[AnalysisResult] =
    for {
      s3DirectoryForFile <- IO.pure {
        s3WorkingDirectoryPrefix
          .resolve(S3Key(documentContent.sha512Digest.asHexString))
      }

      s3UploadLocation =
        s3DirectoryForFile
          .resolve(S3Key(documentContent.fileName))

      textractOutputDirectory = s3DirectoryForFile.resolve("textract_output")

      sdkDocumentLocation =
        DocumentLocation
          .builder()
          .s3Object(S3Object.builder().bucket(s3Bucket).name(s3UploadLocation.toRaw).build())
          .build()

      sdkOutputLocation =
        OutputConfig
          .builder()
          .s3Bucket(s3Bucket)
          .s3Prefix(textractOutputDirectory.toRaw)
          .build()

      _      <- uploadToS3(documentContent, s3UploadLocation)
      result <- analysisClient.run(sdkDocumentLocation, sdkOutputLocation)

    } yield result

  private def uploadToS3(
    documentContent: DocumentContent,
    key: S3Key,
  ) =
    for {
      putRequest <- IO.pure {
        PutObjectRequest
          .builder()
          .bucket(s3Bucket)
          .key(key.toRaw)
          .contentMD5(documentContent.md5Digest.asBase64String)
          .build()
      }

      putRequestBody = AsyncRequestBody.fromBytes(documentContent.body.unsafeArray)

      _ <- toIO(s3Client.putObject(putRequest, putRequestBody))
    } yield ()

}

object FriendlyClient {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply(
    cache: JobIdCache,
    s3Bucket: String,
    s3WorkingDirectoryPrefix: S3Key,
    httpClient: SttpBackend[IO, Any],
    executionContext: ExecutionContextExecutor,
  )(implicit
    timer: Timer[IO],
  ): Resource[IO, FriendlyClient] =
    for {
      analysisClient <- AwsTextractAnalysisClient()
      s3Client <- Resource.make(
        IO {
          S3AsyncClient
            .builder()
            .asyncConfiguration(
              ClientAsyncConfiguration
                .builder()
                .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executionContext)
                .build(),
            )
            .build()
        },
      )(client => IO(client.close()))
    } yield new FriendlyClient(cache, s3Bucket, s3WorkingDirectoryPrefix, httpClient, s3Client, analysisClient)

  sealed trait Document

  object Document {
    final case class Local(path: Path) extends Document
    final case class Remote(uri: URI)  extends Document

    object Local {
      def apply(path: => Path): IO[Local] = IO(new Local(path))
    }

  }

  final case class DocumentContent(
    fileName: String,
    body: ArraySeq.ofByte,
    contentType: Option[String],
    sha512Digest: SHA512Digest,
    md5Digest: MD5Digest,
  )

  trait JobIdCache {
    def getFor(documentDigest: SHA512Digest): IO[Option[TextractJobId]]

    def update(documentDigest: SHA512Digest, textractJobId: TextractJobId): IO[Unit]

    def drop(documentDigest: SHA512Digest): IO[Unit]

    def clear: IO[Unit]
  }

  object JobIdCache {

    final class UsingDynamoDb private (
      tableName: String,
      client: DynamoDbClient,
    )(implicit
      timer: Timer[IO],
    ) extends JobIdCache {
      override def getFor(documentDigest: SHA512Digest): IO[Option[TextractJobId]] =
        for {
          request <- IO.pure {
            GetItemRequest
              .builder()
              .tableName(tableName)
              .key(
                Map(
                  "documentDigest" -> AttributeValue
                    .builder()
                    .s(documentDigest.asHexString)
                    .build(),
                ).asJava,
              )
              .build()
          }

          response <- IO(client.getItem(request))

          maybeAttributeValue <- Option(response.item())
            .filterNot(_.isEmpty)
            .traverse { javaMap =>
              IO.fromEither {
                javaMap.asScala
                  .get("jobId")
                  .toRight(GenericException("No key for item"))
              }
            }

          rawJobId <- IO.fromEither {
            maybeAttributeValue.traverse { attributeValue =>
              for {
                s <- Option(attributeValue.s).toRight(GenericException("Value wasn't string"))
              } yield s
            }
          }

          jobId <- IO.fromEither(rawJobId.traverse(TextractJobId.fromString))

        } yield jobId

      override def update(documentDigest: SHA512Digest, textractJobId: TextractJobId): IO[Unit] =
        for {
          request <- IO.pure(
            PutItemRequest
              .builder()
              .tableName(tableName)
              .item(
                Map(
                  "documentDigest" -> AttributeValue.builder().s(documentDigest.asHexString).build(),
                  "jobId"          -> AttributeValue.builder().s(textractJobId.asString).build(),
                ).asJava,
              )
              .build(),
          )

          response <- IO(client.putItem(request))
        } yield ()

      override def clear: IO[Unit] =
        for {
          deleteTableRequest <- IO.pure(
            DeleteTableRequest.builder().tableName(tableName).build(),
          )

          _ <- IO(client.deleteTable(deleteTableRequest))

          _ <- IO(logger.info(s"Deleted table $tableName"))

          _ <- makeTableIfNoneDefined(client, tableName)
          _ <- waitForTableCreated(client, tableName)
        } yield ()

      override def drop(documentDigest: SHA512Digest): IO[Unit] =
        for {
          request <- IO.pure(
            DeleteItemRequest
              .builder()
              .tableName(tableName)
              .key(
                Map(
                  "documentDigest" -> AttributeValue.builder().s(documentDigest.asHexString).build(),
                ).asJava,
              )
              .build(),
          )

          _ <- IO(client.deleteItem(request))
        } yield ()
    }

    object UsingDynamoDb {
      def apply(tableName: String)(implicit timer: Timer[IO]): Resource[IO, UsingDynamoDb] =
        for {
          client <- Resource.make(IO(DynamoDbClient.builder().build()))(dynamoDbClient => IO(dynamoDbClient.close()))
          dynamoKeyValueStore <-
            Resource.liftF(makeTableIfNoneDefined(client, tableName).as(new UsingDynamoDb(tableName, client)))
        } yield dynamoKeyValueStore

      private def makeTableIfNoneDefined(
        client: DynamoDbClient,
        tableName: String,
      )(implicit
        timer: Timer[IO],
      ): IO[Unit] =
        for {
          describeTableRequest <- IO.pure(DescribeTableRequest.builder().tableName(tableName).build())
          tableExists <- IO(client.describeTable(describeTableRequest)).as(true).recover {
            case _: ResourceNotFoundException => false
          }

          _ <-
            if (tableExists) {
              IO.unit
            } else {
              for {
                createTableRequest <- IO.pure(
                  CreateTableRequest
                    .builder()
                    .tableName(tableName)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                      AttributeDefinition
                        .builder()
                        .attributeName("documentDigest")
                        .attributeType(ScalarAttributeType.S)
                        .build(),
                    )
                    .keySchema(KeySchemaElement.builder().attributeName("documentDigest").keyType(KeyType.HASH).build())
                    .build(),
                )

                createTableResponse <-
                  IO(client.createTable(createTableRequest)) // TODO need to wait for the able creation to complete

                _ <- RetryEffect.exponentialRetry(
                  op = waitForTableCreated(client, tableName),
                  initialDelay = Duration.ofSeconds(10),
                  factor = 1,
                  maxWait = Duration.ofMinutes(1),
                )

              } yield ()
            }

        } yield ()

      private def waitForTableCreated(client: DynamoDbClient, tableName: String): IO[RetryEffect.Result[Unit]] = {
        val request = DescribeTableRequest.builder().tableName(tableName).build()

        for {
          describeTableResult <- IO(client.describeTable(request))

          result <- Option(describeTableResult.table).map(_.tableStatus) match {
            case Some(TableStatus.CREATING) => IO.raiseError(GenericException("Table still creating"))
            case Some(_)                    => IO.pure(RetryEffect.Result.Finished(()))
            case None                       => IO.pure(RetryEffect.Result.FailedFinished(GenericException("Table not created")))
          }
        } yield result
      }
    }

  }

  private case object CachedJobHasExpired

}
