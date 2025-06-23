package no.nav.amt.lib.outbox

import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.utils.job.JobManager
import no.nav.amt.lib.utils.objectMapper
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Processes and publishes outbox events.
 * The processor is designed to be run as a background job.
 *
 * It will periodically fetch unprocessed events from the database and publish them to Kafka.
 * If an event fails to be published, it will be marked as failed and will be retried later.
 * To avoid publishing events out of order for the same key, the processor will not publish any new events for a key/topic
 * if a previous event for the same key/topic has failed.
 *
 * @param service The service for interacting with the outbox event repository.
 * @param jobManager The job manager for scheduling the background job.
 * @param producer The Kafka producer for publishing events.
 */
class OutboxProcessor(
    private val service: OutboxService,
    private val jobManager: JobManager,
    private val producer: Producer<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Starts a background job that periodically processes outbox events.
     *
     * @param initialDelay The initial delay before the job starts.
     * @param period The period between each job execution.
     */
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
