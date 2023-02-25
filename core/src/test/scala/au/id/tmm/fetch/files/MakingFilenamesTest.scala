package au.id.tmm.fetch.files

import munit.{FunSuite, Location}

class MakingFilenamesTest extends FunSuite {

  private def registerNaiveToPathTest(string: String, expectedPath: String)(implicit loc: Location): Unit =
    test(s"""naiveToPath("$string") == "$expectedPath"""") {
      assertEquals(MakingFilenames.makePathSafe(string), expectedPath)
    }

  registerNaiveToPathTest("asdf", "asdf")
  registerNaiveToPathTest("as df", "as-df")
  registerNaiveToPathTest("as    df", "as-df")
  registerNaiveToPathTest("asdf.txt", "asdf-txt")
  registerNaiveToPathTest("asdf/txt", "asdf-txt")

}
