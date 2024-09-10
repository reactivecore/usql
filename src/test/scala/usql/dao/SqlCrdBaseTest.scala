package usql.dao

import usql.SqlIdentifier
import usql.profiles.BasicProfile.*
import usql.util.TestBaseWithH2

class SqlCrdBaseTest extends TestBaseWithH2 {
  override protected def baseSql: String =
    """
      |CREATE TABLE coordinate(
      |  id INT PRIMARY KEY,
      |  x INT,
      |  y INT
      |);
      |
      |
      |CREATE TABLE subcoord (
      |  id INT PRIMARY KEY,
      |  from_x DOUBLE,
      |  from_y DOUBLE,
      |  x_to DOUBLE,
      |  y_to DOUBLE
      |);
      |""".stripMargin

  case class Coordinate(id: Int, x: Int, y: Int) derives SqlTabular

  object CoordinateCrd extends CrdBase[Coordinate] {
    override lazy val tabular: SqlTabular[Coordinate] = summon
  }

  val sample  = Coordinate(0, 5, 6)
  val samples = Seq(
    Coordinate(1, 10, 20),
    Coordinate(2, 20, 30)
  )

  it should "do the usual operations with one item" in {
    CoordinateCrd.countAll() shouldBe 0
    CoordinateCrd.findAll() shouldBe empty
    CoordinateCrd.insert(sample) shouldBe 1
    CoordinateCrd.countAll() shouldBe 1
    CoordinateCrd.findAll() shouldBe Seq(sample)
    CoordinateCrd.deleteAll() shouldBe 1
    CoordinateCrd.findAll() shouldBe empty
  }

  it should "do the usual operations with many items" in {
    CoordinateCrd.insert(samples) shouldBe samples.size
    CoordinateCrd.countAll() shouldBe 2
    CoordinateCrd.findAll() should contain theSameElementsAs samples
    CoordinateCrd.deleteAll() shouldBe 2
    CoordinateCrd.countAll() shouldBe 0
    CoordinateCrd.findAll() shouldBe empty
  }

  case class SubCoord(x: Double, y: Double) derives SqlColumnar

  @TableName("subcoord")
  case class WithSubCoords(
      id: Int,
      from: SubCoord,
      @ColumnGroup("%c_to")
      to: SubCoord
  ) derives SqlTabular

  object WithSubCoords extends KeyedCrudBase[Int, WithSubCoords] {
    override val keyColumn: SqlIdentifier = "id"

    override def keyOf(value: WithSubCoords): Int = value.id

    override lazy val tabular: SqlTabular[WithSubCoords] = summon
  }

  it should "work for nested columns" in {
    val examples = Seq(
      WithSubCoords(3, SubCoord(3.4, 5.6), SubCoord(7.8, 9.7)),
      WithSubCoords(4, SubCoord(2.4, 1.6), SubCoord(2.8, 1.7))
    )
    WithSubCoords.insert(examples)
    WithSubCoords.findAll() should contain theSameElementsAs examples
    WithSubCoords.deleteByKey(1)
    WithSubCoords.findAll() should contain theSameElementsAs examples
    WithSubCoords.deleteByKey(3)
    WithSubCoords.findAll() should contain theSameElementsAs Seq(examples(1))
    WithSubCoords.findByKey(3) shouldBe None
    WithSubCoords.findByKey(4) shouldBe Some(examples(1))
  }
}
