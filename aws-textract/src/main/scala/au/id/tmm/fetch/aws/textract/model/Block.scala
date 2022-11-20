package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.fetch.aws.textract.model.SelectionElement.Status
import io.circe._
import io.circe.syntax.{EncoderOps, KeyOps}

import scala.collection.immutable.ArraySeq

sealed trait HasBlockId {
  val id: BlockId
}

sealed trait ReadableText {
  def readableText: String

  override def toString: String = readableText
}

object ReadableText {
  def from(others: ArraySeq[ReadableText]): String = others.map(_.readableText).mkString(" ")
}

sealed trait Block extends HasBlockId {
  val pageNumber: PageNumber
  val geometry: Geometry
}

sealed trait AtomicBlock extends Block with ReadableText

object AtomicBlock {
  implicit val codec: Codec[AtomicBlock] = Codec.from(
    decodeA = c =>
      for {
        typeFlag <- c.get[String]("type")
        atomicBloc: AtomicBlock <- typeFlag match {
          case "SelectionElement" => c.as[SelectionElement]
          case "Word"             => c.as[Word]
          case _                  => Left(DecodingFailure("Unrecognised type", c.history))
        }
      } yield atomicBloc,
    encodeA = {
      case s: SelectionElement => s.asJsonObject.+:("type" := "SelectionElement").asJson
      case w: Word             => w.asJsonObject.+:("type" := "Word").asJson
    },
  )
}

final case class Line(
  id: BlockId,
  pageNumber: PageNumber,
  geometry: Geometry,
  text: String,
  children: ArraySeq[AtomicBlock],
) extends Block
    with ReadableText {
  override def readableText: String = text
}

object Line {
  implicit val codec: Codec[Line] = Codec.from(
    Decoder.forProduct5("id", "pageNumber", "geometry", "text", "children")(Line.apply),
    Encoder.forProduct5("id", "pageNumber", "geometry", "text", "children")(l =>
      (l.id, l.pageNumber, l.geometry, l.text, l.children),
    ),
  )
}

final case class Page(
  id: BlockId,
  pageNumber: PageNumber,
  geometry: Geometry,
  children: ArraySeq[Page.Child],
) extends Block {
  def lines: ArraySeq[Line] =
    children.collect { case Page.Child.OfLine(l) =>
      l
    }

  def tables: ArraySeq[Table] =
    children.collect { case Page.Child.OfTable(t) =>
      t
    }

  def keyValueSets: ArraySeq[KeyValueSet] =
    children.collect { case Page.Child.OfKeyValueSet(kvSet) =>
      kvSet
    }
}

object Page {
  sealed trait Child

  object Child {
    final case class OfLine(line: Line)                      extends Child
    final case class OfTable(table: Table)                   extends Child
    final case class OfKeyValueSet(keyValueSet: KeyValueSet) extends Child

    implicit val codec: Codec[Child] = Codec.from[Child](
      decodeA = c =>
        c.keys.map(_.toList) match {
          case Some(onlyKey :: Nil) =>
            onlyKey match {
              case "line"        => c.get[Line]("line").map(OfLine.apply)
              case "table"       => c.get[Table]("table").map(OfTable.apply)
              case "keyValueSet" => c.get[KeyValueSet]("keyValueSet").map(OfKeyValueSet.apply)
            }
          case None | Some(_) => Left(DecodingFailure("Bad keys", c.history))
        },
      encodeA = {
        case OfLine(line)               => Json.obj("line" := line)
        case OfTable(table)             => Json.obj("table" := table)
        case OfKeyValueSet(keyValueSet) => Json.obj("keyValueSet" := keyValueSet)
      },
    )
  }

  implicit val ordering: Ordering[Page] = Ordering.by(_.pageNumber)

  implicit val codec: Codec[Page] = Codec.from(
    Decoder.forProduct4("id", "pageNumber", "geometry", "children")(Page.apply),
    Encoder.forProduct4("id", "pageNumber", "geometry", "children")(p => (p.id, p.pageNumber, p.geometry, p.children)),
  )
}

final case class SelectionElement(
  id: BlockId,
  pageNumber: PageNumber,
  geometry: Geometry,
  status: SelectionElement.Status,
) extends AtomicBlock {
  override def readableText: String =
    status match {
      case Status.Selected    => "☑"
      case Status.NotSelected => "☐"
    }
}

object SelectionElement {
  sealed abstract class Status(val isSelected: Boolean)

  object Status {
    case object Selected    extends Status(isSelected = true)
    case object NotSelected extends Status(isSelected = false)

    implicit val codec: Codec[Status] = Codec.from(
      c =>
        c.get[Boolean]("isSelected").map {
          case true  => Selected
          case false => NotSelected
        },
      {
        case Selected    => Json.obj("isSelected" := true)
        case NotSelected => Json.obj("isSelected" := false)
      },
    )
  }

  implicit val decoder: Decoder[SelectionElement] =
    Decoder.forProduct4("id", "pageNumber", "geometry", "status")(SelectionElement.apply)
  implicit val encoder: Encoder.AsObject[SelectionElement] =
    Encoder.forProduct4("id", "pageNumber", "geometry", "status")(s => (s.id, s.pageNumber, s.geometry, s.status))
}

final case class Table(
  id: BlockId,
  pageNumber: PageNumber,
  geometry: Geometry,
  children: ArraySeq[Table.Cell],
  mergedCells: ArraySeq[Table.MergedCell],
) extends Block {
  def rows: ArraySeq[ArraySeq[Table.Cell]] =
    children
      .sortBy(c => (c.index.row, c.index.column))
      .groupBy(c => c.index.row)
      .to(ArraySeq)
      .sortBy(_._1)
      .map(_._2)
}

object Table {

  final case class Cell(
    id: BlockId,
    pageNumber: PageNumber,
    geometry: Geometry,
    index: Cell.Index,
    children: ArraySeq[AtomicBlock],
  ) extends Block
      with ReadableText {
    override def readableText: String = ReadableText.from(children)
  }

  object Cell {
    final case class Index(
      row: Int,
      column: Int,
    )

    implicit val codec: Codec[Cell] = Codec.from(
      Decoder.instance { c =>
        for {
          id          <- c.get[BlockId]("id")
          pageNumber  <- c.get[PageNumber]("pageNumber")
          geometry    <- c.get[Geometry]("geometry")
          columnIndex <- c.get[Int]("columnIndex")
          columnSpan  <- c.get[Int]("columnSpan")
          rowIndex    <- c.get[Int]("rowIndex")
          rowSpan     <- c.get[Int]("rowSpan")
          children    <- c.get[ArraySeq[AtomicBlock]]("children")

          _ <- Either.cond(columnSpan == 1, (), DecodingFailure(s"Bad column span $columnSpan", c.history))
          _ <- Either.cond(rowSpan == 1, (), DecodingFailure(s"Bad row span $rowSpan", c.history))
        } yield Cell(id, pageNumber, geometry, Index(rowIndex, columnIndex), children)
      },
      Encoder.forProduct6(
        "id",
        "pageNumber",
        "geometry",
        "columnIndex",
        "rowIndex",
        "children",
      )(c => (c.id, c.pageNumber, c.geometry, c.index.column, c.index.row, c.children)),
    )
  }

  final case class MergedCell(
    id: BlockId,
    pageNumber: PageNumber,
    geometry: Geometry,
    index: Cell.Index,
    span: MergedCell.Span,
    children: ArraySeq[Cell],
  ) extends Block
      with ReadableText {
    override def readableText: String = ReadableText.from(children)
  }

  object MergedCell {
    final case class Span(
      row: Int,
      column: Int,
    )

    object Span {
      implicit val codec: Codec[Span] = Codec.from(
        Decoder.forProduct2("row", "column")(Span.apply),
        Encoder.forProduct2("row", "column")(s => (s.row, s.column)),
      )
    }

    private implicit val indexCodec: Codec[Cell.Index] = Codec.from(
      Decoder.forProduct2("row", "column")(Cell.Index.apply),
      Encoder.forProduct2("row", "column")(s => (s.row, s.column)),
    )

    implicit val codec: Codec[MergedCell] = Codec.from(
      Decoder.forProduct6(
        "id",
        "pageNumber",
        "geometry",
        "index",
        "span",
        "children",
      )(MergedCell.apply),
      Encoder.forProduct6(
        "id",
        "pageNumber",
        "geometry",
        "index",
        "span",
        "children",
      )(c => (c.id, c.pageNumber, c.geometry, c.index, c.span, c.children)),
    )
  }

  implicit val codec: Codec[Table] = Codec.from(
    Decoder.forProduct5("id", "pageNumber", "geometry", "children", "mergedCells") {
      (
        id: BlockId,
        pageNumber: PageNumber,
        geometry: Geometry,
        children: ArraySeq[Cell],
        mergedCells: Option[ArraySeq[MergedCell]],
      ) =>
        Table(
          id,
          pageNumber,
          geometry,
          children,
          mergedCells.getOrElse(ArraySeq.empty), // TODO should probably remove the forgiving mergedCells decoding
        )
    },
    Encoder.forProduct5("id", "pageNumber", "geometry", "children", "mergedCells")(t =>
      (t.id, t.pageNumber, t.geometry, t.children, t.mergedCells),
    ),
  )

}

final case class Word(
  id: BlockId,
  pageNumber: PageNumber,
  geometry: Geometry,
  text: String,
  confidence: Confidence,
  textType: Word.TextType,
) extends AtomicBlock {
  override def readableText: String = text
}

object Word {
  sealed trait TextType

  object TextType {
    case object Printed     extends TextType
    case object Handwriting extends TextType

    implicit val codec: Codec[TextType] = Codec.from(
      Decoder[String].emap {
        case "printed"     => Right(Printed)
        case "handwriting" => Right(Handwriting)
        case badTextType   => Left(s"Bad text type $badTextType")
      },
      Encoder[String].contramap {
        case Printed     => "printed"
        case Handwriting => "handwriting"
      },
    )
  }

  implicit val decoder: Decoder[Word] =
    Decoder.forProduct6("id", "pageNumber", "geometry", "text", "confidence", "textType")(Word.apply)
  implicit val encoder: Encoder.AsObject[Word] =
    Encoder.forProduct6("id", "pageNumber", "geometry", "text", "confidence", "textType")(w =>
      (w.id, w.pageNumber, w.geometry, w.text, w.confidence, w.textType),
    )
}

final case class KeyValueSet(
  key: KeyValueSet.Key,
  value: KeyValueSet.Value,
)

object KeyValueSet {
  final case class Key(
    id: BlockId,
    pageNumber: PageNumber,
    geometry: Geometry,
    children: ArraySeq[AtomicBlock],
  ) extends Block
      with ReadableText {
    override def readableText: String = ReadableText.from(children)
  }

  object Key {
    implicit val codec: Codec[Key] = Codec.from(
      Decoder.forProduct4("id", "pageNumber", "geometry", "children")(Key.apply),
      Encoder.forProduct4("id", "pageNumber", "geometry", "children")(k => (k.id, k.pageNumber, k.geometry, k.children)),
    )
  }

  final case class Value(
    id: BlockId,
    pageNumber: PageNumber,
    geometry: Geometry,
    children: ArraySeq[AtomicBlock],
  ) extends Block
      with ReadableText {
    override def readableText: String = ReadableText.from(children)
  }

  object Value {
    implicit val codec: Codec[Value] = Codec.from(
      Decoder.forProduct4("id", "pageNumber", "geometry", "children")(Value.apply),
      Encoder.forProduct4("id", "pageNumber", "geometry", "children")(v => (v.id, v.pageNumber, v.geometry, v.children)),
    )
  }

  implicit val codec: Codec[KeyValueSet] = Codec.from(
    Decoder.forProduct2("key", "value")(KeyValueSet.apply),
    Encoder.forProduct2("key", "value")(kvs => (kvs.key, kvs.value)),
  )
}
