package usql

import java.sql.Connection
import scala.util.control.NonFatal

/** Provider for JDBC Connections. */
trait ConnectionProvider {

  /** Run some code using this connection. */
  def withConnection[T](f: Connection ?=> T): T
}

/** Helper for building transactions. */
def transaction[T](using cp: ConnectionProvider)(f: ConnectionProvider ?=> T): T = {
  cp.withConnection {
    val c = summon[Connection]

    val oldAutoCommit = c.getAutoCommit
    c.setAutoCommit(false)
    try {
      val res = f(using ConnectionProvider.forConnection(using c))
      c.commit()
      res
    } catch {
      case NonFatal(e) =>
        c.rollback()
        throw e
    } finally {
      c.setAutoCommit(oldAutoCommit)
    }
  }
}

object ConnectionProvider {
  given forConnection(using c: Connection): ConnectionProvider with {
    override def withConnection[T](f: Connection ?=> T): T = {
      f(using c)
    }
  }
}
