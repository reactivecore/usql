package usql.dao

import usql.{SqlIdentifier, SqlIdentifiers, SqlRawPart}

/** Experimental helper for building aliases used in Join Statements */
case class Alias[T](
    aliasName: String,
    tabular: SqlTabular[T]
) {

  /** Alias one identifier */
  def apply(c: SqlIdentifier): SqlRawPart = {
    SqlRawPart(this.aliasName + "." + c.serialize)
  }

  /** Refers to all aliased columns */
  def columns: SqlRawPart = {
    SqlRawPart(
      tabular.columns.identifiers
        .map { i =>
          apply(i).s
        }
        .mkString(",")
    )
  }

  def table: SqlRawPart = {
    SqlRawPart(tabular.tableName.serialize + " " + aliasName)
  }
}
