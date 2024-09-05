package usql.dao

import usql.{SqlIdentifier, SqlIdentifiers}
import usql.profiles.BasicProfile.*
import usql.util.TestBaseWithH2
import scala.language.implicitConversions

class KeyedCrudBaseTest extends TestBaseWithH2 {
  override protected def baseSql: String =
    """
      |CREATE TABLE "user" (
      |  id INT PRIMARY KEY,
      |  name TEXT,
      |  age INT
      |)
      |""".stripMargin

  case class User(
      id: Int,
      name: Option[String],
      age: Option[Int]
  ) derives SqlTabular

  object UserCrd extends KeyedCrudBase[Int, User] {
    override val keyColumn: SqlIdentifier = "id"

    override lazy val tabular: SqlTabular[User] = summon

    override def keyOf(value: User): Int = value.id
  }

  val sample1 = User(1, Some("Alice"), Some(42))
  val sample2 = User(2, Some("Bob"), None)
  val sample3 = User(3, None, None)

  it should "properly escape" in {
    UserCrd.tabular.tableName shouldBe SqlIdentifier.fromString("user")
    UserCrd.tabular.columns shouldBe SqlIdentifiers.fromStrings(
      "id",
      "name",
      "age"
    )
  }

  it should "provide basic crd features" in {
    UserCrd.countAll() shouldBe 0
    UserCrd.insert(sample1)
    UserCrd.insert(sample2)
    UserCrd.countAll() shouldBe 2
    UserCrd.findAll() should contain theSameElementsAs Seq(sample1, sample2)
    UserCrd.findByKey(1) shouldBe Some(sample1)
    UserCrd.findByKey(0) shouldBe empty
    UserCrd.deleteByKey(0) shouldBe 0
    UserCrd.deleteByKey(1) shouldBe 1
    UserCrd.findAll() should contain theSameElementsAs Seq(sample2)
    UserCrd.deleteAll() shouldBe 1
    UserCrd.countAll() shouldBe 0
    UserCrd.findAll() shouldBe empty
  }

  it should "provide updates" in {
    UserCrd.insert(Seq(sample1, sample2))
    val sample2x = sample2.copy(
      age = Some(100)
    )
    UserCrd.update(sample3) shouldBe 0 // was not existant
    UserCrd.update(sample2x) shouldBe 1
    UserCrd.findAll() should contain theSameElementsAs Seq(
      sample1,
      sample2x
    )

    val sample1x = sample1.copy(name = None)
    UserCrd.update(sample1x) shouldBe 1

    UserCrd.findAll() should contain theSameElementsAs Seq(
      sample1x,
      sample2x
    )
  }
}
