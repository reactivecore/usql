package usql

/**
 * An SQL Identifier (table or colum name
 * @param name
 *   raw name
 * @param quoted
 *   if true, the identifier will be quoted.
 */
@throws[IllegalArgumentException]("If name contains a \"")
case class SqlIdentifier(name: String, quoted: Boolean) {
  require(!name.contains("\""), "Identifiers may not contain \"")

  /** Serialize the identifier. */
  def serialize: String = {
    if (quoted) {
      "\"" + name + "\""
    } else {
      name
    }
  }

  /** Placeholder for select query */
  def placeholder: SqlRawPart = SqlRawPart("?")

  /** Named placeholder for update query */
  def namedPlaceholder: SqlRawPart = SqlRawPart(serialize + " = ?")

  override def toString: String = serialize
}

/** Multiple identifiers (e.g. column names) */
case class SqlIdentifiers(identifiers: Seq[SqlIdentifier]) {
  def serialize: String = {
    identifiers.iterator.map(_.serialize).mkString(",")
  }

  def size: Int = identifiers.size

  /** ?-Placeholders for each identifier */
  def placeholders: SqlRawPart = SqlRawPart(identifiers.map(_.placeholder.s).mkString(","))

  /** identifier = ?-Placeholders for Update statements */
  def namedPlaceholders: SqlRawPart = SqlRawPart(identifiers.map(_.namedPlaceholder.s).mkString(","))
}

object SqlIdentifier {
  given stringToIdentifier: Conversion[String, SqlIdentifier] with {
    override def apply(x: String): SqlIdentifier = fromString(x)
  }

  def fromString(s: String): SqlIdentifier = {
    if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
      SqlIdentifier(s.drop(1).dropRight(1), true)
    } else {
      if (SqlReservedWords.isReserved(s)) {
        SqlIdentifier(s, quoted = true)
      } else {
        SqlIdentifier(s, quoted = false)
      }
    }
  }
}

object SqlIdentifiers {
  def fromStrings(s: String*): SqlIdentifiers = {
    new SqlIdentifiers(s.map(SqlIdentifier.fromString))
  }
}
