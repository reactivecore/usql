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

  private def makeSeqType[T: ClassTag](jdbcType: JDBCType): DataType[Seq[T]] = arrayType.adaptWithPs[Seq[T]](
    { array =>
      val casted = array.getArray.asInstanceOf[Array[AnyRef]]
      casted.map(_.asInstanceOf[T]).toSeq
    },
    (v, ps) => {
      val javaArray = v.view.map(_.asInstanceOf[AnyRef]).toArray[AnyRef]
      val array     = ps.getConnection.createArrayOf(jdbcType.toString, javaArray)
      array
    }
  )

  private def makeNumberSeq[T: ClassTag](jdbcType: JDBCType, extractor: Number => T): DataType[Seq[T]] =
    arrayType.adaptWithPs[Seq[T]](
      { array =>
        val casted = array.getArray.asInstanceOf[Array[AnyRef]]
        casted.map(x => extractor(x.asInstanceOf[Number])).toSeq
      },
      (v, ps) => {
        val javaArray = v.asInstanceOf[Seq[AnyRef]].toArray[AnyRef]
        val array     = ps.getConnection.createArrayOf(jdbcType.toString, javaArray)
        array
      }
    )

  implicit val stringSeq: DataType[Seq[String]] = makeSeqType[String](JDBCType.VARCHAR)

  implicit val stringList: DataType[List[String]] = stringSeq.adapt(_.toList, identity)

  implicit val intSeq: DataType[Seq[Int]] = makeSeqType[Int](JDBCType.INTEGER)

  implicit val longSeq: DataType[Seq[Long]] = makeSeqType[Long](JDBCType.BIGINT)

  implicit val floatSeq: DataType[Seq[Float]] = makeNumberSeq[Float](JDBCType.FLOAT, _.floatValue())

  implicit val doubleSeq: DataType[Seq[Double]] = makeSeqType[Double](JDBCType.DOUBLE)

  implicit val booleanSeq: DataType[Seq[Boolean]] = makeSeqType[Boolean](JDBCType.BOOLEAN)

  implicit val shortSeq: DataType[Seq[Short]] = makeNumberSeq[Short](JDBCType.SMALLINT, _.shortValue())

  implicit val byteSeq: DataType[Seq[Byte]] = makeNumberSeq[Byte](JDBCType.TINYINT, _.byteValue())
}

object BasicProfile extends BasicProfile
