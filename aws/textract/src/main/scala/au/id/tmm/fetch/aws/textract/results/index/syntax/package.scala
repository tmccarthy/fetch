package au.id.tmm.fetch.aws.textract.results.index

package object syntax
    extends AnyRef
    with AnalysisResultOps.ToAnalysisResultOps
    with AtomicBlockOps.ToAtomicBlockOps
    with BlocksOps.ToBlocksOps
    with CellOps.ToCellOps
    with KeyOps.ToKeyOps
    with KeyValueSetOps.ToKeyValueSetOps
    with LineOps.ToLineOps
    with PageOps.ToPageOps
    with TableOps.ToTableOps
    with ValueOps.ToValueOps
