package au.id.tmm.fetch.files

import java.nio.file.Paths

import munit.{FunSuite, Location}

class NamesTest extends FunSuite {

  private def registerNaiveToPathTest(string: String, expectedPath: String)(implicit loc: Location): Unit =
    test(s"""naiveToPath("$string") == "$expectedPath"""") {
      assertEquals(Names.naiveToPath(string), Paths.get(expectedPath))
    }

  registerNaiveToPathTest("asdf", "asdf")
  registerNaiveToPathTest("as df", "as-df")
  registerNaiveToPathTest("as    df", "as-df")
  registerNaiveToPathTest("asdf.txt", "asdf-txt")
  registerNaiveToPathTest("asdf/txt", "asdf-txt")

}
