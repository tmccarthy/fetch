package au.id.tmm.db.statements

sealed trait DbSymbol

final case class ColumnName(asString: String)                        extends DbSymbol
final case class TableName(asString: String)                         extends DbSymbol
final case class ColumnOnTable(table: TableName, column: ColumnName) extends DbSymbol
