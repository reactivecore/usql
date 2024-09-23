package usql.dao

import usql.{SqlIdentifier, SqlIdentifiers, dao}
import usql.profiles.BasicProfile.given
import usql.util.TestBase

class SqlColumnarTest extends TestBase {
  case class Sample(
      name: String,
      age: Int
  )

  "Columnar" should "be derivable" in {
    val columnar = SqlColumnar.derived[Sample]
    columnar.columns shouldBe SqlIdentifiers.fromStrings("name", "age")
  }

  "Tabular" should "be derivable" in {
    val tabular = SqlTabular.derived[Sample]
    tabular.columns shouldBe SqlIdentifiers.fromStrings("name", "age")
    tabular.tableName shouldBe SqlIdentifier.fromString("sample")
  }

  @TableName("samplename")
  case class SampleWithAnnotations(
      @ColumnName("my_name") name: String,
      age: Int
  )

  it should "work with annotations" in {
    val tabular = SqlTabular.derived[SampleWithAnnotations]
    tabular.tableName shouldBe SqlIdentifier.fromString("samplename")
    tabular.columns shouldBe SqlIdentifiers.fromStrings("my_name", "age")
  }

  case class Nested(
      x: Double,
      y: Double
  ) derives SqlColumnar

  case class WithNested(
      @ColumnGroup(pattern = "a_%c")
      a: Nested,
      @ColumnGroup(pattern = "%c_s")
      b: Nested,
      c: Nested
  ) derives dao.SqlTabular

  it should "work for nested" in {
    val tabular = SqlTabular.derived[WithNested]
    tabular.parameterFiller.cardinality shouldBe 6
    tabular.rowDecoder.cardinality shouldBe 6
    tabular.columns shouldBe SqlIdentifiers.fromStrings("a_x", "a_y", "x_s", "y_s", "c_x", "c_y")
  }
}
