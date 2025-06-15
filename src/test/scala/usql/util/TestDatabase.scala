package usql.util

import org.scalatest.BeforeAndAfterEach
import usql.ConnectionProvider

import java.sql.{Connection, DriverManager}
import java.util.Properties
import scala.util.{Random, Using}

trait TestDatabaseSupport {

  /** Generates JDBC Url and Properties to connect to the database. */
  def makeJdbcUrlAndProperties(): (String, Properties)
}

trait TestDatabase extends BeforeAndAfterEach with TestDatabaseSupport {
  self: TestBase =>

  protected def baseSql: String = ""

  private var _rootConnection: Option[Connection]             = None
  private var _urlAndProperties: Option[(String, Properties)] = None

  protected def jdbcUrl: String = _urlAndProperties.getOrElse {
    throw new IllegalStateException(s"No jdbc url")
  }._1

  protected def jdbcPropertes: Properties = _urlAndProperties.getOrElse {
    throw new IllegalStateException(s"No properties")
  }._2

  given cp: ConnectionProvider with {
    override def withConnection[T](f: Connection ?=> T): T = {
      Using.resource(DriverManager.getConnection(jdbcUrl, jdbcPropertes)) { c =>
        f(using c)
      }
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val (url, props) = makeJdbcUrlAndProperties()

    val connection = DriverManager.getConnection(url, props)
    _rootConnection = Some(connection)
    _urlAndProperties = Some((url, props))

    runBaseSql()
  }

  override protected def afterEach(): Unit = {
    _rootConnection.foreach(_.close())
    super.afterEach()
  }

  protected def runSql(sql: String): Unit = {
    cp.withConnection {
      val c = summon[Connection]
      c.prepareStatement(sql).execute()
    }
  }

  protected def runSqlMultiline(sql: String): Unit = {
    val splitted = splitSql(baseSql)
    splitted.foreach { line =>
      runSql(line)
    }
  }

  private def runBaseSql(): Unit = {
    runSqlMultiline(baseSql)
  }

  protected def splitSql(s: String): Seq[String] = {
    // Note: very rough
    s.split("(?<=;)\\s+").toSeq.map(_.trim.stripSuffix(";")).filter(_.nonEmpty)
  }

}
