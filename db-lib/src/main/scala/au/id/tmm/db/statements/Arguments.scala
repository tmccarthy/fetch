package au.id.tmm.db.statements

import java.sql.PreparedStatement
import au.id.tmm.db.data.ColumnIndex

import scala.collection.immutable.ArraySeq

final class Arguments private[db] (private[db] val asSeq: ArraySeq[Argument[_]]) {
  private[db] def bindToUnsafe(preparedStatement: PreparedStatement): Unit = {
    preparedStatement.clearParameters()

    var index = ColumnIndex.first

    asSeq.foreach { boundArgument =>
      boundArgument.bindToUnsafe(preparedStatement, index)
      index = index.increment
    }
  }

  override def equals(other: Any): Boolean = other match {
    case that: Arguments =>
      asSeq == that.asSeq
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(asSeq)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def toString: String = getClass.getSimpleName + asSeq.mkString("(", ",", ")")

}

object Arguments {
  def apply(boundArguments: Seq[Argument[_]]): Arguments = new Arguments(boundArguments.to(ArraySeq))
  def of(boundArguments: Argument[_]*): Arguments        = new Arguments(ArraySeq(boundArguments: _*))
  val empty: Arguments                                   = new Arguments(ArraySeq.empty)
}
