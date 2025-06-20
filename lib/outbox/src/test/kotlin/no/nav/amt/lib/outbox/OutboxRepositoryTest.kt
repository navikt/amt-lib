package no.nav.amt.lib.outbox

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import kotlin.test.Test

class OutboxRepositoryTest {
    init {
        SingletonPostgres16Container
    }

    @Test
    fun `test outbox repository`() {
        val repo = OutboxRepository()
        val event = NewOutboxEvent(
            aggregateId = "test-aggregate-id",
            aggregateType = "test-aggregate-type",
            topic = "test-topic",
            payload = objectMapper.createObjectNode().put("key", "value"),
        )

        val eventWithId = repo.insertNewEvent(event)

        eventWithId.id shouldNotBe null
    }

    @Test
    fun `findUnprocessedEvents returns pending and failed events`() {
        val repo = OutboxRepository()
        val pendingEvent = NewOutboxEvent(
            aggregateId = "agg-1",
            aggregateType = "type-1",
            topic = "topic-1",
            payload = objectMapper.createObjectNode().put("key", "pending"),
        )
        repo.insertNewEvent(pendingEvent)
        val failedEvent = pendingEvent.copy(
            aggregateId = "agg-2",
            payload = objectMapper.createObjectNode().put("key", "failed"),
        )
        repo.insertNewEvent(failedEvent).also {
            repo.markAsFailed(it.id, "Some error")
        }
        val processedEvent = pendingEvent.copy(
            aggregateId = "agg-3",
            payload = objectMapper.createObjectNode().put("key", "processed"),
        )
        repo.insertNewEvent(processedEvent).also { repo.markAsProcessed(it.id) }

        val result = repo.findUnprocessedEvents(10)
        result.map { it.status }.toSet().find { it == OutboxEventStatus.PROCESSED } shouldBe null
        result.any { it.aggregateId == "agg-1" } shouldBe true
        result.any { it.aggregateId == "agg-2" } shouldBe true
        result.any { it.aggregateId == "agg-3" } shouldBe false
    }

    @Test
    fun `markAsProcessed updates event status and processedAt`() {
        val repo = OutboxRepository()
        val event = NewOutboxEvent(
            aggregateId = "agg-4",
            aggregateType = "type-2",
            topic = "topic-2",
            payload = objectMapper.createObjectNode().put("key", "to-process"),
        )
        val inserted = repo.insertNewEvent(event)
        repo.markAsProcessed(inserted.id)
        val updated = repo.get(inserted.id)
        updated?.status shouldBe OutboxEventStatus.PROCESSED
        updated?.processedAt shouldNotBe null
    }

    @Test
    fun `markAsFailed updates event status, error message, and retry count`() {
        val repo = OutboxRepository()
        val event = NewOutboxEvent(
            aggregateId = "agg-5",
            aggregateType = "type-3",
            topic = "topic-3",
            payload = objectMapper.createObjectNode().put("key", "to-fail"),
        )
        val inserted = repo.insertNewEvent(event)
        val errorMsg = "Something went wrong"
        repo.markAsFailed(inserted.id, errorMsg)
        val failed = repo.get(inserted.id)
        failed?.status shouldBe OutboxEventStatus.FAILED
        failed?.errorMessage shouldBe errorMsg
        failed?.retryCount shouldBe 1
    }

    @Test
    fun `markAsProcessing updates event status to PROCESSING`() {
        val repo = OutboxRepository()
        val event = NewOutboxEvent(
            aggregateId = "agg-6",
            aggregateType = "type-4",
            topic = "topic-4",
            payload = objectMapper.createObjectNode().put("key", "to-process"),
        )
        val inserted = repo.insertNewEvent(event)
        repo.markAsProcessing(inserted.id)
        val processing = repo.get(inserted.id)
        processing?.status shouldBe OutboxEventStatus.PROCESSING
    }
}
