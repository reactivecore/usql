package usql.dao

import usql.SqlIdentifier

/** Maps Column / Table names. */
trait NameMapping {

  /** Converts a column name to SQL identifiers */
  def columnToSql(name: String): SqlIdentifier

  /** Converts a case class (full qualified) name to SQL. */
  def caseClassToTableName(name: String): SqlIdentifier
}

object NameMapping {

  /** Simple Snake Case Conversion with checking against escaping. */
  object Default extends NameMapping {

    override def columnToSql(name: String): SqlIdentifier = SqlIdentifier.fromString(snakeCase(name))

    override def caseClassToTableName(name: String): SqlIdentifier = {
      SqlIdentifier.fromString(snakeCase(getSimpleClassName(name)))
    }
  }

  /** Returns the simple class name from full qualified name. */
  def getSimpleClassName(s: String): String = {
    s.lastIndexOf('.') match {
      case -1 => s
      case n  => s.drop(n + 1)
    }
  }

  /** Converts a string to snake case. */
  def snakeCase(s: String): String = {
    val builder     = StringBuilder()
    var lastIsUpper = false
    var first       = true
    s.foreach { c =>
      if (c.isUpper && !lastIsUpper && !first) {
        builder += '_'
      }
      builder += c.toLower
      lastIsUpper = c.isUpper
      first = false
    }
    builder.result()
  }
}
