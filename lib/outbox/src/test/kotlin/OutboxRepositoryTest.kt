import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.lib.OutboxEvent
import no.nav.amt.lib.OutboxEventStatus
import no.nav.amt.lib.OutboxRepository
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.objectMapper
import java.time.ZonedDateTime
import kotlin.test.Test

class OutboxRepositoryTest {
    init {
        SingletonPostgres16Container
    }

    @Test
    fun `test outbox repository`() {
        val repo = OutboxRepository()
        val event = OutboxEvent(
            aggregateId = "test-aggregate-id",
            aggregateType = "test-aggregate-type",
            topic = "test-topic",
            payload = objectMapper.createObjectNode().put("key", "value"),
            createdAt = ZonedDateTime.now(),
            processedAt = null,
            status = OutboxEventStatus.PENDING,
            retryCount = 0,
            errorMessage = null,
        )

        val eventWithId = repo.insert(event)

        eventWithId.id shouldNotBe null
    }

    @Test
    fun `findUnprocessedEvents returns pending and failed events`() {
        val repo = OutboxRepository()
        val pendingEvent = OutboxEvent(
            aggregateId = "agg-1",
            aggregateType = "type-1",
            topic = "topic-1",
            payload = objectMapper.createObjectNode().put("key", "pending"),
            createdAt = ZonedDateTime.now(),
            processedAt = null,
            status = OutboxEventStatus.PENDING,
            retryCount = 0,
            errorMessage = null,
        )
        val failedEvent = pendingEvent.copy(
            aggregateId = "agg-2",
            payload = objectMapper.createObjectNode().put("key", "failed"),
            status = OutboxEventStatus.FAILED,
        )
        val processedEvent = pendingEvent.copy(
            aggregateId = "agg-3",
            payload = objectMapper.createObjectNode().put("key", "processed"),
            status = OutboxEventStatus.PROCESSED,
        )
        repo.insert(pendingEvent)
        repo.insert(failedEvent)
        repo.insert(processedEvent)

        val result = repo.findUnprocessedEvents(10)
        result.map { it.status }.toSet().find { it == OutboxEventStatus.PROCESSED } shouldBe null
        result.any { it.aggregateId == "agg-1" } shouldBe true
        result.any { it.aggregateId == "agg-2" } shouldBe true
        result.any { it.aggregateId == "agg-3" } shouldBe false
    }

    @Test
    fun `markAsProcessed updates event status and processedAt`() {
        val repo = OutboxRepository()
        val event = OutboxEvent(
            aggregateId = "agg-4",
            aggregateType = "type-2",
            topic = "topic-2",
            payload = objectMapper.createObjectNode().put("key", "to-process"),
            createdAt = ZonedDateTime.now(),
            processedAt = null,
            status = OutboxEventStatus.PENDING,
            retryCount = 0,
            errorMessage = null,
        )
        val inserted = repo.insert(event)
        repo.markAsProcessed(inserted.id!!)
        val updated = repo.get(inserted.id!!)
        updated?.status shouldBe OutboxEventStatus.PROCESSED
        updated?.processedAt shouldNotBe null
    }

    @Test
    fun `markAsFailed updates event status, error message, and retry count`() {
        val repo = OutboxRepository()
        val event = OutboxEvent(
            aggregateId = "agg-5",
            aggregateType = "type-3",
            topic = "topic-3",
            payload = objectMapper.createObjectNode().put("key", "to-fail"),
            createdAt = ZonedDateTime.now(),
            processedAt = null,
            status = OutboxEventStatus.PENDING,
            retryCount = 0,
            errorMessage = null,
        )
        val inserted = repo.insert(event)
        val errorMsg = "Something went wrong"
        repo.markAsFailed(inserted.id!!, errorMsg)
        val failed = repo.get(inserted.id!!)
        failed?.status shouldBe OutboxEventStatus.FAILED
        failed?.errorMessage shouldBe errorMsg
        failed?.retryCount shouldBe 1
    }

    @Test
    fun `markAsProcessing updates event status to PROCESSING`() {
        val repo = OutboxRepository()
        val event = OutboxEvent(
            aggregateId = "agg-6",
            aggregateType = "type-4",
            topic = "topic-4",
            payload = objectMapper.createObjectNode().put("key", "to-process"),
            createdAt = ZonedDateTime.now(),
            processedAt = null,
            status = OutboxEventStatus.PENDING,
            retryCount = 0,
            errorMessage = null,
        )
        val inserted = repo.insert(event)
        repo.markAsProcessing(inserted.id!!)
        val processing = repo.get(inserted.id!!)
        processing?.status shouldBe OutboxEventStatus.PROCESSING
    }
}
