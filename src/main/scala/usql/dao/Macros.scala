package usql.dao

import usql.{DataType, ParameterFiller, ResultRowDecoder, SqlIdentifier, SqlIdentifiers}

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes, Type}

object Macros {
  inline def buildColumnar[T <: Product](
      using nm: NameMapping,
      mirror: Mirror.ProductOf[T]
  ): SqlColumnar.SimpleColumnar[T] = {
    val labels            = deriveLabels[T]
    val columnAnnotations = columnNameAnnotations[T]
    val columnNames       = SqlIdentifiers(labels.zip(columnAnnotations).map {
      case (_, Some(annotation)) => SqlIdentifier.fromString(annotation.name)
      case (label, _)            => nm.columnToSql(label)
    })

    val rowDecoder      = summonInline[ResultRowDecoder[mirror.MirroredElemTypes]].map(mirror.fromTuple)
    val parameterFiller =
      summonInline[ParameterFiller[mirror.MirroredElemTypes]].contraMap[T](x => Tuple.fromProductTyped(x)(using mirror))
    SqlColumnar.SimpleColumnar(
      columns = columnNames,
      rowDecoder = rowDecoder,
      parameterFiller = parameterFiller
    )
  }

  inline def buildTabular[T <: Product](using nm: NameMapping, mirror: Mirror.ProductOf[T]): SqlTabular[T] = {
    val columnar = buildColumnar[T]

    val tableName: SqlIdentifier = tableNameAnnotation[T]
      .map { tn =>
        SqlIdentifier.fromString(tn.name)
      }
      .getOrElse {
        nm.caseClassToTableName(typeName[T])
      }

    SqlTabular.SimpleTabular(
      tableName = tableName,
      columnar.columns,
      columnar.rowDecoder,
      columnar.parameterFiller
    )
  }

  inline def typeName[T]: String = {
    ${ typeNameImpl[T] }
  }

  def typeNameImpl[T](using types: Type[T], quotes: Quotes): Expr[String] = {
    Expr(Type.show[T])
  }

  inline def deriveLabels[T](using m: Mirror.Of[T]): List[String] = {
    // Also See https://stackoverflow.com/a/70416544/335385
    summonLabels[m.MirroredElemLabels]
  }

  inline def summonLabels[T <: Tuple]: List[String] = {
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ValueOf[t]].value.asInstanceOf[String] :: summonLabels[ts]
    }
  }

  /** Extract table name annotation for the type. */
  inline def tableNameAnnotation[T]: Option[TableName] = {
    ${ tableNameAnnotationImpl[T] }
  }

  def tableNameAnnotationImpl[T](using quotes: Quotes, t: Type[T]): Expr[Option[TableName]] = {
    import quotes.reflect.*
    val tree   = TypeRepr.of[T]
    val symbol = tree.typeSymbol
    symbol.annotations.collectFirst {
      case term if (term.tpe <:< TypeRepr.of[TableName]) =>
        term.asExprOf[TableName]
    } match {
      case None    => '{ None }
      case Some(e) => '{ Some(${ e }) }
    }
  }

  /** Extract column name annotations for each column. */
  inline def columnNameAnnotations[T]: List[Option[ColumnName]] = {
    ${ fieldAnnotationExtractor[ColumnName, T] }
  }

  def fieldAnnotationExtractor[A, T](using quotes: Quotes, t: Type[T], a: Type[A]): Expr[List[Option[A]]] = {
    import quotes.reflect.*
    val tree   = TypeRepr.of[T]
    val symbol = tree.typeSymbol

    // Note: symbol.caseFields.map(_.annotations) does not work, but using the primaryConstructor works
    // Also see https://august.nagro.us/read-annotations-from-macro.html

    Expr.ofList(
      symbol.primaryConstructor.paramSymss.flatten
        .map { sym =>
          sym.annotations.collectFirst {
            case term if (term.tpe <:< TypeRepr.of[A]) =>
              term.asExprOf[A]
          } match {
            case None    => '{ None }
            case Some(e) => '{ Some(${ e }) }
          }
        }
    )
  }
}
