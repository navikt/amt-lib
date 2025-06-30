package no.nav.amt.lib.outbox

import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

/**
 * Provides a high-level API for interacting with the outbox.
 * This service simplifies the creation and management of outbox events,
 * abstracting away the underlying repository details.
 */
class OutboxService {
    private val repository = OutboxRepository()

    /**
     * Creates a new outbox event and persists it to the database.
     *
     * @param T The type of the value being sent.
     * @param key The key of the event, typically a unique identifier for the entity.
     * @param value The value (payload) of the event.
     * @param topic The Kafka topic to which the event will be published.
     * @return The created [OutboxRecord].
     */
    fun <T : Any> insertRecord(
        key: UUID,
        value: T,
        topic: String,
    ): OutboxRecord {
        val event = NewOutboxRecord(
            key = key.toString(),
            valueType = value::class.java.simpleName,
            topic = topic,
            value = objectMapper.readTree(objectMapper.writeValueAsString(value)),
        )
        return repository.insertNewRecord(event)
    }

    /**
     * Finds unprocessed outbox events (with status PENDING or FAILED).
     *
     * @param limit The maximum number of events to return.
     * @return A list of unprocessed [OutboxRecord]s.
     */
    fun findUnprocessedRecords(limit: Int): List<OutboxRecord> = repository.findUnprocessedRecords(limit)

    /**
     * Marks an outbox event as processed.
     *
     * @param id The ID of the event to mark as processed.
     */
    fun markAsProcessed(id: OutboxRecordId) = repository.markAsProcessed(id)

    /**
     * Marks an outbox event as failed.
     *
     * @param id The ID of the event.
     * @param errorMessage A message describing the reason for the failure.
     */
    fun markAsFailed(id: OutboxRecordId, errorMessage: String) = repository.markAsFailed(id, errorMessage)
}
