package usql.dao

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
}
