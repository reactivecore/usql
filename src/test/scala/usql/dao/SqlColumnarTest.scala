package usql.dao

import usql.{SqlIdentifier, SqlIdentifiers}
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
}
