package no.nav.amt.lib.testing

import no.nav.amt.lib.testing.utils.ContainerReuseConfig
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.slf4j.LoggerFactory
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

object SingletonKafkaProvider {
    private val log = LoggerFactory.getLogger(javaClass)
    private var kafkaContainer: KafkaContainer? = null

    private val reuseConfig = ContainerReuseConfig()

    val adminClient: AdminClient by lazy {
        AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to getHost()))
    }

    fun start() {
        if (kafkaContainer != null) return

        log.info("Starting new Kafka Instance...")

        kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka"))
        kafkaContainer!!.withReuse(reuseConfig.reuse)
        kafkaContainer!!.withLabel("reuse.UUID", reuseConfig.reuseLabel)
        kafkaContainer!!.start()

        setupShutdownHook()
        log.info("Kafka setup finished listening on ${kafkaContainer!!.bootstrapServers}.")
    }

    fun getHost(): String {
        if (kafkaContainer == null) {
            start()
        }
        return kafkaContainer!!.bootstrapServers
    }

    private fun setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Shutting down Kafka server...")
                if (reuseConfig.reuse) {
                    cleanup()
                } else {
                    kafkaContainer?.stop()
                }
            },
        )
    }

    fun cleanup() {
        val topics = adminClient.listTopics().names().get()

        topics.forEach {
            try {
                adminClient.deleteTopics(listOf(it))
                log.info("Deleted topic $it")
            } catch (e: Exception) {
                log.warn("Could not delete topic $it", e)
            }
        }
    }
}
