package no.nav.amt.lib.utils.database

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object Database {
    internal class TxContext(
        val transactionalSession: TransactionalSession,
    ) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<TxContext>
    }

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

            minimumIdle = 1
            leakDetectionThreshold = 10_000
        }

        runMigration()
    }

    fun close() = (dataSource as HikariDataSource).close()

    /**
     * Kjør en suspenderende kodeblokk innenfor en database-transaksjon.
     *
     * Transaksjonen fullføres automatisk: hvis koden kaster en exception, rulles transaksjonen tilbake.
     * Alle kall til `Database.query {}` innenfor blokken kjøres i samme transaksjon.
     * Nøstede transaksjoner er ikke tillatt – kaller man `transaction` innenfor en aktiv transaksjon,
     * kastes en [IllegalStateException].
     *
     * Funksjonen er trygg å bruke i coroutines, og sikrer isolasjon av transaksjoner per coroutine.
     *
     * Merk at direkte kall til `session.transaction { ... }` på KotliQuery-session innenfor en aktiv
     * transaksjon kan føre til [org.postgresql.util.PSQLException], typisk med meldingen
     * "Cannot commit when autoCommit is enabled", fordi KotliQuery forventer at transaksjonen
     * håndteres gjennom `Database.transaction`.
     *
     * @param block Kode som skal kjøres i transaksjon
     * @return Resultatet fra blokken
     * @throws [IllegalStateException] hvis funksjonen kalles mens en annen transaksjon er aktiv
     * @throws [org.postgresql.util.PSQLException] hvis en utilsiktet prøver å committe direkte via session.transaction innenfor aktiv transaksjon
     */
    suspend fun <T> withTransaction(block: suspend () -> T): T {
        if (currentCoroutineContext()[TxContext] != null) error("Nested transactions are not supported")

        return sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                withContext(TxContext(tx)) {
                    block()
                }
            }
        }
    }

    /**
     * Kjør en spørring i databasen.
     *
     * Hvis en transaksjon er aktiv i gjeldende coroutine, brukes den eksisterende transaksjonen.
     * Hvis ikke, åpnes en ny session som kjøres på [Dispatchers.IO].
     *
     * Alle kall til `Database.query {}` innenfor samme transaksjon vil bruke samme session,
     * slik at transaksjonen isoleres per coroutine.
     *
     * Merk at koden i [block] må være rask og ikke blokkere unødvendig, siden sessionen
     * allerede er koblet til databasen.
     *
     * @param block Lambda som mottar en [Session] og utfører spørringen.
     *              Returner verdien du ønsker fra blokken.
     * @return Resultatet fra [block], som kan være hvilken som helst type [T].
     * @throws org.postgresql.util.PSQLException hvis [block] prøver å utføre operasjoner som ikke er kompatible med aktiv transaksjon
     */
    suspend fun <T> query(block: (Session) -> T): T {
        val transactionalSession = currentCoroutineContext()[TxContext]?.transactionalSession
        return withContext(Dispatchers.IO) {
            if (transactionalSession != null) {
                block(transactionalSession)
            } else {
                queryWithNewSession(block)
            }
        }
    }

    internal suspend fun currentTransactionalSession(): TransactionalSession = currentCoroutineContext()[TxContext]?.transactionalSession
        ?: error("No active transaction in context. Use Database.transaction { }")

    private fun <T> queryWithNewSession(block: (Session) -> T): T = using(sessionOf(dataSource)) { session -> block(session) }

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
