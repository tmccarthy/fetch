package au.id.tmm.db

import java.net.URI
import java.sql.Connection

import au.id.tmm.db.Database.logger
import au.id.tmm.db.Session.{CommitBehaviour, ReadMode, TransactionIsolation}
import au.id.tmm.db.operations.DatabaseOp
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.kernel.Resource.ExitCase
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.slf4j.{Logger, LoggerFactory}

// TODO some transaction support
class Database private (private val connectionPool: HikariDataSource) extends FluentDbOps {

  override def run[F[_], A](dbOp: DatabaseOp[F, A]): F[A] = dbOp.runOn(this)

  def inTransaction[A](use: Session => IO[A]): IO[A] =
    session().use(use)

  def inReadOnlyTransaction[A](use: Session => IO[A]): IO[A] =
    session(CommitBehaviour.CommitAtEndOfTransaction, ReadMode.ReadOnly).use(use)

  def session(
    commitBehaviour: CommitBehaviour = CommitBehaviour.CommitAtEndOfTransaction,
    readMode: ReadMode = ReadMode.ReadWrite,
    transactionIsolation: Option[TransactionIsolation] = None,
  ): Resource[IO, Session] =
    Resource
      .makeCase(
        acquire = IO {
          val connection = connectionPool.getConnection()

          if (connection.getMetaData.getDriverName == "SQLite JDBC") {
            logger.warn("Cannot set read-only for sqlite")
          } else {
            connection.setReadOnly(readMode match {
              case ReadMode.ReadOnly  => true
              case ReadMode.ReadWrite => false
            })
          }
          connection.setAutoCommit(commitBehaviour match {
            case CommitBehaviour.AutoCommit               => true
            case CommitBehaviour.CommitAtEndOfTransaction => false
          })
          transactionIsolation.foreach(t => connection.setTransactionIsolation(t.jdbcFlag))

          connection
        },
      )(
        release = (connection, exitCase) =>
          for {
            _ <- finaliseTransaction(connection, exitCase)
            _ <- IO(connection.close())
          } yield (),
      )
      .map(new Session(_))

  // TODO should probably do something where we provide a bit more information in the stacktrace
  private def finaliseTransaction(connection: Connection, exitCase: ExitCase): IO[Unit] =
    exitCase match {
      case Resource.ExitCase.Succeeded                               => IO(connection.commit())
      case Resource.ExitCase.Errored(_) | Resource.ExitCase.Canceled => IO(connection.rollback())
    }

}

object Database {

  private val logger: Logger = LoggerFactory.getLogger(classOf[Database])

  private[db] val READ_CHUNK_SIZE       = 2000
  private[db] val BULK_WRITE_CHUNK_SIZE = 50

  def apply(
    jdbcUrl: URI,
    username: String,
    password: String,
    dataSourceProperties: Map[String, String] = Map.empty,
  ): Resource[IO, Database] = Resource
    .make[IO, Database](
      acquire = for {
        poolConfig <- IO.pure {
          val config = new HikariConfig()

          config.setJdbcUrl(jdbcUrl.toString)
          config.setUsername(username)
          config.setPassword(password)
          dataSourceProperties.foreach { case (key, value) =>
            config.addDataSourceProperty(key, value)
          }

          config
        }
        connectionPool <- IO(new HikariDataSource(poolConfig))

        _ <- IO(connectionPool.getConnection.nativeSQL("SELECT 1;"))
      } yield new Database(connectionPool),
    )(release = db => IO(db.connectionPool.close()))

}
