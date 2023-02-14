package au.id.tmm.fetch.aws.textract.parsing

import au.id.tmm.fetch.aws.textract.model.{AtomicBlock, BlockId, KeyValueSet, PageNumber}
import au.id.tmm.collections.syntax.toIterableOps
import au.id.tmm.utilities.errors.{ExceptionOr, GenericException}
import cats.syntax.apply._
import cats.syntax.traverse.toTraverseOps
import cats.syntax.traverseFilter.toTraverseFilterOps
import software.amazon.awssdk.services.textract.{model => sdk}

import scala.collection.immutable.ArraySeq

object KeyValueSets {

  import Common._
  import Relationships._

  final class Lookup private[KeyValueSets] (allKeyValueSets: ArraySeq[KeyValueSet]) {
    private val keyLookup: Map[BlockId, KeyValueSet] =
      allKeyValueSets.groupBy(_.key.id).view.mapValues(_.head).toMap

    private val valueLookup: Map[BlockId, KeyValueSet] =
      allKeyValueSets.groupBy(_.value.id).view.mapValues(_.head).toMap

    def keyValueSetChildrenOf(block: sdk.Block): ExceptionOr[ArraySeq[KeyValueSet]] =
      for {
        keySetsFromKeyChildren   <- lookupOrIgnore(keyLookup, block, sdk.RelationshipType.CHILD)
        keySetsFromValueChildren <- lookupOrIgnore(valueLookup, block, sdk.RelationshipType.CHILD)
        keySets <-
          if (keySetsFromKeyChildren.diff(keySetsFromValueChildren).isEmpty) {
            Right(keySetsFromKeyChildren)
          } else {
            Left(GenericException(s"Didn't find both key and value"))
          }
      } yield keySets
  }

  def extractKeyValueSets(
    atomBlockLookup: Map[BlockId, AtomicBlock],
    allBlocks: ArraySeq[sdk.Block],
  ): ExceptionOr[Lookup] =
    for {
      kvSetBlocks <- Right(allBlocks.filter(_.blockType == sdk.BlockType.KEY_VALUE_SET))
      kvSetBlocksById <-
        kvSetBlocks
          .traverse(b => BlockId.fromString(b.id).map(_ -> b))
          .map(_.toMap)

      keyValueSets <-
        kvSetBlocks
          .traverseFilter { block =>
            for {
              isKey <- isKeyBlock(block)
              maybeKeyValueSet <-
                if (isKey) {
                  parseKeyValueSet(atomBlockLookup, kvSetBlocksById, block).map(Some.apply)
                } else {
                  Right(None)
                }
            } yield maybeKeyValueSet
          }

    } yield new Lookup(keyValueSets)

  private def parseKeyValueSet(
    atomBlockLookup: Map[BlockId, AtomicBlock],
    kvSetBlocksLookup: Map[BlockId, sdk.Block],
    keyBlock: sdk.Block,
  ): ExceptionOr[KeyValueSet] =
    for {
      key            <- parseKey(atomBlockLookup, keyBlock)
      valueSdkBlocks <- lookupOrFail(kvSetBlocksLookup, keyBlock, sdk.RelationshipType.VALUE)
      valueSdkBlock  <- valueSdkBlocks.onlyElementOrException
      value          <- parseValue(atomBlockLookup, valueSdkBlock)
    } yield KeyValueSet(key, value)

  private def parseKey(
    atomBlockLookup: Map[BlockId, AtomicBlock],
    keyBlock: sdk.Block,
  ): ExceptionOr[KeyValueSet.Key] =
    for {
      id         <- BlockId.fromString(keyBlock.id)
      pageNumber <- PageNumber(keyBlock.page)
      geometry   <- parseGeometry(keyBlock.geometry)
      children   <- lookupOrIgnore(atomBlockLookup, keyBlock, sdk.RelationshipType.CHILD)
    } yield KeyValueSet.Key(
      id,
      pageNumber,
      geometry,
      children,
    )

  private def parseValue(
    atomBlockLookup: Map[BlockId, AtomicBlock],
    valueSdkBlock: sdk.Block,
  ): ExceptionOr[KeyValueSet.Value] =
    for {
      _          <- requireValueBlock(valueSdkBlock)
      id         <- BlockId.fromString(valueSdkBlock.id)
      pageNumber <- PageNumber(valueSdkBlock.page)
      geometry   <- parseGeometry(valueSdkBlock.geometry)
      children   <- lookupOrFail(atomBlockLookup, valueSdkBlock, sdk.RelationshipType.CHILD)
    } yield KeyValueSet.Value(
      id,
      pageNumber,
      geometry,
      children,
    )

  private def isKeyBlock(sdkBlock: sdk.Block): ExceptionOr[Boolean] =
    (requireNonNull(sdkBlock.blockType), hasEntityType(sdkBlock, sdk.EntityType.KEY)).mapN {
      (blockType, hasEntityType) =>
        hasEntityType && blockType == sdk.BlockType.KEY_VALUE_SET
    }

  private def requireValueBlock(sdkBlock: sdk.Block): ExceptionOr[Unit] =
    isValueBlock(sdkBlock).flatMap { valueBlockCheck =>
      Either.cond(valueBlockCheck, (), GenericException("Expected value block"))
    }

  private def isValueBlock(sdkBlock: sdk.Block): ExceptionOr[Boolean] =
    (requireNonNull(sdkBlock.blockType), hasEntityType(sdkBlock, sdk.EntityType.VALUE)).mapN {
      (blockType, hasEntityType) =>
        hasEntityType && blockType == sdk.BlockType.KEY_VALUE_SET
    }

  private def hasEntityType(sdkBlock: sdk.Block, entityType: sdk.EntityType): ExceptionOr[Boolean] =
    Option(sdkBlock.entityTypes).map(_.size) match {
      case None | Some(0) => Right(false)
      case Some(1)        => Right(sdkBlock.entityTypes.get(0) == entityType)
      case Some(_)        => Left(GenericException("Multiple entity types"))
    }

}
