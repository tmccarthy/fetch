package au.id.tmm.fetch.aws.textract.results.index.syntax

import au.id.tmm.fetch.aws.textract.model.AtomicBlock
import au.id.tmm.fetch.aws.textract.results.index.{AnalysisResultIndex, AtomicBlockParent}
import au.id.tmm.utilities.errors.ExceptionOr

import scala.collection.immutable.ArraySeq

final class AtomicBlockOps private (atomicBlock: AtomicBlock)(implicit index: AnalysisResultIndex)
    extends BlockCommonOps[AtomicBlock](atomicBlock) {
  def parent: ExceptionOr[AtomicBlockParent] =
    index.parentOf(atomicBlock)

  def siblings: ExceptionOr[ArraySeq[AtomicBlock]] =
    index.siblingsOf(atomicBlock)
}

object AtomicBlockOps {
  trait ToAtomicBlockOps {
    implicit def toAtomicBlockOps(atomicBlock: AtomicBlock)(implicit index: AnalysisResultIndex): AtomicBlockOps =
      new AtomicBlockOps(atomicBlock)
  }
}
