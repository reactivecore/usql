# usql micro JDBC toolkit for Scala 3

usql is a small jdbc wrapper to automate recurring patterns and
to simplify writing SQL typical Actions in the age of direct style scala.

Note: this is Beta software. Only Postgres and H2 are supported yet (altough it's 
easy wo write more Profiles).

## Installation

Add to build.sbt

```scala
libraryDependencies += "net.reactivecore" %% "usql" % "CURRENT_VERSION"
```

Replace `CURRENT_VERSION` with current version (e.g. `0.2.0`)

## Features

- No dependencies
- Fast compile speed
- Functional API
- Extensible
- SQL Interpolation
- Simple CRUD (Create, Replace, Update, Modify) / DAO-Object generation for your case classes.

## Non-Features

- Not bound to effect system
- No ORM
- JDBC Only.
- No Connection-Management, but easy to connect to [HikariCP](https://github.com/brettwooldridge/HikariCP)
- No query validation (this should be done by testcases)
- No DDL Generation

## Supported Databases

- `BasicProfile` supports basic types for most JDBC-Compatible Databases
- `H2Profile` for H2
- `PostgresProfile` for Postgres

The profiles can be incomplete, but should be easy to extend for your needs.

## Prior Art

- A lot of ideas are from [Anorm](https://playframework.github.io/anorm/)
- [Magnum](https://github.com/AugustNagro/magnum), quite similar but more advanced.

# Examples

Also see the Example in [example.sc](src/test/scala/com/example/example.sc)

## Connecting to a Database

To use usql you need to provide a given `ConnectionProvider`, this can be as easy as:

```scala 3
import usql.*
import usql.profiles.H2Profile.*

val jdbcUrl = "<your-jdbc-connection-url>"
given cp: ConnectionProvider with {
  override def withConnection[T](f: Connection ?=> T): T = {
    Using.resource(DriverManager.getConnection(jdbcUrl)) { c =>
      f(using c)
    }
  }
}
```

## Simple Actions

```scala 3
sql"CREATE TABLE person (id INT PRIMARY KEY, name TEXT)"
  .execute()
```

Using Interpolation, which will be used as parameter for prepared statements

```scala 3
sql"INSERT INTO person (id, name) VALUES (${1}, ${"Alice"})"
  .execute()

sql"INSERT INTO person (id, name) VALUES (${2}, ${"Bob"})"
  .execute()
```

## Queries and Interpolation

Simple Queries:

```scala 3
val all: Vector[(Int, String)] = sql"SELECT id, name FROM person".query.all[(Int, String)]()
println(s"All=${all}")
```

```scala 3
val one: Option[(Int, String)] = sql"SELECT id, name FROM #${"person"} WHERE id = ${1}".query.one[(Int, String)]()
println(s"One=${one}")
```

## Inserts

```scala 3
// Single Insert
sql"INSERT INTO person (id, name) VALUES(?, ?)".one((3, "Charly")).update.run()

// Batch Insert
sql"INSERT INTO person (id, name) VALUES(?, ?)"
  .batch(
    Seq(
      4 -> "Dave",
      5 -> "Emil"
    )
  )
  .run()

sql"SELECT COUNT(*) FROM person".query.one[Int]().get
// is 5
```

## Reusable Parts

You can concatenate sql parts:

```scala 3
val select      = sql"SELECT id, name FROM person"
val selectAlice = (select + sql" WHERE id = ${1}").query.one[(Int, String)]()
println(s"Alice: ${selectAlice}")
```

## Transactions

This fails because of the duplicate entry with id `100`, but at the end both are not inside:
```scala 3
Try {
  transaction {
    sql"INSERT INTO person(id, name) VALUES(${100}, ${"Duplicate"})".execute()
    sql"INSERT INTO person(id, name) VALUES(${100}, ${"Duplicate 2"})".execute()
  }
}
```

## Automatic DAO Objects

DAO (Data Access Objects) can be created using the base classes `CrdBase` and `KeyedCrudBase`.

They are using a helper description object called `SqlColumnar` and `SqlTabular`.

```scala 3
import usql.dao.*

case class Person(
   id: Int,
   name: String
 ) derives SqlTabular

object Person extends KeyedCrudBase[Int, Person] {
  override val keyColumn: SqlIdentifier = "id"

  override def keyOf(value: Person): Int = value.id

  override lazy val tabular: SqlTabular[Person] = summon
}

println(s"All Persons: ${Person.findAll()}")

Person.insert(Person(6, "Fritz"))
Person.update(Person(6, "Franziska"))
println(Person.findByKey(6)) // Person(6, Franziska)
```

# Core Types

- `DataType` a type class which derives how to fetch a Type `T` from a `ResultSet` and how to store it in a `PreparedStatement`
- `ResultRowDecoder` type class for fetching tuples / values from `ResultSet`
- `ParameterFiller` type class for filling tuples / values into a `PreparedStatement`
- `SqlIdentifier` an SQL identifier, quoted if necessary.
- `RawSql` Raw SQL Queries
- `Sql` interpolated SQL Queries

## DAO Core Types

- `SqlColumnar` describes the columns and codec for a case class `T`, macro generated
- `SqlTabular` like `SqlColumnar`, but also contains a table name
- `Crd` basic Create-Read-Delete operations
- `KeyedCrud` Crd for single-keyed types
