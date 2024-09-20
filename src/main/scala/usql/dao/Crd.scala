package usql.dao

import usql.*

/** Simple create/read/delete interface */
trait Crd[T] {

  /** Insert into database. */
  def insert(value: T)(using ConnectionProvider): Int = insert(Seq(value))

  /** Insert many elements */
  def insert(value1: T, value2: T, values: T*)(using ConnectionProvider): Int = insert(value1 +: value2 +: values)

  /** Insert many elements. */
  def insert(values: Seq[T])(using ConnectionProvider): Int

  /** Find all instances */
  def findAll()(using ConnectionProvider): Seq[T]

  /** Count all instances. */
  def countAll()(using ConnectionProvider): Int

  /** Delete all instances. */
  def deleteAll()(using ConnectionProvider): Int

}

/** CRUD (Create, Retrieve, Update, Delete) for keyed data. */
trait KeyedCrud[T] extends Crd[T] {

  /** Type of the key */
  type Key

  /** Returns the key of a value. */
  def keyOf(value: T): Key

  /** Update some value. */
  def update(value: T)(using ConnectionProvider): Int

  /** Find one by key. */
  def findByKey(key: Key)(using ConnectionProvider): Option[T]

  /** Load some value again based upon key. */
  def findAgain(value: T)(using ConnectionProvider): Option[T] = findByKey(keyOf(value))

  /** Delete by key. */
  def deleteByKey(key: Key)(using ConnectionProvider): Int
}

/** Implementation of Crd for Tabular data. */
abstract class CrdBase[T] extends Crd[T] {

  protected given pf: ParameterFiller[T] = tabular.parameterFiller

  protected given rd: ResultRowDecoder[T] = tabular.rowDecoder

  lazy val tabular: SqlTabular[T]

  private lazy val insertStatement =
    sql"INSERT INTO ${tabular.tableName} (${tabular.columns}) VALUES (${tabular.columns.placeholders})"

  override def insert(value: T)(using ConnectionProvider): Int = {
    insertStatement.one(value).update.run()
  }

  override def insert(values: Seq[T])(using ConnectionProvider): Int = {
    insertStatement.batch(values).run().sum
  }

  /** Select All Statement, may be reused. */
  protected lazy val selectAll = sql"SELECT ${tabular.columns} FROM ${tabular.tableName}"

  override def findAll()(using ConnectionProvider): Seq[T] = {
    selectAll.query.all()
  }

  private lazy val countAllStatement = sql"SELECT COUNT(*) FROM ${tabular.tableName}"

  override def countAll()(using ConnectionProvider): Int = {
    import usql.profiles.BasicProfile.intType
    countAllStatement.query.one[Int]().getOrElse(0)
  }

  private lazy val deleteAllStatement = sql"DELETE FROM ${tabular.tableName}"

  override def deleteAll()(using ConnectionProvider): Int = {
    deleteAllStatement.update.run()
  }
}

/** Implementation of KeyedCrd for KeyedTabular data. */
abstract class KeyedCrudBase[K, T](using keyDataType: DataType[K]) extends CrdBase[T] with KeyedCrud[T] {

  override type Key = K

  /** The column of the key */
  val keyColumn: SqlIdentifier

  override def keyOf(value: T): K

  private lazy val updateStatement =
    sql"UPDATE ${tabular.tableName} SET ${tabular.columns.namedPlaceholders} WHERE ${keyColumn} = ?"

  override def update(value: T)(using ConnectionProvider): Int = {
    val key = keyOf(value)
    updateStatement.one((value, key)).update.run()
  }

  private lazy val findByKeyQuery =
    sql"${selectAll} WHERE ${keyColumn} = ?"

  override def findByKey(key: K)(using ConnectionProvider): Option[T] = {
    findByKeyQuery.one(key).query.one()
  }

  private lazy val deleteByKeyStatement =
    sql"DELETE FROM ${tabular.tableName} WHERE ${keyColumn} = ?"

  override def deleteByKey(key: K)(using ConnectionProvider): Int = {
    deleteByKeyStatement.one(key).update.run()
  }
}
