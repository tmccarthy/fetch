package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{KeyValueSet, Page}
import au.id.tmm.fetch.aws.textract.results.index.AnalysisResultIndex
import au.id.tmm.utilities.errors.ExceptionOr

final class ValueOps private (value: KeyValueSet.Value)(implicit index: AnalysisResultIndex)
    extends BlockCommonOps[KeyValueSet.Value](value) {
  def parent: ExceptionOr[Page]         = index.parentOf(value)
  def kvSet: ExceptionOr[KeyValueSet]   = index.kvSetFor(value)
  def key: ExceptionOr[KeyValueSet.Key] = index.keyFor(value)
}

object ValueOps {
  trait ToValueOps {
    implicit def toValueOps(value: KeyValueSet.Value)(implicit index: AnalysisResultIndex): ValueOps =
      new ValueOps(value)
  }
}
