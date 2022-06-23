package au.id.tmm.fetch.aws.textract

import java.time.Duration

import au.id.tmm.fetch.aws.RetryEffect
import au.id.tmm.fetch.aws.textract.AwsTextractAnalysisClient.logger
import au.id.tmm.fetch.aws.textract.model.AnalysisResult
import au.id.tmm.fetch.aws.textract.parsing.Parse
import au.id.tmm.utilities.errors.GenericException
import cats.effect.{IO, Resource, Timer}
import cats.implicits.catsSyntaxApplicativeError
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.services.textract.model.{GetDocumentAnalysisRequest, InvalidJobIdException}
import software.amazon.awssdk.services.{textract => sdk}

import scala.collection.immutable.ArraySeq

class AwsTextractAnalysisClient private (
  textractClient: sdk.TextractClient,
)(implicit
  timer: Timer[IO],
) {

  def run(
    input: sdk.model.DocumentLocation,
    output: sdk.model.OutputConfig,
  ): IO[AnalysisResult] =
    for {
      jobId          <- startAnalysis(input, output)
      analysisResult <- getAnalysisResult(jobId)
    } yield analysisResult

  private def startAnalysis(
    input: sdk.model.DocumentLocation,
    output: sdk.model.OutputConfig,
  ): IO[TextractJobId] =
    for {
      startAnalysisRequest <- IO.pure(makeStartAnalysisRequest(input, output))
      _                    <- IO(logger.info("Sent document analysis request"))
      startAnalysisResult  <- IO(textractClient.startDocumentAnalysis(startAnalysisRequest))
      jobId                <- IO.fromEither(TextractJobId.fromString(startAnalysisResult.jobId))
    } yield jobId

  private def makeStartAnalysisRequest(
    input: sdk.model.DocumentLocation,
    output: sdk.model.OutputConfig,
  ): sdk.model.StartDocumentAnalysisRequest =
    sdk.model.StartDocumentAnalysisRequest
      .builder()
      .documentLocation(input)
      .featureTypes(
        sdk.model.FeatureType.FORMS,
        sdk.model.FeatureType.TABLES,
      )
      .outputConfig(output)
      .build()

  def getAnalysisResult(jobId: TextractJobId): IO[AnalysisResult] =
    for {
      firstPage  <- waitUntilFinished(jobId)
      otherPages <- readRemaining(jobId, firstPage)
      pages      <- IO.fromEither(Parse.parsePages(ArraySeq(firstPage) ++ otherPages))
    } yield AnalysisResult(jobId, pages)

  private def waitUntilFinished(jobId: TextractJobId): IO[sdk.model.GetDocumentAnalysisResponse] =
    for {
      getAnalysisRequest <- IO.pure(
        GetDocumentAnalysisRequest
          .builder()
          .jobId(jobId.asString)
          .build(),
      )
      getAnalysisResponse <- RetryEffect.exponentialRetry(
        op = for {
          responseOrInvalidJob <-
            IO(textractClient.getDocumentAnalysis(getAnalysisRequest)).attemptNarrow[InvalidJobIdException]
          response <- responseOrInvalidJob match {
            case Left(invalidJobIdException) => IO.pure(RetryEffect.Result.FailedFinished(invalidJobIdException))
            case Right(response) =>
              response.jobStatus match {
                case sdk.model.JobStatus.SUCCEEDED   => IO.pure(RetryEffect.Result.Finished(response))
                case sdk.model.JobStatus.IN_PROGRESS => IO.raiseError(GenericException("Job in progress"))
                case sdk.model.JobStatus.FAILED | sdk.model.JobStatus.PARTIAL_SUCCESS |
                    sdk.model.JobStatus.UNKNOWN_TO_SDK_VERSION =>
                  IO.pure(RetryEffect.Result.FailedFinished(GenericException("Job failed")))
              }
          }
        } yield response,
        initialDelay = Duration.ofSeconds(10),
        factor = 1,
        maxWait = Duration.ofMinutes(2),
      )
    } yield getAnalysisResponse

  private def readRemaining(
    jobId: TextractJobId,
    firstResult: sdk.model.GetDocumentAnalysisResponse,
  ): IO[ArraySeq[sdk.model.GetDocumentAnalysisResponse]] = {
    def go(
      nextToken: Option[String],
      responsesSoFar: List[sdk.model.GetDocumentAnalysisResponse],
    ): IO[List[sdk.model.GetDocumentAnalysisResponse]] =
      nextToken match {
        case None => IO.pure(responsesSoFar)
        case Some(nextToken) => {
          val request = GetDocumentAnalysisRequest
            .builder()
            .jobId(jobId.asString)
            .nextToken(nextToken)
            .build()

          for {
            response <- IO(textractClient.getDocumentAnalysis(request))
            nextToken = Option(response.nextToken())
            allResponses <- go(nextToken, responsesSoFar :+ response)
          } yield allResponses
        }
      }

    go(Option(firstResult.nextToken), responsesSoFar = List.empty).map(_.to(ArraySeq))
  }

}

object AwsTextractAnalysisClient {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply()(implicit timer: Timer[IO]): Resource[IO, AwsTextractAnalysisClient] =
    for {
      sdkClient <- Resource.make(IO(sdk.TextractClient.builder().build()))(sdkClient => IO(sdkClient.close()))
    } yield new AwsTextractAnalysisClient(sdkClient)
}
