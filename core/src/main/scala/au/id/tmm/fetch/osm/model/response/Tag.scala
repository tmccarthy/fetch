package au.id.tmm.fetch.osm.model.response

final case class Tag(key: Tag.Key, value: Tag.Value)

object Tag {
  final case class Key(asString: String)   extends AnyVal
  final case class Value(asString: String) extends AnyVal
}
