package au.id.tmm.db.operations.streaming

import java.sql.ResultSet

import au.id.tmm.db.Database
import au.id.tmm.db.statements.FromResultSet
import cats.effect.IO

import scala.collection.immutable.ArraySeq

private[streaming] object StreamingResultSetUtils {
  def streamResultSet[A](fromResultSet: FromResultSet[A], resultSet: ResultSet): fs2.Stream[IO, A] =
    fs2.Stream.unfoldChunkEval[IO, ResultSet, A](resultSet)(rs =>
      IO {
        val builder          = ArraySeq.untagged.newBuilder[A]
        var countResultsRead = 0

        builder.sizeHint(Database.READ_CHUNK_SIZE)

        while (countResultsRead < Database.READ_CHUNK_SIZE && resultSet.next()) {
          builder.addOne(fromResultSet.fromResultSetUnsafe(resultSet))
          countResultsRead = countResultsRead + 1
        }

        if (countResultsRead > 0) {
          Some(fs2.Chunk.arraySeq(builder.result()), rs)
        } else {
          None
        }
      },
    )
}
