package no.nav.amt.lib.outbox

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import org.postgresql.util.PGobject

internal class OutboxRepository {
    private fun rowmapper(row: Row) = OutboxEvent(
        id = OutboxEventId(row.long("id")),
        aggregateId = row.string("aggregate_id"),
        aggregateType = row.string("aggregate_type"),
        topic = row.string("topic"),
        payload = objectMapper.readTree(row.string("payload")),
        createdAt = row.zonedDateTime("created_at"),
        processedAt = row.zonedDateTimeOrNull("processed_at"),
        status = OutboxEventStatus.valueOf(row.string("status")),
        retryCount = row.int("retry_count"),
        errorMessage = row.stringOrNull("error_message"),
    )

    internal fun insertNewEvent(event: NewOutboxEvent) = Database.query {
        val sql =
            """
            insert into outbox_event (
                aggregate_id, 
                aggregate_type, 
                topic, 
                payload, 
                status, 
                retry_count
            )
            values (
                :aggregate_id, 
                :aggregate_type, 
                :topic, 
                :payload, 
                :status, 
                :retry_count 
            )
            returning *
            """.trimIndent()

        val params = mapOf(
            "aggregate_id" to event.aggregateId,
            "aggregate_type" to event.aggregateType,
            "topic" to event.topic,
            "payload" to toPGObject(event.payload),
            "status" to OutboxEventStatus.PENDING.name,
            "retry_count" to 0,
        )

        it.single(queryOf(sql, params), this::rowmapper)
            ?: throw NoSuchElementException(
                "Failed to insert OutboxEvent for aggregateId: ${event.aggregateId}, " +
                    "aggregateType: ${event.aggregateType} and topic: ${event.topic}",
            )
    }

    fun findUnprocessedEvents(limit: Int): List<OutboxEvent> = Database.query {
        val sql =
            """
            select * from outbox_event
            where status = '${OutboxEventStatus.PENDING.name}' or status = '${OutboxEventStatus.FAILED.name}'
            order by created_at asc
            limit :limit
            """.trimIndent()

        val params = mapOf("limit" to limit)

        it.list(queryOf(sql, params), this::rowmapper)
    }

    fun markAsProcessed(eventId: OutboxEventId) = Database.query {
        val sql =
            """
            update outbox_event
            set processed_at = current_timestamp, 
                status = '${OutboxEventStatus.PROCESSED.name}',
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to eventId.value,
        )

        it.update(queryOf(sql, params))
    }

    fun markAsProcessing(eventId: OutboxEventId) = Database.query {
        val sql =
            """
            update outbox_event
            set status = '${OutboxEventStatus.PROCESSING.name}', 
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to eventId.value,
        )

        it.update(queryOf(sql, params))
    }

    fun markAsFailed(eventId: OutboxEventId, errorMessage: String) = Database.query {
        val sql =
            """
            update outbox_event
            set status = '${OutboxEventStatus.FAILED.name}', 
                error_message = :error_message, 
                retry_count = retry_count + 1,
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to eventId.value,
            "error_message" to errorMessage,
        )

        it.update(queryOf(sql, params))
    }

    fun get(id: OutboxEventId): OutboxEvent? = Database.query {
        val sql = "select * from outbox_event where id = :id"
        val params = mapOf("id" to id.value)
        it.single(queryOf(sql, params), this::rowmapper)
    }
}

private fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
