package usql.profiles

import usql.util.{TestBase, TestBaseWithDatabase, TestDatabase}
import usql.*
import BasicProfile.*

import java.time.Instant

abstract class ProfileTestBase extends TestBaseWithDatabase {
  case class Example[T](
      dataType: DataType[T],
      optionalDataType: DataType[Option[T]],
      dbType: String,
      value: T
  )

  object Example {
    def make[T](dbType: String, value: T)(using dt: DataType[T], optionalDt: DataType[Option[T]]): Example[T] =
      Example(dt, optionalDt, dbType, value)
  }

  def hasTinyInt: Boolean = true

  val examples = Seq(
    Example.make("TEXT", "Hallo"),
    Example.make("INT", 123),
    Example.make("BIGINT", 12345678901234567L),
    Example.make("BOOLEAN", true),
    Example.make("FLOAT", 3.14f),
    Example.make("DOUBLE PRECISION", Math.PI),
    Example.make("SMALLINT", 124.shortValue),
    Example.make("TIMESTAMP", Instant.parse("2025-06-11T20:48:13Z")),
    Example.make("TEXT ARRAY", Seq("Alice", "Bob")),
    Example.make("INT ARRAY", Seq(1, 2, 3)),
    Example.make("BIGINT ARRAY", Seq(-1, 1, 2, 3, 3464368453864568L)),
    // Note: NaN values are not checked, because they do not compare to be equal
    Example.make[Seq[Float]]("FLOAT ARRAY", Seq(3.14f, 5.3f, -14f)),
    Example.make[Seq[Double]]("DOUBLE PRECISION ARRAY", Seq(3.14, 5.3, -14)),
    Example.make[Seq[Short]]("SMALLINT ARRAY", Seq(0.toShort, 100.toShort)),
    Example.make[Seq[Boolean]]("BOOLEAN ARRAY", Seq(false, true))
  ) ++ {
    if hasTinyInt then {
      Seq(
        Example.make("TINYINT", 124.byteValue),
        Example.make[Seq[Byte]]("TINYINT ARRAY", Seq(0.toByte, 100.toByte))
      )
    } else Nil
  }

  private def testExample[T](example: Example[T]): Unit = {
    it should s"work for ${example.dbType}" in {
      runSql(s"""
                |CREATE TABLE foo (id INT PRIMARY KEY, x ${example.dbType} NOT NULL);
                |""".stripMargin)

      given dt: DataType[T] = example.dataType

      sql"INSERT INTO foo (id, x) VALUES (1, ${example.value})".execute()

      val all = sql"SELECT id, x FROM foo".query.all[(Int, T)]()
      all shouldBe Seq((1, example.value))
    }

    it should s"work for optional ${example.dbType}" in {
      runSql(s"""
                |CREATE TABLE foo (id INT PRIMARY KEY, x ${example.dbType});
                |""".stripMargin)

      given dt: DataType[Option[T]] = example.optionalDataType

      sql"INSERT INTO foo (id, x) VALUES (1, ${Some(example.value): Option[T]})".execute()
      sql"INSERT INTO foo (id, x) VALUES (2, ${None: Option[T]})".execute()

      val all = sql"SELECT id, x FROM foo".query.all[(Int, Option[T])]()
      all should contain theSameElementsAs Seq(
        (1, Some(example.value)),
        (2, None)
      )
    }
  }

  examples.foreach { example =>
    testExample(example)
  }
}
