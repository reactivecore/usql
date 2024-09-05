package usql.dao

import usql.{DataType, ParameterFiller, ResultRowDecoder, SqlIdentifier, SqlIdentifiers}

import scala.deriving.Mirror

/** Maps some thing to a whole table */
trait SqlTabular[T] extends SqlColumnar[T] {

  /** Name of the table. */
  def tableName: SqlIdentifier

  /** Alias this table to be used in joins */
  def alias(name: String): Alias[T] = Alias(name, this)
}

object SqlTabular {
  inline def derived[T <: Product: Mirror.ProductOf](using nm: NameMapping = NameMapping.Default): SqlTabular[T] =
    Macros.buildTabular[T]

  case class SimpleTabular[T](
      tableName: SqlIdentifier,
      columns: SqlIdentifiers,
      rowDecoder: ResultRowDecoder[T],
      parameterFiller: ParameterFiller[T]
  ) extends SqlTabular[T] {
    override def cardinality: Int = columns.size
  }
}

case class LazySqlTabular[T](tabular: SqlTabular[T])

object LazySqlTabular {
  inline given derived[T <: Product: Mirror.ProductOf](
      using nm: NameMapping = NameMapping.Default
  ): LazySqlTabular[T] = {
    LazySqlTabular(SqlTabular.derived)
  }
}
