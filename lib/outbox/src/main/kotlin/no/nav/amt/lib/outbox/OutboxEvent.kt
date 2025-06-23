package no.nav.amt.lib.outbox

import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime

/**
Create a postgres table with the following SQL:
```sql
CREATE TABLE outbox_event (
id SERIAL PRIMARY KEY,
key VARCHAR(255) NOT NULL,
value JSONB NOT NULL,
value_type VARCHAR(255) NOT NULL,
topic VARCHAR(255) NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
processed_at TIMESTAMPTZ,
status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
retry_count INT NOT NULL DEFAULT 0,
error_message TEXT
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
```
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
