package link_shortener

import org.flywaydb.core.Flyway

import javax.sql.DataSource

object DatabaseMigrations:
  def run(dataSource: DataSource): Unit =
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate()
