package usql

import usql.profiles.BasicProfile.*
import usql.util.TestBaseWithH2

import java.sql.{ResultSet, SQLException}

class HelloDbTest extends TestBaseWithH2 {

  override protected def baseSql: String =
    """
      |CREATE TABLE "user" (id INT PRIMARY KEY, name VARCHAR);
      |""".stripMargin

  val tableName = SqlIdentifier.fromString("user")

  it should "work" in {
    sql"""INSERT INTO "user" (id, name) VALUES (${1}, ${"Hello World"})""".update.run()
    sql"""INSERT INTO "user" (id, name) VALUES (${3}, ${"How are you?"})""".update.run()

    // implicitly[ResultRowParser[EmptyTuple]]
    withClue("it should be possible to build various result row parsers") {
      summon[ResultRowDecoder[EmptyTuple]]
      summon[ResultRowDecoder[Int *: EmptyTuple]]
      summon[ResultRowDecoder[Int]]
      summon[ResultRowDecoder[(Int, String)]]
    }

    sql"""SELECT id, name FROM "user" WHERE id=${1}""".query.one[(Int, String)]() shouldBe Some(1 -> "Hello World")

    sql"""SELECT id, name FROM "user" WHERE id=${2}""".query.one[(Int, String)]() shouldBe None

    sql"""SELECT id, name FROM "user" ORDER BY id""".query.all[(Int, String)]() shouldBe Seq(
      1 -> "Hello World",
      3 -> "How are you?"
    )

    withClue("It should allow inferenced return types") {
      val result: Seq[(Int, String)] = sql"""SELECT id, name FROM "user" ORDER BY id""".query.all()
      result shouldBe Seq(
        1 -> "Hello World",
        3 -> "How are you?"
      )
    }
  }

  it should "allow hash replacements" in {
    sql"""SELECT id, name FROM #${"\"user\""} WHERE id=${1}""".query.one[(Int, String)]() shouldBe empty
  }

  it should "allow identifiers" in {
    val userTable = SqlIdentifier.fromString("user")
    sql"""SELECT id, name FROM ${userTable}""".query.one[(Int, String)]() shouldBe empty
  }

  it should "allow batch inserts" in {
    val batchInsert = sql"""INSERT INTO "user" (id, name) VALUES(?,?)""".batch(
      Seq(
        1 -> "Hello",
        2 -> "World"
      )
    )
    val response    = batchInsert.run()
    response shouldBe Seq(1, 1)

    val got = sql"""SELECT id, name FROM "user" ORDER BY ID""".query.all[(Int, String)]()
    got shouldBe Seq(
      1 -> "Hello",
      2 -> "World"
    )
  }

  it should "allow transactions" in {
    val insertCall = sql"INSERT INTO ${tableName} (id, name) VALUES(${1}, ${"Alice"})"
    intercept[SQLException] {
      transaction {
        insertCall.execute()
        insertCall.execute()
      }
    }
    sql"SELECT COUNT(*) FROM ${tableName}".query.one[Int]() shouldBe Some(0)

    transaction {
      insertCall.execute()
    }

    sql"SELECT COUNT(*) FROM ${tableName}".query.one[Int]() shouldBe Some(1)
  }
}
