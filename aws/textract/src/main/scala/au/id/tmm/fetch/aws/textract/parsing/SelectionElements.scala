package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model.{BlockId, PageNumber, SelectionElement}
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import software.amazon.awssdk.services.textract.{model => sdk}

private[parsing] object SelectionElements {

  import Common._

  def parseSelectionElement(block: sdk.Block): ExceptionOr[SelectionElement] =
    for {
      _          <- requireBlockType(block, sdk.BlockType.SELECTION_ELEMENT)
      id         <- BlockId.fromString(block.id)
      pageNumber <- PageNumber(block.page)
      geometry   <- parseGeometry(block.geometry)
      status <- block.selectionStatus match {
        case sdk.SelectionStatus.SELECTED               => Right(SelectionElement.Status.Selected)
        case sdk.SelectionStatus.NOT_SELECTED           => Right(SelectionElement.Status.NotSelected)
        case sdk.SelectionStatus.UNKNOWN_TO_SDK_VERSION => Left(GenericException("Unknown selection status"))
      }
    } yield SelectionElement(id, pageNumber, geometry, status)

}
