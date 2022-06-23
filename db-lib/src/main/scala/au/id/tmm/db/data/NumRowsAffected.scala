package au.id.tmm.db.data

import cats.Monoid

final case class NumRowsAffected(asLong: Long) extends AnyVal

object NumRowsAffected {
  implicit val monoid: Monoid[NumRowsAffected] = new Monoid[NumRowsAffected] {
    override def empty: NumRowsAffected = NumRowsAffected(0)

    override def combine(x: NumRowsAffected, y: NumRowsAffected): NumRowsAffected = NumRowsAffected(x.asLong + y.asLong)
  }
}
