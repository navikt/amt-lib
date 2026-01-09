package no.nav.amt.lib.outbox

import no.nav.amt.lib.outbox.metrics.OutboxMeter
import no.nav.amt.lib.outbox.metrics.PrometheusOutboxMeter
import no.nav.amt.lib.utils.objectMapper

/**
 * Provides a high-level API for interacting with the outbox.
 * This service simplifies the creation and management of outbox events,
 * abstracting away the underlying repository details.
 */
class OutboxService(
    private val meter: OutboxMeter = PrometheusOutboxMeter(),
) {
    private val repository = OutboxRepository()

    /**
     * Creates a new outbox event and persists it to the database.
     *
     * @param K The type of the key being sent.
     * @param V The type of the value being sent.
     * @param key The key of the event, typically a unique identifier for the entity.
     * @param value The value (payload) of the event.
     * @param topic The Kafka topic to which the event will be published.
     * @return The created [OutboxRecord].
     */
    suspend fun <K : Any, V : Any> insertRecord(
        key: K,
        value: V,
        topic: String,
    ): OutboxRecord {
        val event = NewOutboxRecord(
            key = key.toString(),
            valueType = value::class.java.simpleName,
            topic = topic,
            value = objectMapper.readTree(objectMapper.writeValueAsString(value)),
        )
        return repository.insertNewRecord(event).also { meter.incrementNewRecords(topic) }
    }

    /**
     * Finds unprocessed outbox events (with status PENDING or FAILED).
     *
     * @param limit The maximum number of events to return.
     * @return A list of unprocessed [OutboxRecord]s.
     */
    suspend fun findUnprocessedRecords(limit: Int): List<OutboxRecord> = repository.findUnprocessedRecords(limit)

    /**
     * Marks an outbox record as processed.
     *
     * @param record The record to mark as processed.
     */
    suspend fun markAsProcessed(record: OutboxRecord) {
        repository.markAsProcessed(record.id)
        meter.incrementProcessedRecords(record.topic, OutboxRecordStatus.PROCESSED)
    }

    /**
     * Marks an outbox record as failed.
     *
     * @param record The failed record.
     * @param errorMessage A message describing the reason for the failure.
     */
    suspend fun markAsFailed(record: OutboxRecord, errorMessage: String) {
        repository.markAsFailed(record.id, errorMessage)
        meter.incrementProcessedRecords(record.topic, OutboxRecordStatus.FAILED)
    }

    suspend fun getRecordsByTopicAndKey(topic: String, key: String): List<OutboxRecord> = repository.getRecordsByTopicAndKey(topic, key)
}
