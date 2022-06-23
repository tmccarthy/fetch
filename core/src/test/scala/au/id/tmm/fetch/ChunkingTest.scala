package au.id.tmm.fetch

import au.id.tmm.fetch.files.Chunking
import munit.FunSuite

class ChunkingTest extends FunSuite {

  test("breaking by headings") {
    val lines = fs2.Stream.emits(
      Vector(
        "dead1",
        "dead2",
        "header1",
        "body1",
        "header2",
        "body2",
        "body3",
        "header3",
        "body4",
        "body5",
        "header4",
        "body6",
        "body7",
        "body8",
      ),
    )

    val actualResult = lines
      .through(Chunking.breakByHeadings(_.startsWith("header")))
      .map { case (header, bodyStream) =>
        header -> bodyStream.toVector
      }
      .toVector

    val expectedResult = Vector(
      "header1" -> Vector(
        "body1",
      ),
      "header2" -> Vector(
        "body2",
        "body3",
      ),
      "header3" -> Vector(
        "body4",
        "body5",
      ),
      "header4" -> Vector(
        "body6",
        "body7",
        "body8",
      ),
    )

    assertEquals(actualResult, expectedResult)
  }

  test("heading doesn't appear") {
    val lines = fs2.Stream.emits(
      Vector(
        "dead1",
        "dead2",
      ),
    )

    val result = lines.through(Chunking.breakByHeadings(_.startsWith("header"))).toVector

    assertEquals(result, Vector.empty)
  }

}
