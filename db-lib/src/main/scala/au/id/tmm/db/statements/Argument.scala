package au.id.tmm.db.statements

import java.sql.PreparedStatement

import au.id.tmm.db.data.ColumnIndex

/**
  * A representation of a value, combined with a reference to a `ParameterBinder` which is capable of binding that value
  * to a [[java.sql.PreparedStatement]].
  */
final class Argument[A] private (private val binder: ArgumentBinder[A], private val value: A) {
  private[db] def bindToUnsafe(preparedStatement: PreparedStatement, index: ColumnIndex): Unit =
    binder.bindArgumentUnsafe(preparedStatement, index, value)

  override def toString: String = getClass.getSimpleName + "(" + value + ")"

  override def equals(other: Any): Boolean = other match {
    case that: Argument[_] =>
      binder == that.binder &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq[Any](binder, value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Argument {
  def apply[A : ArgumentBinder](value: A): Argument[A] = new Argument[A](ArgumentBinder[A], value)
}
