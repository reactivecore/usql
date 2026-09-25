# usql micro JDBC toolkit for Scala 3

usql is a small jdbc wrapper to automate recurring patterns and
to simplify writing SQL typical Actions in the age of direct style scala.

Note: this is Beta software. Only Postgres and H2 are supported yet (although it's 
easy to write more Profiles).

**[Scalar 2026 Presentation: Named Tuples in Action](https://github.com/nob13/scalar_named_tuple)**

## Installation

Version Matrix

| Version | JVM Version | Scala Version |
|---------|-------------|---------------|
| 0.6.x   | 21+         | 3.9.x+        |
| 0.5.x   | 21+         | 3.8.x+        |
| 0.4.x   | 21+         | 3.7.x+        |
| 0.3.x   | 21+         | 3.7.x+        |
| 0.2.x   | 17+         | 3.3.x+        |           

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
- Simple Query Builder

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

Encoding multiple Parameters (e.g. SQL-In-Operator):

```scala 3
val ids = Seq(1,2,3)
val names = sql"SELECT name FROM person WHERE id IN (${SqlParameters(ids)})".query.all[String]()
println(s"Names=${names}")
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

They are using a helper description object called `Structure` and `SqlTabular`.

```scala 3
import usql.dao.*

case class Person(
   id: Int,
   name: String
 ) derives SqlTabular

object Person extends KeyedCrudBase[Int, Person] {
  override def key: KeyColumnPath = cols.id

  override def keyOf(value: Person): Int = value.id
}

println(s"All Persons: ${Person.findAll()}")

Person.insert(Person(6, "Fritz"))
Person.update(Person(6, "Franziska"))
println(Person.findByKey(6)) // Person(6, Franziska)
```

### Remarks

You can not use a directly derived `SqlTabular` in a nested class. The Scala Compiler
may crash with

```
assertion failed: missing outer accessor in [Outer class Name]
```

You can work around, by defering the initialization of SqlTabular.

Instead of 

```scala
class Outer {
  case class Foo(
      // Fields
  ) derives SqlTabular
  
  object Foo extends KeyedCrudBase[Int, Foo]
}
```

write

```scala
class Outer {
  case class Foo(
      // Fields
  )
  
  given fooTabular: SqlTabular[Foo] = SqlTabular.derived
  
  object Foo extends KeyedCrudBase[Int, Foo]
}

Scala bug is https://github.com/scala/scala3/issues/22704

```
 

## Named Tuples

```scala 3
// Person.col.id will be automatically checked.
val allAgain: Vector[(Int, String)] =
  sql"SELECT ${Person.cols.id}, ${Person.cols.name} FROM ${Person}".query.all[(Int, String)]()

println(s"allAgain=${allAgain}")
```

## Simple Query Builder

```scala 3
println(Person.query.filter(_.id === 6).map(_.name).one()) // Some("Franziska")
```

The query builder also supports simple inner and left joins. They still need more testing though.

# Core Types

- `DataType` a type class which derives how to fetch a Type `T` from a `ResultSet` and how to store it in a `PreparedStatement`
- `RowDecoder` type class for fetching tuples / values from `ResultSet`
- `RowEncoder` type class for filling tuples / values into a `PreparedStatement`
- `SqlColumnId` an SQL Column incluing Alias
- `TableId` an Table identifier
- `RawSql` Raw SQL Queries
- `Sql` interpolated SQL Queries

## DAO Core Types

- `Structure` something which has columns, either SqlFielded or SqlColumn
- `SqlFielded` a field structure for a case class, is a `Structure[T]` 
- `SqlColumn` a single column
- `SqlTabular` like `SqlFielded`, but also contains a table name
- `Crd` basic Create-Read-Delete operations
- `KeyedCrud` Crd for single-keyed types
- `QueryBuilder` simple query builder
