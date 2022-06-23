package au.id.tmm.db.statements

import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import au.id.tmm.db.data.ColumnIndex
import au.id.tmm.db.statements.FromResultSetColumn.make

// TODO should probably find some way to model the relationship between this type and argument which is its dual
// TODO the error handling on this class is not good
trait FromResultSetColumn[+A] {

  def fromResultSetIndexedColumnUnsafe(resultSet: ResultSet, columnIndex: ColumnIndex): A
  def fromResultSetNamedColumnUnsafe(resultSet: ResultSet, columnName: ColumnName): A

  def map[B](f: A => B): FromResultSetColumn[B] = new FromResultSetColumn[B] {
    override def fromResultSetIndexedColumnUnsafe(resultSet: ResultSet, columnIndex: ColumnIndex): B = f(
      FromResultSetColumn.this.fromResultSetIndexedColumnUnsafe(resultSet, columnIndex),
    )
    override def fromResultSetNamedColumnUnsafe(resultSet: ResultSet, columnName: ColumnName): B = f(
      FromResultSetColumn.this.fromResultSetNamedColumnUnsafe(resultSet, columnName),
    )
  }

  // TODO could probably do with a better exception here
  def emap[B](f: A => Either[String, B]): FromResultSetColumn[B] = map[B] { a =>
    f(a) match {
      case Right(b)           => b
      case Left(errorMessage) => throw new Exception(errorMessage)
    }
  }

}

object FromResultSetColumn extends FromResultSetColumnImplementations {

  def apply[A : FromResultSetColumn]: FromResultSetColumn[A] = implicitly

  def make[A](
    fromColumnIndex: (ResultSet, ColumnIndex.AsJdbc) => A,
    fromColumnName: (ResultSet, String) => A,
  ): FromResultSetColumn[A] =
    new FromResultSetColumn[A] {
      override def fromResultSetIndexedColumnUnsafe(resultSet: ResultSet, columnIndex: ColumnIndex): A =
        fromColumnIndex(resultSet, columnIndex.asJdbc)
      override def fromResultSetNamedColumnUnsafe(resultSet: ResultSet, columnName: ColumnName): A =
        fromColumnName(resultSet, columnName.asString)
    }

  private[db] trait Syntax {
    // TODO should these be safe?
    implicit class ResultSetOps(resultSet: ResultSet) {
      def getColumn[A : FromResultSetColumn](columnIndex: ColumnIndex): A =
        FromResultSetColumn[A].fromResultSetIndexedColumnUnsafe(resultSet, columnIndex)
      def getColumn[A : FromResultSetColumn](columnName: ColumnName): A =
        FromResultSetColumn[A].fromResultSetNamedColumnUnsafe(resultSet, columnName)
    }
  }
}

private[statements] trait FromResultSetColumnImplementations {

  implicit val forBoolean: FromResultSetColumn[Boolean] = make(_.getBoolean(_), _.getBoolean(_))
  implicit val forInt: FromResultSetColumn[Int]         = make(_.getInt(_), _.getInt(_))
  implicit val forLong: FromResultSetColumn[Long]       = make(_.getLong(_), _.getLong(_))

  implicit val forString: FromResultSetColumn[String]         = make(_.getString(_), _.getString(_))
  implicit val forBigDecimal: FromResultSetColumn[BigDecimal] = make(_.getBigDecimal(_), _.getBigDecimal(_))
  implicit val forLocalDate: FromResultSetColumn[LocalDate] =
    FromResultSetColumn[String].map(LocalDate.parse(_, DateTimeFormatter.ISO_LOCAL_DATE))

  implicit def forOption[A : FromResultSetColumn]: FromResultSetColumn[Option[A]] = new FromResultSetColumn[Option[A]] {
    override def fromResultSetIndexedColumnUnsafe(resultSet: ResultSet, columnIndex: ColumnIndex): Option[A] =
      Option.when(resultSet.getObject(columnIndex.asJdbc) != null) {
        FromResultSetColumn[A].fromResultSetIndexedColumnUnsafe(resultSet, columnIndex)
      }
    override def fromResultSetNamedColumnUnsafe(resultSet: ResultSet, columnName: ColumnName): Option[A] =
      Option.when(resultSet.getObject(columnName.asString) != null) {
        FromResultSetColumn[A].fromResultSetNamedColumnUnsafe(resultSet, columnName)
      }
  }
}
