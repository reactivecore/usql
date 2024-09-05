package usql

import java.sql.{Connection, PreparedStatement}
import scala.util.Using

/** Raw SQL Query string. */
case class RawSql(sql: String) extends SqlBase {
  override def withPreparedStatement[T](f: PreparedStatement => T)(using cp: ConnectionProvider): T = {
    cp.withConnection {
      val c = summon[Connection]
      Using.resource(c.prepareStatement(sql)) { statement =>
        f(statement)
      }
    }
  }
}
