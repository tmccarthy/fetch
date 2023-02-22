package au.id.tmm.fetch.files

import java.nio.file.{Path, Paths}

object Names {

  def naiveToPath(string: String): Path = Paths.get(string.replaceAll("[\\W-]+", "-"))

}
