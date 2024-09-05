package usql.dao

import usql.{ParameterFiller, ResultRowDecoder, SqlIdentifiers}

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes, Type}

object Macros {
  inline def buildColumnar[T <: Product](
      using nm: NameMapping,
      mirror: Mirror.ProductOf[T]
  ): SqlColumnar.SimpleColumnar[T] = {
    val labels          = deriveLabels[T]
    val columnNames     = usql.SqlIdentifiers(labels.map(nm.columnToSql).toVector)
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
    val name     = typeName[T]
    val columnar = buildColumnar[T]
    SqlTabular.SimpleTabular(
      tableName = nm.caseClassToTableName(name),
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
}
