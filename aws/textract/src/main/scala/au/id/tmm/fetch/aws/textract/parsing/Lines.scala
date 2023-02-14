package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model._
import au.id.tmm.utilities.errors.ExceptionOr
import software.amazon.awssdk.services.textract.{model => sdk}

private[parsing] object Lines {

  import Common._
  import Relationships._

  def parseLine(
    atomBlocksLookup: Map[BlockId, AtomicBlock],
    block: sdk.Block,
  ): ExceptionOr[Line] =
    for {
      _          <- requireBlockType(block, sdk.BlockType.LINE)
      id         <- BlockId.fromString(block.id)
      pageNumber <- PageNumber(block.page)
      geometry   <- parseGeometry(block.geometry)
      text       <- requireNonNull(block.text)
      children   <- lookupOrFail(atomBlocksLookup, block, sdk.RelationshipType.CHILD)
    } yield Line(
      id,
      pageNumber,
      geometry,
      text,
      children,
    )

}
