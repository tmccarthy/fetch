package au.id.tmm.fetch.cache

import au.id.tmm.utilities.errors.ExceptionOr
import cats.data.NonEmptyList
import munit.{FunSuite, Location}
import sttp.model.Uri

class KeySchemesTest extends FunSuite {

  private def defineTest(
    uri: Uri,
    expectedHead: String,
    expectedTail: String*,
  )(implicit
    loc: Location,
  ): Unit =
    defineTest(uri, Right(NonEmptyList.of(expectedHead, expectedTail: _*)))(loc)

  private def defineTest(uri: Uri, expected: ExceptionOr[NonEmptyList[String]])(implicit loc: Location): Unit = {
    val expectedDesc = expected match {
      case Right(nel) => nel.toList.mkString("[\"", "\", \"", "\"]")
      case Left(e)    => e.toString
    }

    test(s"naiveUriAsPathComponents($uri) = $expectedDesc") {
      assertEquals(KeySchemes.naiveUriAsPathComponents(uri), expected)
    }
  }

  defineTest(Uri.unsafeParse("https://example.com/path/file.txt"), "example.com", "file.txt")
  defineTest(Uri.unsafeParse("https://example.com/file.txt"), "example.com", "file.txt")
  defineTest(Uri.unsafeParse("https://example.com/"), "example.com", "root")
  defineTest(Uri.unsafeParse("https://example.com/root"), "example.com", "root")
  defineTest(Uri.unsafeParse("https://example.com/path/file.txt?param=value"), "example.com", "file.txt")

}
