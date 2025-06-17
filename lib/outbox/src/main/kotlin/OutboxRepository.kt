package no.nav.amt.lib

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import org.postgresql.util.PGobject

class OutboxRepository {
    private fun rowmapper(row: Row) = OutboxEvent(
        id = row.long("id"),
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

    fun insert(event: OutboxEvent) = Database.query {
        val sql =
            """
            insert into outbox_event (
                aggregate_id, 
                aggregate_type, 
                topic, 
                payload, 
                created_at, 
                processed_at, 
                status, 
                retry_count, 
                error_message
            )
            values (
                :aggregate_id, 
                :aggregate_type, 
                :topic, 
                :payload, 
                :created_at, 
                :processed_at, 
                :status, 
                :retry_count, 
                :error_message
            )
            returning *
            """.trimIndent()

        val params = mapOf(
            "aggregate_id" to event.aggregateId,
            "aggregate_type" to event.aggregateType,
            "topic" to event.topic,
            "payload" to toPGObject(event.payload),
            "created_at" to event.createdAt,
            "processed_at" to event.processedAt,
            "status" to event.status.name,
            "retry_count" to event.retryCount,
            "error_message" to event.errorMessage,
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

    fun markAsProcessed(eventId: Long) = Database.query {
        val sql =
            """
            update outbox_event
            set processed_at = current_timestamp, 
                status = '${OutboxEventStatus.PROCESSED.name}',
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to eventId,
        )

        it.update(queryOf(sql, params))
    }

    fun markAsProcessing(eventId: Long) = Database.query {
        val sql =
            """
            update outbox_event
            set status = '${OutboxEventStatus.PROCESSING.name}', 
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to eventId,
        )

        it.update(queryOf(sql, params))
    }

    fun markAsFailed(eventId: Long, errorMessage: String) = Database.query {
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
            "id" to eventId,
            "error_message" to errorMessage,
        )

        it.update(queryOf(sql, params))
    }

    fun get(id: Long): OutboxEvent? = Database.query {
        val sql = "select * from outbox_event where id = :id"
        val params = mapOf("id" to id)
        it.single(queryOf(sql, params), this::rowmapper)
    }
}

private fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
