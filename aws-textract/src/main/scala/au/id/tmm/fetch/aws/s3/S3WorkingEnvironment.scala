package au.id.tmm.fetch.aws.s3

import java.net.URI
import java.util.concurrent.Executor

import au.id.tmm.digest4s.binarycodecs.syntax._
import au.id.tmm.digest4s.digest.MD5Digest
import au.id.tmm.digest4s.digest.syntax._
import au.id.tmm.fetch.aws.s3.S3WorkingEnvironment._
import au.id.tmm.fetch.aws.toIO
import au.id.tmm.utilities.errors.GenericException
import cats.effect.IO
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import cats.syntax.traverse._
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.client.config.{ClientAsyncConfiguration, SdkAdvancedAsyncClientOption}
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, HeadObjectResponse, NoSuchKeyException, PutObjectRequest}
import sttp.client3.{Response, SttpBackend, _}
import sttp.model.{HeaderNames, Uri => SttpUri}

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

class S3WorkingEnvironment(
  val bucket: S3BucketName,
  val namePrefix: S3Key,
  httpClient: SttpBackend[IO, Any],
  executionContext: Executor,
) {

  private val s3Client =
    S3AsyncClient
      .builder()
      .asyncConfiguration(
        ClientAsyncConfiguration
          .builder()
          .advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executionContext)
          .build(),
      )
      .build()

  def s3ObjectCopyFor(resource: URI): IO[S3ObjectRef] =
    for {
      content     <- retrieveWebResourceContent(resource)
      s3ObjectRef <- s3ObjectFor(content.bytes, content.contentType)
    } yield s3ObjectRef

  private def retrieveWebResourceContent(uri: URI): IO[WebResourceContent] =
    for {
      response: Response[Either[String, Array[Byte]]] <-
        basicRequest
          .response(asByteArray)
          .get(SttpUri(uri))
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
    } yield WebResourceContent(contentType, bytes)

  def s3ObjectFor(bytes: ArraySeq.ofByte, contentType: Option[String]): IO[S3ObjectRef] = {
    val bytesMd5 = bytes.md5
    val name     = S3Key(bytesMd5.asBase64String)
    val key      = namePrefix resolve name

    val check: IO[CheckResult] =
      for {
        headRequest <- IO.pure {
          HeadObjectRequest
            .builder()
            .bucket(bucket.asString)
            .key(key.toRaw)
            .build()
        }

        maybeHeadResponse <- toIO(IO(s3Client.headObject(headRequest)))
          .map[Option[HeadObjectResponse]](Some(_))
          .recover { case e: NoSuchKeyException =>
            None
          }

        contentType = maybeHeadResponse.flatMap(_.metadata.asScala.get("Content-Type"))
        contentMd5 <- IO.fromEither {
          maybeHeadResponse
            .flatTraverse { response =>
              response.metadata.asScala.get("Content-MD5").traverse(_.parseBase64.map(MD5Digest.apply))
            }
        }

      } yield (maybeHeadResponse, contentType, contentMd5) match {
        case (None, _, _)                             => CheckResult.NoObjectAtKey
        case (_, Some(contentType), Some(contentMd5)) => CheckResult.ObjectAtKey(contentType, contentMd5)
        case (_, _, _)                                => CheckResult.ObjectAtKeyMissingMetadata
      }

    val put: IO[Unit] =
      for {
        putRequest <- IO.pure {
          PutObjectRequest
            .builder()
            .bucket(bucket.asString)
            .key(key.toRaw)
            .contentType(contentType.orNull)
            .contentMD5(bytesMd5.asBase64String)
            .build()
        }

        putRequestBody = AsyncRequestBody.fromBytes(bytes.unsafeArray)

        _ <- toIO(IO(s3Client.putObject(putRequest, putRequestBody)))
      } yield ()

    check
      .flatMap {
        case CheckResult.NoObjectAtKey              => put
        case CheckResult.ObjectAtKeyMissingMetadata => put
        case CheckResult.ObjectAtKey(existingContentType, existingChecksum) =>
          if (existingChecksum != bytesMd5 || !contentType.contains(existingContentType))
            put
          else
            IO.unit
      }
      .as(S3ObjectRef(bucket, key))
  }

}

object S3WorkingEnvironment {

  private final case class WebResourceContent(
    contentType: Option[String],
    bytes: ArraySeq.ofByte,
  )

  private sealed trait CheckResult

  private object CheckResult {
    case object NoObjectAtKey                                                    extends CheckResult
    case object ObjectAtKeyMissingMetadata                                       extends CheckResult
    final case class ObjectAtKey(objectContentType: String, checksum: MD5Digest) extends CheckResult
  }
}
