package au.id.tmm.fetch.osm.model.common

import io.circe.Decoder

sealed trait OsmType {
  def singleLetterCode: Char = this match {
    case OsmType.Relation => 'R'
    case OsmType.Node     => 'N'
    case OsmType.Way      => 'W'
  }
}

object OsmType {
  final case object Relation extends OsmType
  final case object Node     extends OsmType
  final case object Way      extends OsmType

  implicit val decoder: Decoder[OsmType] = Decoder[String].emap {
    case "relation" => Right(Relation)
    case "node"     => Right(Node)
    case "way"      => Right(Way)
    case badType    => Left(s"Bad OsmType $badType")
  }
}
