package no.nav.amt.lib.outbox

import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.utils.job.JobManager
import no.nav.amt.lib.utils.objectMapper
import org.slf4j.LoggerFactory
import java.time.Duration

class OutboxProcessor(
    private val service: OutboxService,
    private val jobManager: JobManager,
    private val producer: Producer<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun start(initialDelay: Duration = Duration.ofMillis(1000), period: Duration = Duration.ofMillis(5000)) {
        jobManager.startJob(
            name = "outbox-processor",
            initialDelay = initialDelay,
            period = period,
        ) {
            processEvents()
        }
    }

    internal fun processEvents() {
        try {
            val unprocessedEvents = service.findUnprocessedEvents(100)
            if (unprocessedEvents.isEmpty()) {
                return
            }

            val failedKeys = mutableSetOf<KeyTopicPair>()

            unprocessedEvents.forEach { event ->
                try {
                    if (failedKeys.contains(KeyTopicPair(event.key, event.topic))) {
                        log.warn(
                            "Skipping processing of event ${event.id} for key ${event.key} " +
                                "and topic ${event.topic} due to previous failure.",
                        )
                        return@forEach
                    }
                    process(event)
                } catch (e: Exception) {
                    service.markAsFailed(event.id, e.message ?: e::class.java.name)
                    log.error("Failed to process outbox-event ${event.id}", e)
                    failedKeys.add(KeyTopicPair(event.key, event.topic))
                }
            }
        } catch (e: Exception) {
            log.error("Failed to process outbox events: ${e.message}", e)
        }
    }

    private fun process(event: OutboxEvent) {
        producer.produce(
            topic = event.topic,
            key = event.key,
            value = objectMapper.writeValueAsString(event.value),
        )
        service.markAsProcessed(event.id)
        log.info("Processed outbox-event ${event.id} for key ${event.key} on topic ${event.topic}")
    }

    data class KeyTopicPair(
        val key: String,
        val topic: String,
    )
}
