package au.id.tmm.fetch.aws.textract

import au.id.tmm.digest4s.digest.SHA512Digest
import au.id.tmm.digest4s.syntax._
import au.id.tmm.fetch.aws.s3.{S3Key, S3ObjectRef, S3Store}
import au.id.tmm.fetch.aws.textract.model.AnalysisResult
import au.id.tmm.fetch.cache.Cache
import cats.effect.IO
import org.apache.commons.io.FilenameUtils
import software.amazon.awssdk.services.{textract => sdk}
import sttp.model.MediaType

import scala.collection.immutable.ArraySeq

final class FriendlyClient(
  s3Cache: Cache[IO, S3Key, S3Store.Source, S3ObjectRef],
  analysisCache: Cache[IO, SHA512Digest, AnalysisResult, AnalysisResult],
  analysisClient: AwsTextractAnalysisClient,
) {

  def runAnalysisFor(document: FriendlyClient.Document): IO[AnalysisResult] = {
    val digest: SHA512Digest = document.bytes.sha512

    analysisCache.get(digest) {
      for {
        uploadedDocumentLocation: S3ObjectRef <- s3Cache.get(makeS3KeyFor(digest, document)) {
          IO.pure {
            document.bytes match {
              case bytes: ArraySeq.ofByte => S3Store.Source(bytes, document.mediaType)
              case bytes => {
                val bytesArray = new Array[Byte](bytes.size)

                //noinspection ScalaUnusedExpression
                bytes.copyToArray(bytesArray)

                S3Store.Source(new ArraySeq.ofByte(bytesArray), document.mediaType)
              }
            }
          }
        }
        sdkDocumentLocation = asSdkDocumentLocation(uploadedDocumentLocation)
        analysisResult <- analysisClient.run(
          sdkDocumentLocation,
          None,
        ) // TODO configure whether to run the full analysis with tables/forms
      } yield analysisResult
    }
  }

  private def makeS3KeyFor(
    digest: SHA512Digest,
    document: FriendlyClient.Document,
  ): S3Key = S3Key((document.fileNameHint.map(FilenameUtils.getName).toList :+ digest.asHexString).mkString("-"))

  private def asSdkDocumentLocation(s3ObjectRef: S3ObjectRef): sdk.model.DocumentLocation =
    sdk.model.DocumentLocation
      .builder()
      .s3Object(
        sdk.model.S3Object
          .builder()
          .bucket(s3ObjectRef.bucket.asString)
          .name(s3ObjectRef.key.toRaw)
          .build(),
      )
      .build()

}

object FriendlyClient {

  final case class Document(
    bytes: ArraySeq[Byte],
    mediaType: Option[MediaType],
    fileNameHint: Option[String],
  )

}
