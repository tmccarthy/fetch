package au.id.tmm.fetch

package object json extends Codecs {
  type UnrecognisedStringOr[A] = UnrecognisedOr[String, A]
}
