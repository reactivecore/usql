package usql.dao

import usql.*
import usql.util.TestBaseWithH2
import scala.language.implicitConversions

class SimpleJoinTest extends TestBaseWithH2 {

  override protected def baseSql: String =
    """
      |CREATE TABLE person (
      |  id INT PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  level_id INT
      |);
      |
      |CREATE TABLE level(
      |  id INT PRIMARY KEY,
      |  level_name TEXT
      |);
      |""".stripMargin

  case class Person(
      id: Int,
      name: String,
      levelId: Option[Int] = None
  ) derives SqlTabular

  object Person extends KeyedCrudBase[Int, Person] {
    override val keyColumn: SqlIdentifier = "id"

    override def keyOf(value: Person): Int = value.id

    override lazy val tabular: SqlTabular[Person] = summon
  }

  case class Level(
      id: Int,
      levelName: String
  ) derives SqlTabular

  object Level extends KeyedCrudBase[Int, Level] {
    override val keyColumn: SqlIdentifier = "id"

    override def keyOf(value: Level): Int = value.id

    override lazy val tabular: SqlTabular[Level] = summon
  }

  trait Env {
    val person1 = Person(1, "Alice")
    val person2 = Person(2, "Bob", Some(1))
    val person3 = Person(3, "Charly", Some(2))
    val person4 = Person(4, "Secret", Some(999))

    Person.insert(person1)
    Person.insert(person2)
    Person.insert(person3)
    Person.insert(person4)

    val level1 = Level(1, "Administrator")
    val level2 = Level(2, "Regular")
    val level3 = Level(3, "Nobody")

    Level.insert(level1)
    Level.insert(level2)
    Level.insert(level3)
  }

  val person = Alias("p", Person.tabular)
  val level  = Alias("l", Level.tabular)

  it should "do an easy inner join" in new Env {
    val joined =
      sql"""SELECT ${person.columns}, ${level.columns}
            FROM ${person.table} INNER JOIN ${level.table}
            WHERE p.level_id = l.id
            """.query.all[(Person, Level)]()

    joined should contain theSameElementsAs Seq(
      (person2, level1),
      (person3, level2)
    )
  }

  it should "do an easy left join" in new Env {
    val joined =
      sql"""SELECT ${person.columns}, ${level.columns}
                FROM ${person.table} LEFT JOIN ${level.table} ON p.level_id = l.id
                """.query.all[(Person, Option[Level])]()

    joined should contain theSameElementsAs Seq(
      (person1, None),
      (person2, Some(level1)),
      (person3, Some(level2)),
      (person4, None)
    )
  }
}
