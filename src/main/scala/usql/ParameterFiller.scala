package usql

import usql.dao.SqlColumnar

import java.sql.PreparedStatement

/** Responsible for filling arguments into prepared statements for batch operations. */
trait ParameterFiller[T] {

  /** Fill something at the zero-based position index into the prepared statement. */
  def fill(offset: Int, ps: PreparedStatement, value: T): Unit

  /** Fill at position 0 */
  def fill(ps: PreparedStatement, value: T): Unit = fill(0, ps, value)

  def contraMap[U](f: U => T): ParameterFiller[U] = {
    val me = this
    new ParameterFiller[U] {
      override def fill(offset: Int, ps: PreparedStatement, value: U): Unit = me.fill(offset, ps, f(value))

      override def cardinality: Int = me.cardinality
    }
  }

  /** The number of elements set by this filler */
  def cardinality: Int
}

object ParameterFiller {

  given forTuple[H, T <: Tuple](
      using headFiller: ParameterFiller[H],
      tailFiller: ParameterFiller[T]
  ): ParameterFiller[H *: T] = new ParameterFiller[H *: T] {
    override def fill(offset: Int, ps: PreparedStatement, value: H *: T): Unit = {
      headFiller.fill(offset, ps, value.head)
      tailFiller.fill(offset + headFiller.cardinality, ps, value.tail)
    }

    override def cardinality: Int = {
      headFiller.cardinality + tailFiller.cardinality
    }
  }

  given empty: ParameterFiller[EmptyTuple] = new ParameterFiller[EmptyTuple] {
    override def fill(offset: Int, ps: PreparedStatement, value: EmptyTuple): Unit = ()

    override def cardinality: Int = 0
  }

  given forDataType[T](using dt: DataType[T]): ParameterFiller[T] = new ParameterFiller[T] {
    override def fill(offset: Int, ps: PreparedStatement, value: T): Unit = dt.fillByZeroBasedIdx(offset, ps, value)

    override def cardinality: Int = 1
  }

  given forColumnar[T](using c: SqlColumnar[T]): ParameterFiller[T] = c.parameterFiller
}
