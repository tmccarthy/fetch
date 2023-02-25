package au.id.tmm.fetch.files

object MakingFilenames {

  def makePathSafe(string: String): String = string.replaceAll("[\\W-]+", "-")

}
