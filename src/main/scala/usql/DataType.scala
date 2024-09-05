package usql

import java.sql.{Connection, JDBCType, PreparedStatement, ResultSet}

/** Type class describing a type to use. */
trait DataType[T] {

  /** Serialize a value (e.g. Debugging) */
  def serialize(value: T): String = value.toString

  /** The underlying jdbc type. */
  def jdbcType: JDBCType

  // Extractors from ResultSet

  def extractByZeroBasedIndex(idx: Int, rs: ResultSet): T = {
    extractBySqlIdx(idx + 1, rs)
  }

  def extractBySqlIdx(cIdx: Int, rs: ResultSet): T

  def extractOptionalBySqlIdx(cIdx: Int, rs: ResultSet): Option[T] = {
    val candidate = Option(extractBySqlIdx(cIdx, rs))
    if (rs.wasNull()) {
      None
    } else {
      candidate
    }
  }

  def extractByName(columnLabel: String, resultSet: ResultSet): T = {
    val sqlIdx = resultSet.findColumn(columnLabel)
    extractBySqlIdx(sqlIdx, resultSet)
  }

  // Fillers

  def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: T): Unit

  def fillByZeroBasedIdx(idx: Int, ps: PreparedStatement, value: T): Unit = {
    fillBySqlIdx(idx + 1, ps, value)
  }

  /** Adapt to another type. */
  def adapt[U](mapFn: T => U, contraMapFn: U => T): DataType[U] = {
    val me = this
    new DataType[U] {
      override def jdbcType: JDBCType = me.jdbcType

      override def extractBySqlIdx(cIdx: Int, rs: ResultSet): U = mapFn(me.extractBySqlIdx(cIdx, rs))

      override def extractOptionalBySqlIdx(cIdx: Int, rs: ResultSet): Option[U] = {
        me.extractOptionalBySqlIdx(cIdx, rs).map(mapFn)
      }

      override def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: U): Unit = {
        me.fillBySqlIdx(pIdx, ps, contraMapFn(value))
      }
    }
  }

  /** Adapt to another type, also providing the prepared statement */
  def adaptWithPs[U](mapFn: T => U, contraMapFn: (U, PreparedStatement) => T): DataType[U] = {
    val me = this
    new DataType[U] {
      override def jdbcType: JDBCType = me.jdbcType

      override def extractBySqlIdx(cIdx: Int, rs: ResultSet): U = {
        mapFn(me.extractBySqlIdx(cIdx, rs))
      }

      override def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: U): Unit =
        me.fillBySqlIdx(pIdx, ps, contraMapFn(value, ps))
    }
  }
}

object DataType {
  def simple[T](
      jdbc: JDBCType,
      rsExtractor: (ResultSet, Int) => T,
      filler: (PreparedStatement, Int, T) => Unit
  ): DataType[T] = new DataType[T] {
    override def jdbcType: JDBCType = jdbc

    override def extractBySqlIdx(cIdx: Int, rs: ResultSet): T = rsExtractor(rs, cIdx)

    override def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: T): Unit = filler(ps, pIdx, value)
  }

  def get[T](using dt: DataType[T]): DataType[T] = dt
}
