package au.id.tmm.db.data

import au.id.tmm.db.statements.{ArgumentBinder, FromResultSetColumn}

final case class DbId[-A](asLong: Long)

object DbId {
  implicit def fromResultSetColumn[A]: FromResultSetColumn[DbId[A]] = FromResultSetColumn[Long].map(DbId.apply)

  implicit def binder[A]: ArgumentBinder[DbId[A]] = ArgumentBinder[Long].contramap(_.asLong)
}
