package au.id.tmm.fetch.aws.textract.results.index

import au.id.tmm.fetch.aws.textract.model._

object BlockIterator {

  def recursivelyIterateBlockAndChildren(block: Block, includeKeyValueSets: Boolean = false): Iterator[Block] =
    block match {
      case _: KeyValueSet.Key | _: KeyValueSet.Value if !includeKeyValueSets => Iterator.empty
      case _                                                                 => Iterator(block) ++ recursivelyIterateChildrenOf(block, includeKeyValueSets)
    }

  def recursivelyIterateChildrenOf(block: Block, includeKeyValueSets: Boolean = false): Iterator[Block] =
    block match {
      case _: AtomicBlock => Iterator.empty[Block]
      case line: Line     => line.children.flatMap(c => recursivelyIterateBlockAndChildren(c, includeKeyValueSets)).iterator
      case page: Page =>
        page.children.flatMap {
          case Page.Child.OfLine(line)   => recursivelyIterateBlockAndChildren(line, includeKeyValueSets)
          case Page.Child.OfTable(table) => recursivelyIterateBlockAndChildren(table, includeKeyValueSets)
          case Page.Child.OfKeyValueSet(keyValueSet) =>
            if (includeKeyValueSets) {
              recursivelyIterateBlockAndChildren(
                keyValueSet.key,
                includeKeyValueSets,
              ) ++ recursivelyIterateBlockAndChildren(keyValueSet.value, includeKeyValueSets)
            } else {
              Iterator.empty
            }
        }.iterator
      case table: Table =>
        table.children.flatMap(c => recursivelyIterateBlockAndChildren(c, includeKeyValueSets)).iterator
      case cell: Table.Cell =>
        cell.children.flatMap(c => recursivelyIterateBlockAndChildren(c, includeKeyValueSets)).iterator
      case mergedCell: Table.MergedCell =>
        mergedCell.children.flatMap(c => recursivelyIterateBlockAndChildren(c, includeKeyValueSets)).iterator
      case key: KeyValueSet.Key =>
        key.children.flatMap(c => recursivelyIterateBlockAndChildren(c, includeKeyValueSets)).iterator
      case value: KeyValueSet.Value =>
        value.children.flatMap(c => recursivelyIterateBlockAndChildren(c, includeKeyValueSets)).iterator
    }

}
