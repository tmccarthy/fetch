package au.id.tmm.db.statements

final case class InsertStatement[A](sql: Sql, toArguments: ToArguments[A]) {
  // TODO this way of converting the sql squashes any parameters partially applied to the SQL before we construct the
  //  final update. Should instead find some more sophisticated way of partially composing Arguments and constructing a
  //  final representation here
  def toUpdateStatement(a: A): UpdateStatement = UpdateStatement(sql.copy(arguments = toArguments.toArguments(a)))
}

object InsertStatement {
  def from[A : ToArguments](sql: Sql): InsertStatement[A] = InsertStatement(sql, implicitly)
}
