package no.nav.amt.lib.testing

import kotliquery.queryOf
import no.nav.amt.lib.testing.utils.ContainerReuseConfig
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.database.DatabaseConfig
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

object SingletonPostgresContainer {
    private val log = LoggerFactory.getLogger(javaClass)

    private val postgresDockerImageName = getPostgresImage()

    private var postgresContainer: PostgreSQLContainer<Nothing>? = null

    private val reuseConfig = ContainerReuseConfig()

    fun start() {
        if (postgresContainer == null) {
            log.info("Starting new postgres database...")

            val container = createContainer()
            postgresContainer = container

            container.start()

            configureEnv(container)

            Database.init(DatabaseConfig())

            setupShutdownHook()
            log.info("Postgres setup finished")
        }
    }

    private fun configureEnv(container: PostgreSQLContainer<Nothing>) {
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

    private fun createContainer(): PostgreSQLContainer<Nothing> {
        val container = PostgreSQLContainer<Nothing>(
            DockerImageName.parse(postgresDockerImageName).asCompatibleSubstituteFor("postgres"),
        )
        container.addEnv("TZ", "Europe/Oslo")
        container.withReuse(reuseConfig.reuse)
        container.withLabel("reuse.UUID", reuseConfig.reuseLabel)
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
            ).filterNot { table -> table == "flyway_schema_history" }

        it.transaction { tx ->
            tables.forEach { table ->
                val sql = "truncate table $table cascade"
                log.info("Truncating table $table...")
                tx.run(queryOf(sql).asExecute)
            }
        }
    }

    private fun getPostgresImage(): String {
        val digest = when (System.getProperty("os.arch")) {
            "aarch64" -> "@sha256:58ddae4817fc2b7ed43ac43c91f3cf146290379b7b615210c33fa62a03645e70"
            else -> ""
        }
        return "postgres:14-alpine$digest"
    }
}
