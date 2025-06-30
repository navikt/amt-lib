package no.nav.amt.lib.outbox

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.lib.utils.database.Database
import no.nav.amt.lib.utils.objectMapper
import org.postgresql.util.PGobject

internal class OutboxRepository {
    private fun rowmapper(row: Row) = OutboxRecord(
        id = OutboxRecordId(row.long("id")),
        key = row.string("key"),
        value = objectMapper.readTree(row.string("value")),
        valueType = row.string("value_type"),
        topic = row.string("topic"),
        createdAt = row.zonedDateTime("created_at"),
        processedAt = row.zonedDateTimeOrNull("processed_at"),
        status = OutboxRecordStatus.valueOf(row.string("status")),
        retryCount = row.int("retry_count"),
        errorMessage = row.stringOrNull("error_message"),
    )

    internal fun insertNewRecord(record: NewOutboxRecord) = Database.query {
        val sql =
            """
            insert into outbox_record (
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
            "key" to record.key,
            "value" to toPGObject(record.value),
            "value_type" to record.valueType,
            "topic" to record.topic,
            "status" to OutboxRecordStatus.PENDING.name,
            "retry_count" to 0,
        )

        it.single(queryOf(sql, params), this::rowmapper)
            ?: throw NoSuchElementException(
                "Failed to insert OutboxRecord for key: ${record.key}, " +
                    "valueType: ${record.valueType} and topic: ${record.topic}",
            )
    }

    fun findUnprocessedRecords(limit: Int): List<OutboxRecord> = Database.query {
        val sql =
            """
            select * from outbox_record
            where status = '${OutboxRecordStatus.PENDING.name}' or status = '${OutboxRecordStatus.FAILED.name}'
            order by created_at asc
            limit :limit
            """.trimIndent()

        val params = mapOf("limit" to limit)

        it.list(queryOf(sql, params), this::rowmapper)
    }

    fun markAsProcessed(recordId: OutboxRecordId) = Database.query {
        val sql =
            """
            update outbox_record
            set processed_at = current_timestamp, 
                status = '${OutboxRecordStatus.PROCESSED.name}',
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to recordId.value,
        )

        it.update(queryOf(sql, params))
    }

    fun markAsFailed(recordId: OutboxRecordId, errorMessage: String) = Database.query {
        val sql =
            """
            update outbox_record
            set status = '${OutboxRecordStatus.FAILED.name}', 
                error_message = :error_message, 
                retry_count = retry_count + 1,
                modified_at = current_timestamp
            where id = :id
            """.trimIndent()

        val params = mapOf(
            "id" to recordId.value,
            "error_message" to errorMessage,
        )

        it.update(queryOf(sql, params))
    }

    fun get(id: OutboxRecordId): OutboxRecord? = Database.query {
        val sql = "select * from outbox_record where id = :id"
        val params = mapOf("id" to id.value)
        it.single(queryOf(sql, params), this::rowmapper)
    }
}

private fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
