package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model._
import au.id.tmm.utilities.errors.ExceptionOr
import software.amazon.awssdk.services.textract.{model => sdk}

import scala.collection.MapView

private[parsing] object Pages {

  import Common._
  import Relationships._

  def parsePage(
    lineLookup: Map[BlockId, Line],
    tableLookup: Map[BlockId, Table],
    keyValueSetLookup: KeyValueSets.Lookup,
    block: sdk.Block,
  ): ExceptionOr[Page] =
    for {
      _          <- requireBlockType(block, sdk.BlockType.PAGE)
      id         <- BlockId.fromString(block.id)
      pageNumber <- PageNumber(block.page)
      geometry   <- parseGeometry(block.geometry)

      childLookup: Map[BlockId, Page.Child] = (
        (lineLookup.view.mapValues(Page.Child.OfLine.apply): MapView[BlockId, Page.Child]) ++
          (tableLookup.view.mapValues(Page.Child.OfTable.apply): MapView[BlockId, Page.Child])
      ).toMap

      lineAndTableChildren <- lookupOrIgnore(childLookup, block, sdk.RelationshipType.CHILD)
      keyValueSetChildren  <- keyValueSetLookup.keyValueSetChildrenOf(block)
    } yield Page(
      id,
      pageNumber,
      geometry,
      lineAndTableChildren ++ keyValueSetChildren.map(Page.Child.OfKeyValueSet.apply),
    )

}
