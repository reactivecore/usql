package usql.profiles

import usql.DataType

import java.sql.{JDBCType, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant
import java.util
import scala.language.implicitConversions
import scala.reflect.ClassTag

trait BasicProfile {
  implicit val intType: DataType[Int] = DataType.simple(JDBCType.INTEGER, _.getInt(_), _.setInt(_, _))

  implicit val longType: DataType[Long] = DataType.simple(JDBCType.BIGINT, _.getLong(_), _.setLong(_, _))

  implicit val shortType: DataType[Short] = DataType.simple(JDBCType.SMALLINT, _.getShort(_), _.setShort(_, _))

  implicit val byteType: DataType[Byte] = DataType.simple(JDBCType.TINYINT, _.getByte(_), _.setByte(_, _))

  implicit val booleanType: DataType[Boolean] = DataType.simple(JDBCType.BOOLEAN, _.getBoolean(_), _.setBoolean(_, _))

  implicit val floatType: DataType[Float] = DataType.simple(JDBCType.FLOAT, _.getFloat(_), _.setFloat(_, _))

  implicit val doubleType: DataType[Double] = DataType.simple(JDBCType.DOUBLE, _.getDouble(_), _.setDouble(_, _))

  implicit val bigDecimalType: DataType[BigDecimal] =
    DataType.simple(JDBCType.DECIMAL, _.getBigDecimal(_), (ps, idx, v) => ps.setBigDecimal(idx, v.underlying()))

  implicit val stringType: DataType[String] = DataType.simple(JDBCType.VARCHAR, _.getString(_), _.setString(_, _))

  implicit val timestampType: DataType[Timestamp] =
    DataType.simple(JDBCType.TIMESTAMP, _.getTimestamp(_), _.setTimestamp(_, _))

  implicit val instantType: DataType[Instant] = timestampType.adapt[Instant](_.toInstant, Timestamp.from)

  implicit def optionType[T](using dt: DataType[T]): DataType[Option[T]] = new DataType[Option[T]] {
    override def extractBySqlIdx(cIdx: Int, rs: ResultSet): Option[T] = {
      dt.extractOptionalBySqlIdx(cIdx, rs)
    }

    override def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: Option[T]): Unit = {
      value match {
        case None    => ps.setNull(pIdx, jdbcType.getVendorTypeNumber)
        case Some(v) => dt.fillBySqlIdx(pIdx, ps, v)
      }
    }

    override def jdbcType: JDBCType = dt.jdbcType
  }

  implicit val arrayType: DataType[java.sql.Array] = new DataType[java.sql.Array] {
    override def jdbcType: JDBCType = JDBCType.ARRAY

    override def extractBySqlIdx(cIdx: Int, rs: ResultSet): java.sql.Array = {
      rs.getArray(cIdx)
    }

    override def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: java.sql.Array): Unit = {
      ps.setArray(pIdx, value)
    }
  }

  implicit val stringArray: DataType[Seq[String]] = arrayType.adaptWithPs(
    _.getArray.asInstanceOf[Array[String]].toSeq,
    (v, ps) => {
      val array = ps.getConnection.createArrayOf(JDBCType.VARCHAR.toString, v.toArray)
      array
    }
  )

  implicit val stringList: DataType[List[String]] = stringArray.adapt(_.toList, identity)

  implicit val intArray: DataType[Seq[Int]] = arrayType.adaptWithPs(
    _.getArray.asInstanceOf[Array[Int]].toSeq,
    (v, ps) => {
      val array = ps.getConnection.createArrayOf(JDBCType.INTEGER.toString, v.toArray)
      array
    }
  )
}

object BasicProfile extends BasicProfile
