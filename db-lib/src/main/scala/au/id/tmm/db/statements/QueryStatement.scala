package au.id.tmm.db.statements

/**
  * An SQL query, ie some [[Sql]] along with a [[FromResultSet]] which describes how to read the output of
  * the query.
  */
final case class QueryStatement[+A](sql: Sql, fromResultSet: FromResultSet[A])

object QueryStatement {
  def from[A : FromResultSet](sql: Sql): QueryStatement[A] = QueryStatement(sql, implicitly)
}
