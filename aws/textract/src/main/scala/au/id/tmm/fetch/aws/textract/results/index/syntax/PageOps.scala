package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.{KeyValueSet, Page}
import au.id.tmm.fetch.aws.textract.results.index.AnalysisResultIndex
import au.id.tmm.utilities.errors.ExceptionOr

import scala.collection.immutable.ArraySeq

final class PageOps private (page: Page)(implicit index: AnalysisResultIndex) extends BlockCommonOps[Page](page) {

  def siblings: ExceptionOr[ArraySeq[Page]] = index.siblingsOf(page)

  def keysMatching(predicate: KeyValueSet.Key => Boolean): ArraySeq[KeyValueSet.Key] =
    page.children.collect {
      case Page.Child.OfKeyValueSet(keyValueSet) if predicate(keyValueSet.key) => keyValueSet.key
    }

}

object PageOps {
  trait ToPageOps {
    implicit def toPageOps(page: Page)(implicit index: AnalysisResultIndex): PageOps =
      new PageOps(page)
  }
}
