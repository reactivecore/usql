package usql.profiles

import usql.DataType
import java.sql.{JDBCType, PreparedStatement, ResultSet}
import java.util.UUID

trait PostgresProfile extends BasicProfile {
  implicit val uuidType: DataType[UUID] = new DataType[UUID] {
    override def jdbcType: JDBCType = JDBCType.OTHER

    override def extractBySqlIdx(cIdx: Int, rs: ResultSet): UUID = rs.getObject(cIdx, classOf[UUID])

    override def fillBySqlIdx(pIdx: Int, ps: PreparedStatement, value: UUID): Unit = ps.setObject(pIdx, value)
  }
}

object PostgresProfile extends PostgresProfile
