package no.nav.amt.lib.outbox

import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime

/**
 * Represents an event that is stored in the outbox, waiting to be published.
 *
 * To store events create a postgres table with the following SQL:
 * ```sql
 * CREATE TABLE outbox_event (
 * id SERIAL PRIMARY KEY,
 * key VARCHAR(255) NOT NULL,
 * value JSONB NOT NULL,
 * value_type VARCHAR(255) NOT NULL,
 * topic VARCHAR(255) NOT NULL,
 * created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 * modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 * processed_at TIMESTAMPTZ,
 * status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
 * retry_count INT NOT NULL DEFAULT 0,
 * error_message TEXT
 * );
 *
 * CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
 * ```
 *
 * @property id The unique identifier of the event.
 * @property key The key of the event, used for partitioning in Kafka.
 * @property value The payload of the event, as a JSON object.
 * @property valueType The type of the payload, used for deserialization.
 * @property topic The Kafka topic to which the event will be published.
 * @property createdAt The timestamp when the event was created.
 * @property processedAt The timestamp when the event was successfully processed. Null if not yet processed.
 * @property status The current status of the event (e.g., PENDING, PROCESSED, FAILED).
 * @property retryCount The number of times the processing of this event has been attempted.
 * @property errorMessage An optional error message if the event processing failed.
 */
data class OutboxEvent(
    val id: OutboxEventId,
    val key: String,
    val value: JsonNode,
    val valueType: String,
    val topic: String,
    val createdAt: ZonedDateTime,
    val processedAt: ZonedDateTime? = null,
    val status: OutboxEventStatus,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
)

/**
 * A data class for creating a new outbox event.
 * It contains the essential information required to construct an [OutboxEvent].
 *
 * @property key The key of the event.
 * @property value The payload of the event.
 * @property valueType The type of the payload.
 * @property topic The target Kafka topic.
 */
internal data class NewOutboxEvent(
    val key: String,
    val value: JsonNode,
    val valueType: String,
    val topic: String,
)

@JvmInline
value class OutboxEventId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}

enum class OutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED,
}
