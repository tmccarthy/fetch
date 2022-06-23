package au.id.tmm.db.helpers

import au.id.tmm.db.helpers.Insertable.Column
import au.id.tmm.db.statements.{Argument, ArgumentBinder, ColumnName, TableName}
import cats.Contravariant

import scala.collection.immutable.ArraySeq

// TODO document how this relates to ToArguments
// TODO document that this is useful in the case where there's a 1-1 mapping between columns and db fields
// TODO find some way to manage 1-many mapping between columns and db fields
final case class Insertable[-A](tableName: TableName, columns: ArraySeq[Column[A]]) {

  def contramap[B](f: B => A): Insertable[B] = this.copy[B](columns = this.columns.map(_.contramap(f)))

}

object Insertable {
  def apply[A : Insertable]: Insertable[A] = implicitly

  def apply[A](
    tableName: TableName,
    columns: Column[A]*, // TODO the type inference here isn't working how I would expect
  ): Insertable[A] = Insertable(tableName, columns.to(ArraySeq))

  sealed trait Column[-A] {
    def name: ColumnName
    def extractor: Column.Extractor[A]
    def contramap[B](f: B => A): Column[B] = Column.ContraMapped(this, f)
  }

  object Column {

    def apply[A, B : ArgumentBinder](name: ColumnName, extractArgument: A => B): Column[A] =
      Of(name, a => Argument(extractArgument(a)))
    def apply[A](name: ColumnName, extractor: Extractor[A]): Column[A] = Of(name, extractor)

    final case class Of[-A](name: ColumnName, extractor: Extractor[A]) extends Column[A]
    final case class ContraMapped[-A, A_UNDERLYING](underlying: Column[A_UNDERLYING], contramap: A => A_UNDERLYING)
        extends Column[A] {
      override def name: ColumnName        = underlying.name
      override def extractor: Extractor[A] = underlying.extractor.contramap(contramap)
    }

    trait Extractor[-A] {
      def toArgument(a: A): Argument[_]

      def contramap[B](f: B => A): Extractor[B] = (b: B) => this.toArgument(f(b))
    }

    object Extractor {
      implicit val contravariant: Contravariant[Extractor] = new Contravariant[Extractor] {
        override def contramap[A, B](fa: Extractor[A])(f: B => A): Extractor[B] = fa.contramap(f)
      }
    }

    implicit val contravariant: Contravariant[Column] = new Contravariant[Column] {
      override def contramap[A, B](fa: Column[A])(f: B => A): Column[B] = fa.contramap(f)
    }
  }

  implicit val contravariant: Contravariant[Insertable] = new Contravariant[Insertable] {
    override def contramap[A, B](fa: Insertable[A])(f: B => A): Insertable[B] = fa.contramap(f)
  }
}
