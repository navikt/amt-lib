package no.nav.amt.lib

import com.fasterxml.jackson.databind.JsonNode
import java.time.ZonedDateTime

/**
Create a postgres table with the following SQL:
```sql
CREATE TABLE outbox_event (
id SERIAL PRIMARY KEY,
aggregate_id VARCHAR(255) NOT NULL,
aggregate_type VARCHAR(255) NOT NULL,
topic VARCHAR(255) NOT NULL,
payload JSONB NOT NULL,
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
    val id: Long? = null,
    val aggregateId: String,
    val aggregateType: String,
    val topic: String,
    val payload: JsonNode,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val processedAt: ZonedDateTime? = null,
    val status: OutboxEventStatus = OutboxEventStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
)

enum class OutboxEventStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
}
