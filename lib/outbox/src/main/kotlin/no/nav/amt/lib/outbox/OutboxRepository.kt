package no.nav.amt.lib.outbox

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import org.postgresql.util.PGobject

internal class OutboxRepository {
    private fun rowmapper(row: Row) = OutboxEvent(
        id = OutboxEventId(row.long("id")),
        key = row.string("key"),
        value = objectMapper.readTree(row.string("value")),
        valueType = row.string("value_type"),
        topic = row.string("topic"),
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
                key, 
                value, 
                value_type, 
                topic, 
                status, 
                retry_count
            )
            values (
                :key, 
                :value, 
                :value_type, 
                :topic, 
                :status, 
                :retry_count 
            )
            returning *
            """.trimIndent()

        val params = mapOf(
            "key" to event.key,
            "value" to toPGObject(event.value),
            "value_type" to event.valueType,
            "topic" to event.topic,
            "status" to OutboxEventStatus.PENDING.name,
            "retry_count" to 0,
        )

        it.single(queryOf(sql, params), this::rowmapper)
            ?: throw NoSuchElementException(
                "Failed to insert OutboxEvent for key: ${event.key}, " +
                    "valueType: ${event.valueType} and topic: ${event.topic}",
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
