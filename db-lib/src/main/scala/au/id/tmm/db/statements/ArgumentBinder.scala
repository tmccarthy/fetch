package au.id.tmm.db.statements

import java.sql.PreparedStatement
import java.time.LocalDate

import au.id.tmm.db.data.ColumnIndex

trait ArgumentBinder[-A] {
  def bindArgumentUnsafe(
    preparedStatement: PreparedStatement,
    index: ColumnIndex,
    a: A,
  ): Unit

  def contramap[B](f: B => A): ArgumentBinder[B] = (ps, index, b) => bindArgumentUnsafe(ps, index, f(b))
}

// TODO invariant
object ArgumentBinder {
  def apply[A : ArgumentBinder]: ArgumentBinder[A] = implicitly

  implicit val forInt: ArgumentBinder[Int]       = (ps, index, a) => ps.setInt(index.asJdbc, a)
  implicit val forLong: ArgumentBinder[Long]     = (ps, index, a) => ps.setLong(index.asJdbc, a)
  implicit val forDouble: ArgumentBinder[Double] = (ps, index, a) => ps.setDouble(index.asJdbc, a)
  implicit val forString: ArgumentBinder[String] = (ps, index, a) => ps.setString(index.asJdbc, a)
  implicit val forBigDecimal: ArgumentBinder[BigDecimal] = (ps, index, a) =>
    ps.setBigDecimal(index.asJdbc, a.bigDecimal)
  implicit val forLocalDate: ArgumentBinder[LocalDate] = (ps, index, date) => ps.setObject(index.asJdbc, date)
  //noinspection GetOrElseNull
  implicit def forOption[A : ArgumentBinder]: ArgumentBinder[Option[A]] =
    (ps, index, optionA) =>
      optionA match {
        case Some(a) => ArgumentBinder[A].bindArgumentUnsafe(ps, index, a)
        case None    => ps.setObject(index.asJdbc, null)
      }

}
