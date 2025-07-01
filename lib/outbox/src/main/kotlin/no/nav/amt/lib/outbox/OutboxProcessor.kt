package no.nav.amt.lib.outbox

import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.utils.job.JobManager
import no.nav.amt.lib.utils.objectMapper
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Processes and publishes outbox records.
 * The processor is designed to be run as a background job.
 *
 * It will periodically fetch unprocessed records from the database and publish them to Kafka.
 * If an record fails to be published, it will be marked as failed and will be retried later.
 * To avoid publishing records out of order for the same key, the processor will not publish any new records for a key/topic
 * if a previous record for the same key/topic has failed.
 *
 * @param service The service for interacting with the outbox record repository.
 * @param jobManager The job manager for scheduling the background job.
 * @param producer The Kafka producer for publishing records.
 */
class OutboxProcessor(
    private val service: OutboxService,
    private val jobManager: JobManager,
    private val producer: Producer<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Starts a background job that periodically processes outbox records.
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
            processRecords()
        }
    }

    internal fun processRecords() {
        try {
            val unprocessedRecords = service.findUnprocessedRecords(100)
            if (unprocessedRecords.isEmpty()) {
                return
            }

            val failedKeys = mutableSetOf<KeyTopicPair>()

            unprocessedRecords.forEach { record ->
                try {
                    if (failedKeys.contains(KeyTopicPair(record.key, record.topic))) {
                        log.warn(
                            "Skipping processing of record ${record.id} for key ${record.key} " +
                                "and topic ${record.topic} due to previous failure.",
                        )
                        return@forEach
                    }
                    process(record)
                } catch (e: Exception) {
                    service.markAsFailed(record.id, e.message ?: e::class.java.name)
                    log.error("Failed to process outbox-record ${record.id}", e)
                    failedKeys.add(KeyTopicPair(record.key, record.topic))
                }
            }
        } catch (e: Exception) {
            log.error("Failed to process outbox records: ${e.message}", e)
        }
    }

    private fun process(record: OutboxRecord) {
        producer.produce(
            topic = record.topic,
            key = record.key,
            value = objectMapper.writeValueAsString(record.value),
        )
        service.markAsProcessed(record.id)
        log.info("Processed outbox-record ${record.id} for key ${record.key} on topic ${record.topic}")
    }

    data class KeyTopicPair(
        val key: String,
        val topic: String,
    )
}
