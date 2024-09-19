package usql

import java.sql.{Connection, PreparedStatement, Statement}

/** Something which can create prepared statements. */
trait SqlBase {

  /** Prepares a statement which can then be further filled or executed. */
  def withPreparedStatement[T](
      f: PreparedStatement => T
  )(using cp: ConnectionProvider, prep: StatementPreparator = StatementPreparator.default): T

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

/** Hook for changing the preparation of SQL. */
trait StatementPreparator {
  def prepare(connection: Connection, sql: String): PreparedStatement
}

object StatementPreparator {

  /** Default Implementation */
  object default extends StatementPreparator {
    override def prepare(connection: Connection, sql: String): PreparedStatement = {
      connection.prepareStatement(sql)
    }
  }

  /** Statement should return generated keys */
  object withGeneratedKeys extends StatementPreparator {
    override def prepare(connection: Connection, sql: String): PreparedStatement = {
      connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    }
  }
}

/** With supplied arguments */
case class AppliedSql[T](base: SqlBase, parameter: T, parameterFiller: ParameterFiller[T]) extends SqlBase {
  override def withPreparedStatement[T](
      f: PreparedStatement => T
  )(using cp: ConnectionProvider, sp: StatementPreparator): T = {
    base.withPreparedStatement { ps =>
      parameterFiller.fill(ps, parameter)
      f(ps)
    }
  }
}
