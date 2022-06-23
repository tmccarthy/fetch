package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{KeyValueSet, Page}
import au.id.tmm.fetch.aws.textract.results.index.AnalysisResultIndex
import au.id.tmm.utilities.errors.ExceptionOr

import scala.collection.immutable.ArraySeq

final class KeyValueSetOps private (keyValueSet: KeyValueSet)(implicit index: AnalysisResultIndex) {
  def parent: ExceptionOr[Page] = index.parentOf(keyValueSet)
  def siblings: ExceptionOr[ArraySeq[KeyValueSet]] = index.siblingsOf(keyValueSet)
}

object KeyValueSetOps {
  trait ToKeyValueSetOps {
    implicit def toKeyValueSetOps(keyValueSet: KeyValueSet)(implicit index: AnalysisResultIndex): KeyValueSetOps =
      new KeyValueSetOps(keyValueSet)
  }
}
