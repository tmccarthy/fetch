package au.id.tmm.db.operations.streaming

import au.id.tmm.db.Session
import au.id.tmm.db.data.NumRowsAffected
import au.id.tmm.db.operations.{DatabaseOp, PreparedStatementUtils}
import au.id.tmm.db.statements.InsertStatement
import cats.effect.IO
import fs2.Stream.resource

final class BatchInsert[A] private (
  insertStatement: InsertStatement[A],
  stream: fs2.Stream[IO, A],
  chunkSize: Int,
) extends DatabaseOp.UsingIO[NumRowsAffected] {
  override def runIn(session: Session): IO[NumRowsAffected] = {
    val streamOfUpdateCounts: fs2.Stream[IO, Int] =
      for {
        preparedStatement <- resource(PreparedStatementUtils.psReturningGeneratedKeysFrom(session, insertStatement.sql))

        chunkOfA <- stream.chunkLimit(chunkSize)

        numRowsAffected <- fs2.Stream.evalUnChunk(IO {
          chunkOfA.foreach { a =>
            val arguments = insertStatement.toArguments.toArguments(a)
            arguments.bindToUnsafe(preparedStatement)
            preparedStatement.addBatch()
          }

          fs2.Chunk.array(preparedStatement.executeBatch())
        })
      } yield numRowsAffected

    streamOfUpdateCounts.compile
      .fold(0L)((totalUpdates, thisUpdates) => totalUpdates + thisUpdates)
      .map(NumRowsAffected.apply)
  }
}

object BatchInsert {
  val DEFAULT_CHUNK_SIZE = 50

  def apply[A](
    insertStatement: InsertStatement[A],
    stream: fs2.Stream[IO, A],
    chunkSize: Int = DEFAULT_CHUNK_SIZE,
  ): BatchInsert[A] = new BatchInsert(insertStatement, stream, chunkSize)
}
