package usql
import scala.language.implicitConversions

/** Parameters available in sql""-Interpolation. */
sealed trait SqlInterpolationParameter {
  /// Replacement, e.g. '?'
  def replacement: String
}

object SqlInterpolationParameter {

  /** A Parameter which will be filled using '?' and parameter filler */
  class SqlParameter[T](val value: T, val dataType: DataType[T]) extends SqlInterpolationParameter {
    override def equals(obj: Any): Boolean = {
      obj match {
        case s: SqlParameter[_] if value == s.value && dataType == s.dataType => true
        case _                                                                => false
      }
    }

    override def hashCode(): Int = {
      value.hashCode()
    }

    override def replacement: String = "?"

    override def toString: String = {
      s"SqlParameter(${value} of type ${dataType.jdbcType.getName})"
    }
  }

  object SqlParameter {
    def apply[T](value: T)(using dataType: DataType[T]): SqlParameter[T] = new SqlParameter(value, dataType)
  }

  /** A single identifier. */
  case class IdentifierParameter(i: SqlIdentifier) extends SqlInterpolationParameter {
    override def replacement: String = i.serialize
  }

  /** Multiple identifiers. */
  case class IdentifiersParameter(i: SqlIdentifiers) extends SqlInterpolationParameter {
    override def replacement: String = i.serialize
  }

  /** Some unchecked raw block. */
  case class RawBlockParameter(s: String) extends SqlInterpolationParameter {
    override def replacement: String = s
  }

  case class InnerSql(sql: Sql) extends SqlInterpolationParameter {
    // Not used
    override def replacement: String = sql.sql
  }

  /** Empty leaf, so that we have exactly as much interpolation parameters as string parts. */
  object Empty extends SqlInterpolationParameter {
    override def replacement: String = ""
  }

  implicit def toSqlParameter[T](value: T)(using dataType: DataType[T]): SqlParameter[T] = {
    new SqlParameter(value, dataType)
  }

  implicit def toIdentifierParameter(i: SqlIdentifier): IdentifierParameter    = IdentifierParameter(i)
  implicit def toIdentifiersParameter(i: SqlIdentifiers): IdentifiersParameter = IdentifiersParameter(i)
  implicit def rawBlockParameter(rawPart: SqlRawPart): RawBlockParameter       = RawBlockParameter(rawPart.s)
  implicit def innerSql(sql: Sql): InnerSql                                    = InnerSql(sql)
}

/** Something which can be added to sql""-interpolation without further checking. */
case class SqlRawPart(s: String) {
  override def toString: String = s
}
