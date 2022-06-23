package au.id.tmm.db.operations.streaming

import au.id.tmm.db.Database
import au.id.tmm.db.operations.DatabaseOp
import cats.effect.IO

trait StreamingDatabaseOp[A] extends DatabaseOp[fs2.Stream[IO, *], A] {
  override def runOn(database: Database): fs2.Stream[IO, A] =
    fs2.Stream.resource(database.session()).flatMap(session => runIn(session))
}
