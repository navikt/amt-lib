package no.nav.amt.lib.outbox

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import kotlin.test.Test

class OutboxRepositoryTest {
    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
    }

    @Test
    fun `test outbox repository`(): Unit = runBlocking {
        val repo = OutboxRepository()
        val record = NewOutboxRecord(
            key = "test-key",
            valueType = "test-value-type",
            topic = "test-topic",
            value = objectMapper.createObjectNode().put("key", "value"),
        )

        val recordWithId = repo.insertNewRecord(record)

        recordWithId.id shouldNotBe null
    }

    @Test
    fun `findUnprocessedRecords returns pending and failed records`(): Unit = runBlocking {
        val repo = OutboxRepository()
        val pendingRecord = NewOutboxRecord(
            key = "key-1",
            valueType = "type-1",
            topic = "topic-1",
            value = objectMapper.createObjectNode().put("key", "pending"),
        )
        repo.insertNewRecord(pendingRecord)
        val failedRecord = pendingRecord.copy(
            key = "key-2",
            value = objectMapper.createObjectNode().put("key", "failed"),
        )
        repo.insertNewRecord(failedRecord).also {
            repo.markAsFailed(it.id, "Some error")
        }
        val processedRecord = pendingRecord.copy(
            key = "key-3",
            value = objectMapper.createObjectNode().put("key", "processed"),
        )
        repo.insertNewRecord(processedRecord).also { repo.markAsProcessed(it.id) }

        val result = repo.findUnprocessedRecords(10)
        result.map { it.status }.toSet().find { it == OutboxRecordStatus.PROCESSED } shouldBe null
        result.any { it.key == "key-1" } shouldBe true
        result.any { it.key == "key-2" } shouldBe true
        result.any { it.key == "key-3" } shouldBe false
    }

    @Test
    fun `markAsProcessed updates record status and processedAt`(): Unit = runBlocking {
        val repo = OutboxRepository()
        val record = NewOutboxRecord(
            key = "key-4",
            valueType = "type-2",
            topic = "topic-2",
            value = objectMapper.createObjectNode().put("key", "to-process"),
        )
        val inserted = repo.insertNewRecord(record)
        repo.markAsProcessed(inserted.id)
        val updated = repo.get(inserted.id)
        updated?.status shouldBe OutboxRecordStatus.PROCESSED
        updated?.processedAt shouldNotBe null
    }

    @Test
    fun `markAsFailed updates record status, error message, and retry count`(): Unit = runBlocking {
        val repo = OutboxRepository()
        val record = NewOutboxRecord(
            key = "key-5",
            valueType = "type-3",
            topic = "topic-3",
            value = objectMapper.createObjectNode().put("key", "to-fail"),
        )
        val inserted = repo.insertNewRecord(record)
        val errorMsg = "Something went wrong"
        repo.markAsFailed(inserted.id, errorMsg)
        val failed = repo.get(inserted.id)
        failed?.status shouldBe OutboxRecordStatus.FAILED
        failed?.errorMessage shouldBe errorMsg
        failed?.retryCount shouldBe 1
    }
}
