package au.id.tmm.fetch.aws.s3

import au.id.tmm.digest4s.binarycodecs.syntax._
import au.id.tmm.digest4s.digest.MD5Digest
import au.id.tmm.digest4s.digest.syntax._
import au.id.tmm.fetch.aws.s3.S3Store.{S3CheckResult, S3KeyResolvedAgainstPrefix, Source}
import au.id.tmm.fetch.aws.{makeClientAsyncConfiguration, toIO}
import au.id.tmm.fetch.cache.KVStore
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.effect.{IO, Resource}
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import cats.syntax.traverse._
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model._
import sttp.client3.Response
import sttp.model.HeaderNames

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

class S3Store private (
  bucket: S3BucketName,
  namePrefix: Option[S3Key],
  s3Client: S3AsyncClient,
) extends KVStore[IO, S3Key, S3Store.Source, S3ObjectRef] {
  override def get(k: S3Key): IO[Option[S3ObjectRef]] =
    contains(k).map {
      case true  => Some(makeObjectRefFor(resolve(k)))
      case false => None
    }

  override def contains(k: S3Key): IO[Boolean] = checkKey(resolve(k)).map {
    case S3CheckResult.NoObjectAtKey                                                => false
    case S3CheckResult.ObjectAtKeyMissingMetadata | S3CheckResult.ObjectAtKey(_, _) => true
  }

  override def put(k: S3Key, source: S3Store.Source): IO[S3ObjectRef] =
    for {
      resolvedKey <- IO.pure(resolve(k))
      checkResult <- checkKey(resolvedKey)
      _ <- checkResult match {
        case S3CheckResult.NoObjectAtKey | S3CheckResult.ObjectAtKeyMissingMetadata =>
          s3Put(resolvedKey, source, source.bytes.md5)

        case S3CheckResult.ObjectAtKey(existingObjectContentType, existingObjectChecksum) =>
          for {
            digest <- IO.pure(source.bytes.md5)
            _ <-
              if (existingObjectChecksum == digest && source.contentType.contains(existingObjectContentType)) {
                IO.unit
              } else {
                s3Put(resolvedKey, source, digest)
              }
          } yield ()
      }
    } yield makeObjectRefFor(resolvedKey)

  override def drop(k: S3Key): IO[Unit] = {
    val request = DeleteObjectRequest
      .builder()
      .bucket(bucket.asString)
      .key(resolve(k).asKey.toRaw)
      .build()

    toIO(IO(s3Client.deleteObject(request))).as(())
  }

  private def resolve(givenKey: S3Key): S3KeyResolvedAgainstPrefix = namePrefix match {
    case Some(prefix) => S3KeyResolvedAgainstPrefix(prefix.resolve(givenKey))
    case None         => S3KeyResolvedAgainstPrefix(givenKey)
  }

  private def makeObjectRefFor(resolvedKey: S3KeyResolvedAgainstPrefix) =
    S3ObjectRef(bucket, resolvedKey.asKey)

  private def checkKey(key: S3KeyResolvedAgainstPrefix): IO[S3CheckResult] = {
    val request: HeadObjectRequest = HeadObjectRequest
      .builder()
      .bucket(bucket.asString)
      .key(key.asKey.toRaw)
      .build()

    val maybeSdkHeadResponse: IO[Option[HeadObjectResponse]] =
      toIO(IO(s3Client.headObject(request)))
        .map[Option[HeadObjectResponse]](Some(_))
        .recover { case e: NoSuchKeyException =>
          None
        }

    maybeSdkHeadResponse.flatMap {
      case Some(response) => IO.fromEither(checkResultFrom(response))
      case None           => IO.pure(S3CheckResult.NoObjectAtKey)
    }
  }

  private def checkResultFrom(sdkHeadResponse: HeadObjectResponse): ExceptionOr[S3CheckResult] = {
    val metadataMap: mutable.Map[String, String] = sdkHeadResponse.metadata.asScala

    val contentType: Option[String] = metadataMap.get("Content-Type")

    val errorOrDigest: ExceptionOr[Option[MD5Digest]] = metadataMap
      .get("Content-MD5")
      .traverse(_.parseBase64.map(MD5Digest.apply))

    errorOrDigest.map { digest =>
      (contentType, digest) match {
        case (Some(contentType), Some(digest)) => S3CheckResult.ObjectAtKey(contentType, digest)
        case _                                 => S3CheckResult.ObjectAtKeyMissingMetadata
      }
    }
  }

  private def s3Put(
    key: S3KeyResolvedAgainstPrefix,
    source: Source,
    sourceMd5: MD5Digest,
  ): IO[Unit] = {
    val request = PutObjectRequest
      .builder()
      .bucket(bucket.asString)
      .key(key.asKey.toRaw)
      .contentType(source.contentType.orNull)
      .contentMD5(sourceMd5.asBase64String)
      .build()

    val body = AsyncRequestBody.fromBytes(source.bytes.unsafeArray)

    toIO(IO(s3Client.putObject(request, body))).as(())
  }

}

object S3Store {

  /**
    * Attempts to use the `ExecutionContext` of `IO` as the `Executor` for the `S3AsyncClient`. Just uses the default if
    * this fails.
    */
  def apply(bucket: S3BucketName, namePrefix: Option[S3Key]): Resource[IO, S3Store] =
    for {
      s3Client <- s3ClientResource
    } yield new S3Store(bucket, namePrefix, s3Client)

  private val s3ClientResource: Resource[IO, S3AsyncClient] =
    Resource.fromAutoCloseable {
      for {
        clientAsyncConfiguration <- makeClientAsyncConfiguration
      } yield S3AsyncClient.builder().asyncConfiguration(clientAsyncConfiguration).build()
    }

  final case class Source(
    bytes: ArraySeq.ofByte, // TODO not sure about the type here
    contentType: Option[String],
  )

  object Source {
    def fromSttpResponse(response: Response[Either[_, Array[Byte]]]): ExceptionOr[Source] =
      for {
        bytes <- response.body
          .map(bytes => new ArraySeq.ofByte(bytes))
          .left
          .map(errorResponse =>
            GenericException(s"Http response code was ${response.code.code}, message was $errorResponse"),
          )
        contentType = response.header(HeaderNames.ContentType)
      } yield Source(bytes, contentType)
  }

  private sealed trait S3CheckResult

  private object S3CheckResult {
    case object NoObjectAtKey                                                    extends S3CheckResult
    case object ObjectAtKeyMissingMetadata                                       extends S3CheckResult
    final case class ObjectAtKey(objectContentType: String, checksum: MD5Digest) extends S3CheckResult
  }

  private final case class S3KeyResolvedAgainstPrefix(asKey: S3Key)

}
