package au.id.tmm.fetch.files

import scala.collection.immutable.ArraySeq

object Bytes {

  def toByteArrayUnsafe(arraySeq: ArraySeq[Byte]): Array[Byte] = arraySeq match {
    case bytes: ArraySeq.ofByte => bytes.unsafeArray
    case bytes => {
      val bytesArray = new Array[Byte](bytes.size)

      //noinspection ScalaUnusedExpression
      bytes.copyToArray(bytesArray)

      bytesArray
    }
  }

}
