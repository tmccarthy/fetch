package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model.{BlockId, Confidence, PageNumber, Word}
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import software.amazon.awssdk.services.textract.{model => sdk}

private[parsing] object Words {

  import Common._

  def parseWord(block: sdk.Block): ExceptionOr[Word] =
    for {
      _          <- requireBlockType(block, sdk.BlockType.WORD)
      id         <- BlockId.fromString(block.id)
      pageNumber <- PageNumber(block.page)
      geometry   <- parseGeometry(block.geometry)
      text       <- requireNonNull(block.text)
      confidence <- Confidence(block.confidence)
      textType <- block.textType match {
        case sdk.TextType.HANDWRITING            => Right(Word.TextType.Handwriting)
        case sdk.TextType.PRINTED                => Right(Word.TextType.Printed)
        case sdk.TextType.UNKNOWN_TO_SDK_VERSION => Left(GenericException("Unknown text type"))
      }
    } yield Word(id, pageNumber, geometry, text, confidence, textType)

}
