package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model.BlockId
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException, ProductException}
import cats.syntax.traverse.toTraverseOps
import software.amazon.awssdk.services.textract.{model => sdk}

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters._

private[parsing] object Relationships {

  import Common._

  def lookupOrIgnore[B](
    lookup: Map[BlockId, B],
    sdkBlock: sdk.Block,
    relationshipType: sdk.RelationshipType,
  ): ExceptionOr[ArraySeq[B]] =
    for {
      relationships <- requireNonNull(sdkBlock.relationships)
      ids           <- idsFrom(relationships, relationshipType)

      blocks = ids.flatMap(lookup.get)
    } yield blocks

  def lookupOrFail[B](
    lookup: Map[BlockId, B],
    parentBlock: sdk.Block,
    relationshipType: sdk.RelationshipType,
  ): ExceptionOr[ArraySeq[B]] =
    for {
      relationships <- requireNonNull(parentBlock.relationships)
      ids           <- idsFrom(relationships, relationshipType)
      blocks <- ids.traverse { blockId =>
        lookup.get(blockId).toRight(PartialBlockNotFoundException(blockId, relationshipType))
      }
    } yield blocks

  def idsFrom(
    relationships: java.util.List[sdk.Relationship],
    relationshipType: sdk.RelationshipType,
  ): ExceptionOr[ArraySeq[BlockId]] =
    for {
      idsAsStrings <- Right {
        relationships.asScala
          .to(ArraySeq)
          .flatMap {
            case r if r.`type` == relationshipType => r.ids.asScala.to[ArraySeq[String]](ArraySeq)
            case _                                 => ArraySeq.empty[String]
          }
      }

      ids <- idsAsStrings.traverse(BlockId.fromString)
    } yield ids

  final case class PartialBlockNotFoundException(
    badBlockId: BlockId,
    expectedRelationshipType: sdk.RelationshipType,
  ) extends ProductException

  final case class BlockNotFoundException(
    badBlockId: BlockId,
    expectedRelationshipType: sdk.RelationshipType,
    foundAs: Option[sdk.BlockType],
    cause: PartialBlockNotFoundException,
  ) extends ProductException.WithCause(cause)

  def enrichAnyBlockNotFoundFailures[A](allBlocks: ArraySeq[sdk.Block], value: ExceptionOr[A]): ExceptionOr[A] =
    value.left.map(enrichBadBlockException(allBlocks, _))

  private def enrichBadBlockException(allBlocks: ArraySeq[sdk.Block], e: Exception): Exception =
    e match {
      case cause @ PartialBlockNotFoundException(badBlockId, expectedRelationshipType) =>
        BlockNotFoundException(
          badBlockId,
          expectedRelationshipType,
          foundAs = allBlocks.collectFirst {
            case b if BlockId.fromString(b.id).contains(badBlockId) => b.blockType
          },
          cause,
        )
      case e @ GenericException(_, Some(cause: Exception)) =>
        e.copy(cause = Some(enrichBadBlockException(allBlocks, cause)))
      case e => e
    }

}
