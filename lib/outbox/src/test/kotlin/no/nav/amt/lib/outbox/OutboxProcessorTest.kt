package no.nav.amt.lib.outbox

import io.kotest.matchers.shouldBe
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.outbox.utils.assertProduced
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import no.nav.amt.lib.utils.job.JobManager
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class OutboxProcessorTest {
    init {
        SingletonPostgres16Container
        SingletonKafkaProvider.start()
    }

    private val outboxRepository = OutboxRepository()
    private val outboxService = OutboxService()

    private val kafakConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost())
    private val kafkaProducer = Producer<String, String>(kafakConfig)

    private val outboxProcessor = OutboxProcessor(
        service = outboxService,
        jobManager = JobManager({ true }, { true }),
        producer = kafkaProducer,
    )

    private val testTopic = "outbox-test-topic"
    private val failingTestTopic = "INVALID TOPIC NAME!"

    @Test
    fun `process - new event - gets processed`() {
        val event = newEvent()
        outboxProcessor.process()

        val processedEvent = outboxRepository.get(event.id)!!
        processedEvent.processedAt!! shouldBeCloseTo ZonedDateTime.now()
        processedEvent.status shouldBe OutboxEventStatus.PROCESSED
        verifyProducedEvent(event)
    }

    @Test
    fun `process - producer fails - marks event as FAILED`() {
        val event = newEvent(topic = failingTestTopic)

        outboxProcessor.process()

        val failedEvent = outboxRepository.get(event.id)!!
        failedEvent.status shouldBe OutboxEventStatus.FAILED
    }

    @Test
    fun `process - previous event failed - skips new event for same aggregate`() {
        val key = UUID.randomUUID()

        val eventToFail = newEvent(key = key, topic = failingTestTopic)
        val eventToIgnore = newEvent(key = key, topic = failingTestTopic)

        outboxProcessor.process()
        outboxRepository.get(eventToFail.id)!!.status shouldBe OutboxEventStatus.FAILED
        outboxRepository.get(eventToIgnore.id)!!.status shouldBe OutboxEventStatus.PENDING
    }

    @Test
    fun `process - multiple events - all process successfully`() {
        val events = newEvents(5)

        outboxProcessor.process()

        events.forEach { event ->
            val processedEvent = outboxRepository.get(event.id)!!
            processedEvent.status shouldBe OutboxEventStatus.PROCESSED
            processedEvent.processedAt!! shouldBeCloseTo ZonedDateTime.now()
            verifyProducedEvent(event)
        }
    }

    @Test
    fun `process - mixed success and failure - handles both correctly`() {
        val successEvents = newEvents(3, testTopic)
        val failEvents = newEvents(2, failingTestTopic)

        outboxProcessor.process()

        successEvents.forEach { event ->
            val processedEvent = outboxRepository.get(event.id)!!
            processedEvent.status shouldBe OutboxEventStatus.PROCESSED
            verifyProducedEvent(event)
        }

        failEvents.forEach { event ->
            val failedEvent = outboxRepository.get(event.id)!!
            failedEvent.status shouldBe OutboxEventStatus.FAILED
        }
    }

    @Test
    fun `process - failed events are retried on next run`() {
        val event = newEvent(topic = failingTestTopic)

        outboxProcessor.process()
        val firstResult = outboxRepository.get(event.id)!!
        firstResult.status shouldBe OutboxEventStatus.FAILED
        val firstRetryCount = firstResult.retryCount

        outboxProcessor.process()
        val secondResult = outboxRepository.get(event.id)!!
        secondResult.status shouldBe OutboxEventStatus.FAILED
        secondResult.retryCount shouldBe (firstRetryCount + 1)
    }

    @Test
    fun `process - same aggregate different topics - process independently`() {
        val key = UUID.randomUUID()

        val failingEvent = newEvent(key = key, topic = failingTestTopic)
        val successEvent = newEvent(key = key, topic = testTopic)

        outboxProcessor.process()

        outboxRepository.get(failingEvent.id)!!.status shouldBe OutboxEventStatus.FAILED
        outboxRepository.get(successEvent.id)!!.status shouldBe OutboxEventStatus.PROCESSED
        verifyProducedEvent(successEvent)
    }

    @Test
    fun `process - events processed in creation order`() {
        val events = newEvents(3)

        outboxProcessor.process()

        val processedEvents = events.map { outboxRepository.get(it.id)!! }
        processedEvents.forEach {
            it.status shouldBe OutboxEventStatus.PROCESSED
        }
        val sortedByProcessedAt = processedEvents.sortedBy { it.processedAt!! }
        val sortedById = processedEvents.sortedBy { it.id.value }

        sortedByProcessedAt.map { it.id } shouldBe sortedById.map { it.id }
    }

    private data class Value(
        val name: String = "Test",
        val values: List<Int> = listOf(1, 2, 3),
    )

    private fun newEvent(
        value: Any = Value(),
        key: UUID = UUID.randomUUID(),
        topic: String = testTopic,
    ) = outboxService.newEvent(
        key = key,
        value = value,
        topic = topic,
    )

    private fun newEvents(
        count: Int,
        topic: String = testTopic,
        key: UUID? = null,
    ) = (1..count).map {
        newEvent(
            value = Value("Test-$it"),
            key = key ?: UUID.randomUUID(),
            topic = topic,
        )
    }

    private fun verifyProducedEvent(event: OutboxEvent, topic: String = testTopic) = assertProduced(topic) {
        AsyncUtils.eventually {
            val value = objectMapper.readTree(it[event.key])

            value shouldBe event.value
        }
    }
}
