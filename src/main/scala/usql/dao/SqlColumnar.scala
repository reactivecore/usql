package usql.dao

import usql.{DataType, ParameterFiller, ResultRowDecoder, SqlIdentifier, SqlIdentifiers}

import scala.deriving.Mirror

/** Encapsulates column data and codecs for a product type */
trait SqlColumnar[T] {

  /** The column names. */
  def columns: SqlIdentifiers

  /** Count of column names. */
  def cardinality: Int

  /** Decoder for a full row. */
  def rowDecoder: ResultRowDecoder[T]

  /** Filler for a full row. */
  def parameterFiller: ParameterFiller[T]
}

object SqlColumnar {

  /**
   * Derive an instance for a case class.
   *
   * Use [[ColumnName]] to control column names.
   *
   * @param nm
   *   name mapping strategy.
   */
  inline def derived[T <: Product: Mirror.ProductOf](using nm: NameMapping = NameMapping.Default): SqlColumnar[T] =
    Macros.buildColumnar[T]

  case class SimpleColumnar[T](
      columns: SqlIdentifiers,
      rowDecoder: ResultRowDecoder[T],
      parameterFiller: ParameterFiller[T]
  ) extends SqlColumnar[T] {
    override def cardinality: Int = columns.size
  }
}
