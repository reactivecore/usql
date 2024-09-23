package usql

import SqlInterpolationParameter.{Empty, SqlParameter}
import usql.profiles.BasicProfile.*
import usql.util.TestBase

class SqlInterpolationTest extends TestBase {
  it should "work" in {
    val baz        = 123
    val buz        = "Hello"
    sql"foo ${baz} bar" shouldBe Sql(
      Seq(("foo ", SqlParameter(baz)), (" bar", SqlInterpolationParameter.Empty))
    )
    val withParams = sql"foo ${baz} bar ${buz}"
    withParams.sql shouldBe "foo ? bar ?"

    withParams shouldBe Sql(
      Seq(("foo ", SqlParameter(baz)), (" bar ", SqlParameter("Hello")))
    )
    sql"${baz}" shouldBe Sql(
      Seq(
        ("", SqlParameter(baz))
      )
    )

    val identifier = SqlIdentifier.fromString("table1")
    val withSingle = sql"select * from ${identifier}"
    withSingle shouldBe Sql(
      Seq("select * from " -> SqlInterpolationParameter.IdentifierParameter(identifier))
    )
    withSingle.sql shouldBe "select * from table1"

    val identifiers     = SqlIdentifiers(
      Seq(
        SqlIdentifier.fromString("a"),
        SqlIdentifier.fromString("b")
      )
    )
    val withIdentifiers = sql"select ${identifiers} from ${identifier} where id = ${2}"
    withIdentifiers shouldBe Sql(
      Seq(
        "select "      -> SqlInterpolationParameter.IdentifiersParameter(identifiers),
        " from "       -> SqlInterpolationParameter.IdentifierParameter(identifier),
        " where id = " -> SqlInterpolationParameter.SqlParameter(2)
      )
    )
    withIdentifiers.sql shouldBe "select a,b from table1 where id = ?"
  }

  it should "allow stripMargin" in {
    sql"""
         |Hello ${1}
         |World ${2}
         |""".stripMargin shouldBe Sql(
      Seq(
        "\nHello " -> SqlParameter(1),
        "\nWorld " -> SqlParameter(2),
        "\n"       -> Empty
      )
    )
  }

  it should "allow concatenation" in {
    sql"HELLO ${1}" + sql"WORLD ${2}" shouldBe Sql(
      Seq(
        "HELLO " -> SqlParameter(1),
        "WORLD " -> SqlParameter(2)
      )
    )
  }

  it should "allow embedded frags" in {
    val inner    = sql"SELECT * FROM foo"
    val combined = sql"${inner} WHERE id = ${1} AND bar = ${2}"
    combined shouldBe Sql(
      Seq(
        "SELECT * FROM foo" -> Empty,
        " WHERE id = "      -> SqlParameter(1),
        " AND bar = "       -> SqlParameter(2)
      )
    )
  }

  it should "also work in another case" in {
    val inner    = sql"C = ${2}"
    val foo      = sql"HELLO a = ${1} AND"
    val combined = (sql"HELLO a = ${1} AND ${inner}")
    combined shouldBe Sql(
      Seq(
        "HELLO a = " -> SqlParameter(1),
        " AND "      -> Empty,
        "C = "       -> SqlParameter(2)
      )
    )
    combined.sql shouldBe "HELLO a = ? AND C = ?"
  }
}
