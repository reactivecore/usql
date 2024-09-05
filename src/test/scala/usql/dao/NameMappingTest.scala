package usql.dao

import NameMapping.Default
import usql.SqlIdentifier
import usql.util.TestBase

class NameMappingTest extends TestBase {
  "Default" should "work" in {
    Default.caseClassToTableName("foo.bar.MySuperClass") shouldBe SqlIdentifier.fromString("my_super_class")
    Default.caseClassToTableName("foo.bar.User") shouldBe SqlIdentifier.fromString("user")
    Default.columnToSql("id") shouldBe SqlIdentifier.fromString("id")
    Default.columnToSql("myData") shouldBe SqlIdentifier.fromString("my_data")

  }

  "snakeCase" should "work" in {
    NameMapping.snakeCase("foo") shouldBe "foo"
    NameMapping.snakeCase("fooBar") shouldBe "foo_bar"
    NameMapping.snakeCase("XyzTCPStream") shouldBe "xyz_tcpstream"
    NameMapping.snakeCase("BOOM") shouldBe "boom"
  }
}
