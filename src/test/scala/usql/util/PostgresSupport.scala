package usql.util

import org.scalatest.{BeforeAndAfterEach, Suite}

import java.sql.{Connection, DriverManager}
import java.util.Properties
import scala.util.{Random, Using}

/** Testbase which provides an empty postgres database per Test. */
trait PostgresSupport extends TestDatabaseSupport with BeforeAndAfterEach {
  self: Suite =>

  private val postgresHost: String             = sys.env.getOrElse("POSTGRES_HOST", "localhost")
  private val postgresUser: String             = sys.env.getOrElse("POSTGRES_USER", "postgres")
  private val postgresPassword: Option[String] = sys.env.get("POSTGRES_PASSWORD")

  private var _dbName: Option[String] = None

  protected def dbName: String = {
    _dbName.getOrElse {
      throw new IllegalStateException(s"DB name not initialized?!")
    }
  }

  override protected def beforeEach(): Unit = {
    _dbName = Some(s"unittest_${Math.abs(Random.nextLong())}")
    withRootConnection { con =>
      con.prepareStatement(s"CREATE DATABASE ${dbName}").execute()
    }
    super.beforeEach()
  }

  override def makeJdbcUrlAndProperties(): (String, Properties) = {
    (s"jdbc:postgresql://${postgresHost}/${dbName}", makeProperties())
  }

  private def makeProperties(): Properties = {
    val props = new Properties()
    props.setProperty("user", postgresUser)
    postgresPassword.foreach { pwd =>
      props.setProperty("password", pwd)
    }
    props
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    withRootConnection { con =>
      con.prepareStatement(s"DROP DATABASE ${dbName}").execute()
    }
  }

  private def withRootConnection[T](fn: Connection => T): T = {
    val props = makeProperties()
    Using.resource(DriverManager.getConnection(s"jdbc:postgresql://${postgresHost}/postgres", props)) { con =>
      fn(con)
    }
  }
}
