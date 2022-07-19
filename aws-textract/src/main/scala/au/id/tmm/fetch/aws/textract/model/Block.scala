package au.id.tmm.fetch.aws.textract.model

import au.id.tmm.fetch.aws.textract.model.SelectionElement.Status
import io.circe.syntax.{EncoderOps, KeyOps}
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}

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

    implicit val codec: Codec[Child] = Codec.from(
      decodeA = c =>
        c.keys.map(_.toList) match {
          case Some(onlyKey :: Nil) =>
            onlyKey match {
              case "line"        => c.get[Line]("line").map(OfLine)
              case "table"       => c.get[Table]("table").map(OfTable)
              case "keyValueSet" => c.get[KeyValueSet]("keyValueSet").map(OfKeyValueSet)
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
) extends Block {
  def rows: ArraySeq[ArraySeq[Table.Cell]] =
    children
      .sortBy(c => (c.rowIndex, c.columnIndex))
      .groupBy(c => c.rowIndex)
      .to(ArraySeq)
      .sortBy(_._1)
      .map(_._2)
}

object Table {

  final case class Cell(
    id: BlockId,
    pageNumber: PageNumber,
    geometry: Geometry,
    columnIndex: Int,
    columnSpan: Int,
    rowIndex: Int,
    rowSpan: Int,
    children: ArraySeq[AtomicBlock],
  ) extends Block
      with ReadableText {
    override def readableText: String = ReadableText.from(children)
  }

  object Cell {
    implicit val codec: Codec[Cell] = Codec.from(
      Decoder.forProduct8(
        "id",
        "pageNumber",
        "geometry",
        "columnIndex",
        "columnSpan",
        "rowIndex",
        "rowSpan",
        "children",
      )(Cell.apply),
      Encoder.forProduct8(
        "id",
        "pageNumber",
        "geometry",
        "columnIndex",
        "columnSpan",
        "rowIndex",
        "rowSpan",
        "children",
      )(c => (c.id, c.pageNumber, c.geometry, c.columnIndex, c.columnSpan, c.rowIndex, c.rowSpan, c.children)),
    )
  }

  implicit val codec: Codec[Table] = Codec.from(
    Decoder.forProduct4("id", "pageNumber", "geometry", "children")(Table.apply),
    Encoder.forProduct4("id", "pageNumber", "geometry", "children")(t => (t.id, t.pageNumber, t.geometry, t.children)),
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
