package no.nav.amt.lib.testing

import kotliquery.queryOf
import no.nav.amt.lib.testing.utils.ContainerReuseConfig
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.database.DatabaseConfig
import org.slf4j.LoggerFactory
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.postgresql.PostgreSQLContainer.POSTGRESQL_PORT
import org.testcontainers.utility.DockerImageName

@Deprecated("Use TestPostgresContainer instead")
object SingletonPostgres16Container {
    init {
        SingletonPostgresContainer.startWithImage("postgres:16-alpine")
    }
}

@Deprecated("Use TestPostgresContainer instead")
object SingletonPostgresContainer {
    private val log = LoggerFactory.getLogger(javaClass)

    private var postgresContainer: PostgreSQLContainer? = null

    private val reuseConfig = ContainerReuseConfig()

    fun start() {
        start("postgres:17-alpine")
    }

    internal fun startWithImage(version: String) {
        start(version)
    }

    private fun start(image: String) {
        if (postgresContainer == null) {
            log.info("Starting new postgres database...")

            val container = createContainer(image)
            postgresContainer = container

            container.start()

            configureEnv(container)

            Database.init(DatabaseConfig())

            setupShutdownHook()
            log.info("Postgres setup finished")
        }
    }

    private fun configureEnv(container: PostgreSQLContainer) {
        System.setProperty(DatabaseConfig.DB_HOST_KEY, container.host)
        System.setProperty(DatabaseConfig.DB_PORT_KEY, container.getMappedPort(POSTGRESQL_PORT).toString())
        System.setProperty(DatabaseConfig.DB_DATABASE_KEY, container.databaseName)
        System.setProperty(DatabaseConfig.DB_PASSWORD_KEY, container.password)
        System.setProperty(DatabaseConfig.DB_USERNAME_KEY, container.username)

        val jdbcURL = "jdbc:postgresql://${container.host}:${container.getMappedPort(POSTGRESQL_PORT)}" +
            "/${container.databaseName}" +
            "?password=${container.password}" +
            "&user=${container.username}"
        System.setProperty(DatabaseConfig.JDBC_URL_KEY, jdbcURL)
    }

    private fun createContainer(image: String): PostgreSQLContainer {
        val container = PostgreSQLContainer(
            DockerImageName.parse(image).asCompatibleSubstituteFor("postgres"),
        )
        container.addEnv("TZ", "Europe/Oslo")
        container.withReuse(reuseConfig.reuse)
        container.withLabel("reuse.UUID", reuseConfig.reuseLabel)
        container.withCommand("postgres", "-c", "wal_level=logical")
        return container.waitingFor(HostPortWaitStrategy())
    }

    private fun setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Shutting down postgres database...")
                if (reuseConfig.reuse) {
                    cleanup()
                } else {
                    postgresContainer?.stop()
                }
                Database.close()
            },
        )
    }

    fun cleanup() = Database.query {
        val tables = it
            .run(
                queryOf("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
                    .map { it.string("table_name") }
                    .asList,
            )

        it.transaction { tx ->
            tables.forEach { table ->
                log.info("Dropping table $table...")
                tx.run(queryOf("drop table $table cascade").asExecute)
            }
        }
    }
}
