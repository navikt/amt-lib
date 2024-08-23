package no.nav.amt.lib.utils.database

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database {
    private lateinit var dataSource: DataSource

    fun init(config: DatabaseConfig) {
        dataSource = HikariDataSource().apply {
            if (config.jdbcURL.isNotEmpty()) {
                jdbcUrl = config.jdbcURL
            } else {
                dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
                addDataSourceProperty("serverName", config.dbHost)
                addDataSourceProperty("portNumber", config.dbPort)
                addDataSourceProperty("databaseName", config.dbDatabase)
                addDataSourceProperty("user", config.dbUsername)
                addDataSourceProperty("password", config.dbPassword)
            }
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

        runMigration()
    }

    fun <A> query(block: (Session) -> A): A = using(sessionOf(dataSource)) { session ->
        block(session)
    }

    fun close() {
        (dataSource as HikariDataSource).close()
    }

    private fun runMigration(initSql: String? = null): Int = Flyway
        .configure()
        .connectRetries(5)
        .dataSource(dataSource)
        .initSql(initSql)
        .validateMigrationNaming(true)
        .load()
        .migrate()
        .migrations
        .size
}
