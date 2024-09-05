package usql

/** Encapsulates an update statement */
case class Update(sql: SqlBase) {

  /** Run the update statement */
  def run()(using c: ConnectionProvider): Int = {
    sql.withPreparedStatement(_.executeUpdate())
  }
}
