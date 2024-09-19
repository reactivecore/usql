package usql

import usql.Update.SqlResultMissingGenerated

import java.sql.SQLException
import scala.util.Using

/** Encapsulates an update statement */
case class Update(sql: SqlBase) {

  /** Run the update statement */
  def run()(using c: ConnectionProvider): Int = {
    sql.withPreparedStatement(_.executeUpdate())
  }

  /**
   * Run the update statement and get generated values. See [[java.sql.PreparedStatement.getGeneratedKeys()]]
   */
  def runAndGetGenerated[T]()(using d: ResultRowDecoder[T], c: ConnectionProvider): T = {
    given sp: StatementPreparator = StatementPreparator.withGeneratedKeys
    sql.withPreparedStatement { statement =>
      statement.executeUpdate()
      Using.resource(statement.getGeneratedKeys) { resultSet =>
        if (resultSet.next()) {
          d.parseRow(resultSet)
        } else {
          throw new SqlResultMissingGenerated("Missing row for getGeneratedKeys")
        }
      }
    }
  }
}

object Update {

  /** Exception thrown if the result set has no generated data. */
  class SqlResultMissingGenerated(msg: String) extends SQLException(msg)
}
