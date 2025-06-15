package usql.profiles

import usql.util.PostgresSupport

class PostgresProfileTest extends ProfileTestBase with PostgresSupport {
  override def hasTinyInt: Boolean = false
}
