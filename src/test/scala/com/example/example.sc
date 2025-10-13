import usql.*
import usql.dao.*
import usql.profiles.H2Profile.given

import java.sql.{Connection, DriverManager}
import scala.util.{Try, Using}

Class.forName("org.h2.Driver")

val jdbcUrl = "jdbc:h2:mem:hello;DB_CLOSE_DELAY=-1"
given cp: ConnectionProvider with {
  override def withConnection[T](f: Connection ?=> T): T = {
    Using.resource(DriverManager.getConnection(jdbcUrl)) { c =>
      f(using c)
    }
  }
}

// Simple Actions

sql"CREATE TABLE person (id INT PRIMARY KEY, name TEXT)"
  .execute()

sql"INSERT INTO person (id, name) VALUES (${1}, ${"Alice"})"
  .execute()

sql"INSERT INTO person (id, name) VALUES (${2}, ${"Bob"})"
  .execute()

// Simple Queries

val all: Vector[(Int, String)] = sql"SELECT id, name FROM person".queryAll[(Int, String)]()
println(s"All=${all}")

// Constant Parts of the query

val one: Option[(Int, String)] = sql"SELECT id, name FROM #${"person"} WHERE id = ${1}".queryOne[(Int, String)]()
println(s"One=${one}")

val ids   = Seq(1, 2, 3)
val names = sql"SELECT name FROM person WHERE id IN (${SqlParameters(ids)})".queryAll[String]()
println(s"Names=${names}")

// Inserts

sql"INSERT INTO person (id, name) VALUES(?, ?)".one((3, "Charly")).update.run()
sql"INSERT INTO person (id, name) VALUES(?, ?)"
  .batch(
    Seq(
      4 -> "Dave",
      5 -> "Emil"
    )
  )
  .run()

sql"SELECT COUNT(*) FROM person".queryOne[Int]().get

// Reusable Parts
val select      = sql"SELECT id, name FROM person"
val selectAlice = (select + sql" WHERE id = ${1}").queryOne[(Int, String)]()
println(s"Alice: ${selectAlice}")

// Transactions

Try {
  transaction {
    sql"INSERT INTO person(id, name) VALUES(${100}, ${"Duplicate"})".execute()
    sql"INSERT INTO person(id, name) VALUES(${100}, ${"Duplicate 2"})".execute()
  }
}

// Dao

case class Person(
    id: Int,
    name: String
) derives SqlTabular

object Person extends KeyedCrudBase[Int, Person] {
  override def key: KeyColumnPath = cols.id

  override def keyOf(value: Person): Int = value.id

  override lazy val tabular: SqlTabular[Person] = summon
}

println(s"All Persons: ${Person.findAll()}")

Person.insert(Person(6, "Fritz"))
Person.update(Person(6, "Franziska"))
println(Person.findByKey(6)) // Person(6, Franziska)

// Simple Queries (using Scala 3.7.0+ Named Tuples)

val allAgain: Vector[(Int, String)] =
  sql"SELECT ${Person.cols.id}, ${Person.cols.name} FROM ${Person}".queryAll[(Int, String)]()

println(s"allAgain=${allAgain}")

// Simple QueryBuilder

println(Person.query.filter(_.id === 6).map(_.name).one()) // Some("Franziska")
