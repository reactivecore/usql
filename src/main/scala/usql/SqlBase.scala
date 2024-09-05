package usql

import java.sql.PreparedStatement

/** Something which can create prepared statements. */
trait SqlBase {

  /** Prepares a statement which can then be further filled or executed. */
  def withPreparedStatement[T](f: PreparedStatement => T)(using cp: ConnectionProvider): T

  /** Turns into a query */
  def query: Query = Query(this)

  /** Turns into an update. */
  def update: Update = {
    Update(this)
  }

  /** Turns into a update on one value set. */
  def one[T](value: T)(using p: ParameterFiller[T]): AppliedSql[T] = {
    AppliedSql(this, value, p)
  }

  /** Turns into a batch operation */
  def batch[T](values: Iterable[T])(using p: ParameterFiller[T]): Batch[T] = {
    Batch(this, values, p)
  }

  /** Raw Executes this statement. */
  def execute()(using ConnectionProvider): Boolean = {
    withPreparedStatement(_.execute())
  }
}

/** With supplied arguments */
case class AppliedSql[T](base: SqlBase, parameter: T, parameterFiller: ParameterFiller[T]) extends SqlBase {
  override def withPreparedStatement[T](f: PreparedStatement => T)(using cp: ConnectionProvider): T = {
    base.withPreparedStatement { ps =>
      parameterFiller.fill(ps, parameter)
      f(ps)
    }
  }
}
