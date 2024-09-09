package usql.dao

import scala.annotation.StaticAnnotation

/** Annotation to override the default table name in [[SqlTabular]] */
case class TableName(name: String) extends StaticAnnotation

/** Annotation to override the default column name in [[SqlColumnar]] */
case class ColumnName(name: String) extends StaticAnnotation

/** Embedded column group */
case class ColumnGroup(prefix: String = "", suffix: String = "") extends StaticAnnotation
