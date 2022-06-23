package au.id.tmm.db.data

final class ColumnIndex private (val asJdbc: ColumnIndex.AsJdbc) /*extends AnyVal*/ {
  def asZeroIndexed: Int = asJdbc - 1

  def increment: ColumnIndex = new ColumnIndex(asJdbc + 1)
}

object ColumnIndex {
  type AsJdbc = Int

  val first: ColumnIndex = ColumnIndex.fromZeroIndexed(0)

  def apply(asJdbc: Int): ColumnIndex = {
    require(asJdbc > 0, "jdbc column index must be positive")

    new ColumnIndex(asJdbc)
  }

  def fromJdbc(asJdbc: Int): ColumnIndex       = new ColumnIndex(asJdbc)
  def fromZeroIndexed(index: Int): ColumnIndex = new ColumnIndex(index + 1)
}
