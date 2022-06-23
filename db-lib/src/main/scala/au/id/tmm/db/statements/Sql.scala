package au.id.tmm.db.statements

import cats.Monoid

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

/**
  * Some SQL, parameterised with some [[Arguments]].
  */
// TODO need to figure out a way to have a "partially constructed" one of these, where some of the ? are bound, but
//  others arent until we actually run the query.
// TODO this needs a bunch of tests
final case class Sql(raw: String, arguments: Arguments) {
  // TODO join

  def asUpdateStatement: UpdateStatement = UpdateStatement(this)
  def asQueryStatement[A](implicit fromResultSet: FromResultSet[A]): QueryStatement[A] =
    QueryStatement(this, fromResultSet)
  def asInsertStatement[A](implicit toArguments: ToArguments[A]): InsertStatement[A] =
    InsertStatement(this, toArguments)

  def append(that: Sql): Sql = Sql.Builder.from(this).append(that).result()

  def +(that: Sql): Sql = append(that)

  def prepend(that: Sql): Sql = that.append(this)

}

object Sql {

  val empty: Sql = Sql("", Arguments.empty)

  def apply(raw: String): Sql = Sql(raw, Arguments.empty)

  def mkSql[A](components: Seq[A], sep: Sql)(implicit ev: A => Sql.Component): Sql =
    mkSql(components, empty, sep, empty)

  def mkSql[A](
    components: Seq[A],
    start: Sql,
    sep: Sql,
    end: Sql,
  )(implicit
    ev: A => Sql.Component,
  ): Sql =
    if (components.isEmpty) {
      start + end
    } else {
      val firstComponent = components.head

      val remainingComponents = components.tail

      val builder = Builder.from(start)

      builder.append(firstComponent)

      remainingComponents.foreach { component =>
        builder.append(sep)
        builder.append(component)
      }

      builder.append(end)

      builder.result()
    }

  private[db] trait Syntax {
    implicit class SqlStringContext(private val sc: StringContext) {
      def sql(args: Sql.Component*): Sql = {
        val builder: Sql.Builder = Sql.Builder.from(Sql(sc.parts.head))

        (args zip sc.parts.tail).foreach { case (arg, followingString) =>
          builder.append(arg)
          builder.appendRawSql(followingString)
        }

        builder.result()
      }
    }

    implicit class JoinOps[A](private val components: Seq[A])(implicit ev: A => Sql.Component) {
      def mkSql(sep: Sql): Sql = Sql.mkSql(components, sep)
      def mkSql(
        start: Sql,
        sep: Sql,
        end: Sql,
      ): Sql = Sql.mkSql(components, start, sep, end)

      def joinWithCommas: Sql = Sql.mkSql(components, Sql(", "))
    }
  }

  private final class Builder private (
    private val rawSqlBuilder: mutable.StringBuilder = new mutable.StringBuilder(),
    private val argumentsBuilder: mutable.Builder[Argument[_], ArraySeq[Argument[_]]] = ArraySeq.newBuilder,
  ) {
    private[Sql] def appendRawSql(rawSql: String): Unit =
      rawSqlBuilder.append(rawSql)

    def append[A](component: A)(implicit ev: A => Sql.Component): Builder = {
      (component: Sql.Component) match {
        case Sql.Component.OfArgument(argument) => {
          appendRawSql("?")
          argumentsBuilder.addOne(argument)
        }
        case Sql.Component.OfSymbol(symbol) => rawSqlBuilder.append(symbolAsSql(symbol))
        case Sql.Component.OfSql(sql) => {
          appendRawSql(sql.raw)
          argumentsBuilder.addAll(sql.arguments.asSeq)
        }
      }

      this
    }

    private def symbolAsSql(symbol: DbSymbol): String = symbol match {
      case ColumnName(asString)                                => asString
      case TableName(asString)                                 => asString
      case ColumnOnTable(TableName(table), ColumnName(column)) => s"$table.$column"
    }

    def appendAll[A](components: Seq[A])(implicit ev: A => Sql.Component): Builder = {
      components.foreach(append[A])
      this
    }

    def result(): Sql = Sql(rawSqlBuilder.result(), Arguments(argumentsBuilder.result()))
  }

  private object Builder {
    def empty: Builder = new Builder()

    def from(sql: Sql): Builder = new Builder(
      new mutable.StringBuilder(sql.raw),
      ArraySeq.newBuilder,
    )
  }

  sealed trait Component

  object Component {
    final case class OfArgument(argument: Argument[_]) extends Component
    final case class OfSymbol(symbol: DbSymbol)        extends Component
    final case class OfSql(sql: Sql)                   extends Component

    implicit def of(argument: Argument[_]): Component     = OfArgument(argument)
    implicit def ofA[A : ArgumentBinder](a: A): Component = OfArgument(Argument(a))
    implicit def of(dbSymbol: DbSymbol): Component        = OfSymbol(dbSymbol)
    implicit def of(sql: Sql): Component                  = OfSql(sql)
  }

  implicit val monoid: Monoid[Sql] = new Monoid[Sql] {
    override def empty: Sql = Sql.empty

    override def combine(left: Sql, right: Sql): Sql = left + right

    override def combineAll(as: IterableOnce[Sql]): Sql = {
      val builder = Builder.empty

      as.iterator.foreach(builder.append[Sql])

      builder.result()
    }
  }

}
