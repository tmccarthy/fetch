package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{KeyValueSet, Page}
import au.id.tmm.fetch.aws.textract.results.index.AnalysisResultIndex
import au.id.tmm.utilities.errors.ExceptionOr

final class KeyOps private (key: KeyValueSet.Key)(implicit index: AnalysisResultIndex)
    extends BlockCommonOps[KeyValueSet.Key](key) {
  def parent: ExceptionOr[Page] = index.parentOf(key)
  def kvSet: ExceptionOr[KeyValueSet] = index.kvSetFor(key)
  def value: ExceptionOr[KeyValueSet.Value] = index.valueFor(key)
}

object KeyOps {
  trait ToKeyOps {
    implicit def toKeyOps(key: KeyValueSet.Key)(implicit index: AnalysisResultIndex): KeyOps =
      new KeyOps(key)
  }
}
