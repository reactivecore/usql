package usql

/** Encapsulates a batch (insert) */
case class Batch[T](sql: SqlBase, values: IterableOnce[T], filler: ParameterFiller[T]) {
  def run()(using cp: ConnectionProvider): Seq[Int] = {
    sql.withPreparedStatement { ps =>
      values.iterator.foreach { value =>
        filler.fill(ps, value)
        ps.addBatch()
      }
      val results = ps.executeBatch()
      results.toSeq
    }
  }
}
