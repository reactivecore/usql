package usql

import usql.util.TestBase

class SqlIdentifierTest extends TestBase {
  "fromString" should "automatically quote" in {
    SqlIdentifier.fromString("foo") shouldBe SqlIdentifier("foo", false)
    SqlIdentifier.fromString("id") shouldBe SqlIdentifier("id", false)
    SqlIdentifier.fromString("user") shouldBe SqlIdentifier("user", true)
    SqlIdentifier.fromString("\"foo\"") shouldBe SqlIdentifier("foo", true)
    intercept[IllegalArgumentException] {
      SqlIdentifier.fromString("\"id\"\"")
    }
  }
}
