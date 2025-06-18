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

    fun start() {
        jobManager.startJob(
            name = "outbox-processor",
            initialDelay = Duration.ofMillis(1000),
            period = Duration.ofMillis(5000),
        ) {
            process()
        }
    }

    private fun process() {
        try {
            val unprocessedEvents = service.findUnprocessedEvents(100)
            if (unprocessedEvents.isEmpty()) {
                return
            }

            val failedKeys = mutableSetOf<String>()

            unprocessedEvents.forEach { event ->
                try {
                    if (failedKeys.contains(event.aggregateId)) {
                        log.warn("Skipping processing of event ${event.id} for aggregate ${event.aggregateId} due to previous failure.")
                        return@forEach
                    }
                    produce(event)
                } catch (e: Exception) {
                    val msg = "Failed to process outbox-event ${event.id}: ${e.message}"
                    service.markAsFailed(event.id, msg)
                    log.error(msg, e)
                    failedKeys.add(event.aggregateId)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to process outbox events: ${e.message}", e)
        }
    }

    private fun produce(event: OutboxEvent) {
        service.markAsProcessing(event.id)
        producer.produce(
            topic = event.topic,
            key = event.aggregateId,
            value = objectMapper.writeValueAsString(event.payload),
        )
        service.markAsProcessed(event.id)
        log.info("Processed outbox-event ${event.id} for aggregate ${event.aggregateId} on topic ${event.topic}")
    }
}
