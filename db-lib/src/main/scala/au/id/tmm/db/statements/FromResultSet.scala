package au.id.tmm.db.statements

import java.sql.ResultSet

import au.id.tmm.db.data.ColumnIndex

// TODO not crazy about this name. QueryReader? ResultSetReader?
// TODO could probably have implicit instances
trait FromResultSet[+A] {

  /**
    * Implementations must not advance the `ResultSet`.
    */
  def fromResultSetUnsafe(resultSet: ResultSet): A

  def map[B](f: A => B): FromResultSet[B] = rs => f(fromResultSetUnsafe(rs))

}

object FromResultSet extends FromResultSetForProducts {

  def apply[A : FromResultSet]: FromResultSet[A] = implicitly

  implicit val forResultSet: FromResultSet[ResultSet] = rs => rs

  implicit def forSingleColumn[A : FromResultSetColumn]: FromResultSet[A] =
    rs => FromResultSetColumn[A].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex.first)

}

private[statements] trait FromResultSetForProducts {

  // format:off
  implicit def forTuple22[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
    A17 : FromResultSetColumn,
    A18 : FromResultSetColumn,
    A19 : FromResultSetColumn,
    A20 : FromResultSetColumn,
    A21 : FromResultSetColumn,
    A22 : FromResultSetColumn,
  ]: FromResultSet[
    Tuple22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22],
  ] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
      FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
      FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
      FromResultSetColumn[A17].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(17)),
      FromResultSetColumn[A18].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(18)),
      FromResultSetColumn[A19].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(19)),
      FromResultSetColumn[A20].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(20)),
      FromResultSetColumn[A21].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(21)),
      FromResultSetColumn[A22].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(22)),
    )
  implicit def forTuple21[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
    A17 : FromResultSetColumn,
    A18 : FromResultSetColumn,
    A19 : FromResultSetColumn,
    A20 : FromResultSetColumn,
    A21 : FromResultSetColumn,
  ]: FromResultSet[
    Tuple21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21],
  ] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
      FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
      FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
      FromResultSetColumn[A17].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(17)),
      FromResultSetColumn[A18].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(18)),
      FromResultSetColumn[A19].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(19)),
      FromResultSetColumn[A20].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(20)),
      FromResultSetColumn[A21].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(21)),
    )
  implicit def forTuple20[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
    A17 : FromResultSetColumn,
    A18 : FromResultSetColumn,
    A19 : FromResultSetColumn,
    A20 : FromResultSetColumn,
  ]: FromResultSet[Tuple20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20]] =
    rs =>
      (
        FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
        FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
        FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
        FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
        FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
        FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
        FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
        FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
        FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
        FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
        FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
        FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
        FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
        FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
        FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
        FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
        FromResultSetColumn[A17].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(17)),
        FromResultSetColumn[A18].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(18)),
        FromResultSetColumn[A19].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(19)),
        FromResultSetColumn[A20].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(20)),
      )
  implicit def forTuple19[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
    A17 : FromResultSetColumn,
    A18 : FromResultSetColumn,
    A19 : FromResultSetColumn,
  ]: FromResultSet[Tuple19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19]] =
    rs =>
      (
        FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
        FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
        FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
        FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
        FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
        FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
        FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
        FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
        FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
        FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
        FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
        FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
        FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
        FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
        FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
        FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
        FromResultSetColumn[A17].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(17)),
        FromResultSetColumn[A18].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(18)),
        FromResultSetColumn[A19].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(19)),
      )
  implicit def forTuple18[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
    A17 : FromResultSetColumn,
    A18 : FromResultSetColumn,
  ]: FromResultSet[Tuple18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
      FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
      FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
      FromResultSetColumn[A17].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(17)),
      FromResultSetColumn[A18].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(18)),
    )
  implicit def forTuple17[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
    A17 : FromResultSetColumn,
  ]: FromResultSet[Tuple17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
      FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
      FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
      FromResultSetColumn[A17].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(17)),
    )
  implicit def forTuple16[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
    A16 : FromResultSetColumn,
  ]: FromResultSet[Tuple16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
      FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
      FromResultSetColumn[A16].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(16)),
    )
  implicit def forTuple15[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
    A15 : FromResultSetColumn,
  ]: FromResultSet[Tuple15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
      FromResultSetColumn[A15].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(15)),
    )
  implicit def forTuple14[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
    A14 : FromResultSetColumn,
  ]: FromResultSet[Tuple14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
      FromResultSetColumn[A14].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(14)),
    )
  implicit def forTuple13[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
    A13 : FromResultSetColumn,
  ]: FromResultSet[Tuple13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
      FromResultSetColumn[A13].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(13)),
    )
  implicit def forTuple12[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
    A12 : FromResultSetColumn,
  ]: FromResultSet[Tuple12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
      FromResultSetColumn[A12].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(12)),
    )
  implicit def forTuple11[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
    A11 : FromResultSetColumn,
  ]: FromResultSet[Tuple11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
      FromResultSetColumn[A11].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(11)),
    )
  implicit def forTuple10[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
    A10 : FromResultSetColumn,
  ]: FromResultSet[Tuple10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
      FromResultSetColumn[A10].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(10)),
    )
  implicit def forTuple9[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
    A9 : FromResultSetColumn,
  ]: FromResultSet[Tuple9[A1, A2, A3, A4, A5, A6, A7, A8, A9]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
      FromResultSetColumn[A9].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(9)),
    )
  implicit def forTuple8[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
    A8 : FromResultSetColumn,
  ]: FromResultSet[Tuple8[A1, A2, A3, A4, A5, A6, A7, A8]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
      FromResultSetColumn[A8].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(8)),
    )
  implicit def forTuple7[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
    A7 : FromResultSetColumn,
  ]: FromResultSet[Tuple7[A1, A2, A3, A4, A5, A6, A7]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
      FromResultSetColumn[A7].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(7)),
    )
  implicit def forTuple6[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
    A6 : FromResultSetColumn,
  ]: FromResultSet[Tuple6[A1, A2, A3, A4, A5, A6]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
      FromResultSetColumn[A6].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(6)),
    )
  implicit def forTuple5[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
    A5 : FromResultSetColumn,
  ]: FromResultSet[Tuple5[A1, A2, A3, A4, A5]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
      FromResultSetColumn[A5].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(5)),
    )
  implicit def forTuple4[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
    A4 : FromResultSetColumn,
  ]: FromResultSet[Tuple4[A1, A2, A3, A4]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
      FromResultSetColumn[A4].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(4)),
    )
  implicit def forTuple3[
    A1 : FromResultSetColumn,
    A2 : FromResultSetColumn,
    A3 : FromResultSetColumn,
  ]: FromResultSet[Tuple3[A1, A2, A3]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
      FromResultSetColumn[A3].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(3)),
    )
  implicit def forTuple2[A1 : FromResultSetColumn, A2 : FromResultSetColumn]: FromResultSet[Tuple2[A1, A2]] = rs =>
    (
      FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)),
      FromResultSetColumn[A2].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(2)),
    )
  implicit def forTuple1[A1 : FromResultSetColumn]: FromResultSet[Tuple1[A1]] = rs =>
    Tuple1(FromResultSetColumn[A1].fromResultSetIndexedColumnUnsafe(rs, ColumnIndex(1)))

  // format:on
}
