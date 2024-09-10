package usql.dao

import usql.SqlIdentifier

import scala.annotation.StaticAnnotation

/** Annotation to override the default table name in [[SqlTabular]] */
case class TableName(name: String) extends StaticAnnotation

/** Annotation to override the default column name in [[SqlColumnar]] */
case class ColumnName(name: String) extends StaticAnnotation

/**
 * Controls the way nested column group names are generated.
 *
 * @param pattern
 *   the name pattern which will be applied. `%m` will be replaced by the member name, %c will be replaced by the child
 *   column name.
 */
case class ColumnGroup(pattern: String = "%m_%c") extends StaticAnnotation {

  /** Generates the required column name. */
  def columnName(memberName: String, childColumn: SqlIdentifier): SqlIdentifier = {
    val applied = pattern
      .replace("%m", memberName)
      .replace("%c", childColumn.name)
    SqlIdentifier(applied, childColumn.quoted)
  }
}
