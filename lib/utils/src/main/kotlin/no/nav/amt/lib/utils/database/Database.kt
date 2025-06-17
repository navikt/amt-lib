package no.nav.amt.lib.utils.database

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database {
    private lateinit var dataSource: DataSource
    private val transactionalSessionThreadLocal = ThreadLocal<TransactionalSession?>()
    internal val transactionalSession get() = transactionalSessionThreadLocal.get()

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
            maxLifetime = 1001
            leakDetectionThreshold = 2001
        }

        runMigration()
    }

    fun <A> query(block: (Session) -> A): A = if (transactionalSession != null) {
        block(transactionalSession!!)
    } else {
        queryWithNewSession(block)
    }

    /**
     * Kjør en suspenderende kodeblokk innenfor en database-transaksjon.
     *
     * Transaksjonen fullføres automatisk, og alle kall av `Database.query {}` innenfor blokken
     * kjøres i samme transaksjon. Det er ikke tillatt med nøstede transaksjoner.
     *
     * Funksjonen skal være trygg å bruke i coroutines,
     * og sørger for isolasjon av transaksjoner per coroutine.
     *
     * @param block Kode som skal kjøres i transaksjon
     * @return Resultatet fra blokken
     * @throws IllegalStateException hvis funksjonen kalles mens en annen transaksjon er aktiv
     */
    suspend fun <A> transaction(block: suspend () -> A): A {
        check(transactionalSession == null) { "Nested transactions are not supported" }

        return sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val txContext = transactionalSessionThreadLocal.asContextElement(tx)
                withContext(txContext) {
                    try {
                        block()
                    } finally {
                        transactionalSessionThreadLocal.remove()
                    }
                }
            }
        }
    }

    private fun <A> queryWithNewSession(block: (Session) -> A): A = using(sessionOf(dataSource)) { session ->
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
