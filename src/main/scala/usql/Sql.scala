package usql

import SqlInterpolationParameter.{InnerSql, SqlParameter}

import java.sql.{Connection, PreparedStatement}
import scala.annotation.{tailrec, targetName}
import scala.language.implicitConversions
import scala.util.Using

extension (sc: StringContext) {
  def sql(parameters: SqlInterpolationParameter*): Sql = {
    Sql(fixParameters(parameters))
  }

  /** Bring parameters into a canonical format. */
  private def fixParameters(parameters: Seq[SqlInterpolationParameter]): Seq[(String, SqlInterpolationParameter)] = {
    @tailrec
    def fix(
        parts: List[String],
        params: List[SqlInterpolationParameter],
        builder: List[(String, SqlInterpolationParameter)]
    ): List[(String, SqlInterpolationParameter)] = {
      (parts, params) match {
        case (Nil, _)                                                                          =>
          // No more parts
          builder
        case (part :: restParts, Nil) if part.isEmpty                                          =>
          // Skip it, empty part and no parameter
          fix(restParts, Nil, builder)
        case (part :: restParts, Nil)                                                          =>
          // More Parts but no parameters
          fix(restParts, Nil, (part, SqlInterpolationParameter.Empty) :: builder)
        case (part :: restParts, (param: SqlParameter[?]) :: restParams) if part.endsWith("#") =>
          // Getting #${..} parameters to work
          val replacedPart = part.stripSuffix("#") + param.dataType.serialize(param.value)
          fix(restParts, restParams, (replacedPart, SqlInterpolationParameter.Empty) :: builder)
        case (part :: restParts, (param: InnerSql) :: restParams)                              =>
          // Innre Sql
          val combined = param.sql.parts.toList.reverse ++ builder
          val newParts = if (part.isEmpty) {
            combined
          } else {
            (part, SqlInterpolationParameter.Empty) :: combined
          }
          fix(restParts, restParams, newParts)
        case (part :: restParts, param :: restParams)                                          =>
          // Regular Case
          fix(restParts, restParams, (part, param) :: builder)
      }
    }
    fix(sc.parts.toList, parameters.toList, Nil).reverse
  }
}

/** SQL with already embedded parameters. */
case class Sql(parts: Seq[(String, SqlInterpolationParameter)]) extends SqlBase {
  def sql = parts.iterator.map { case (part, param) =>
    part + param.replacement
  }.mkString

  private def sqlParameters: Seq[SqlParameter[?]] = parts.collect { case (_, p: SqlParameter[?]) =>
    p
  }

  override def withPreparedStatement[T](f: PreparedStatement => T)(using cp: ConnectionProvider): T = {
    cp.withConnection {
      val c = summon[Connection]
      Using.resource(c.prepareStatement(sql)) { statement =>
        sqlParameters.zipWithIndex.foreach { case (param, idx) =>
          param.dataType.fillByZeroBasedIdx(idx, statement, param.value)
        }
        f(statement)
      }
    }
  }

  def stripMargin: Sql = {
    stripMargin('|')
  }

  def stripMargin(marginChar: Char): Sql = {
    Sql(
      parts.map { case (s, p) =>
        s.stripMargin(marginChar) -> p
      }
    )
  }

  @targetName("concat")
  inline def +(other: Sql): Sql = concat(other)

  def concat(other: Sql): Sql = {
    Sql(
      this.parts ++ other.parts
    )
  }
}
