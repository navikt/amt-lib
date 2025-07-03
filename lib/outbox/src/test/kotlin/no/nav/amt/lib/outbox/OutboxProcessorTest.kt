package no.nav.amt.lib.outbox

import io.kotest.matchers.shouldBe
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.outbox.metrics.PrometheusOutboxMeter
import no.nav.amt.lib.outbox.utils.assertProduced
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.testing.shouldBeCloseTo
import no.nav.amt.lib.utils.job.JobManager
import no.nav.amt.lib.utils.objectMapper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OutboxProcessorTest {
    init {
        SingletonPostgres16Container
        SingletonKafkaProvider.start()
    }

    private val prometheusRegistry = PrometheusRegistry()
    private val outboxRepository = OutboxRepository()
    private val outboxService = OutboxService(PrometheusOutboxMeter(prometheusRegistry))

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
    fun `processRecords - new record - gets_processed`() {
        val record = newRecord()
        outboxProcessor.processRecords()

        val processedRecord = outboxRepository.get(record.id)!!
        processedRecord.processedAt!! shouldBeCloseTo LocalDateTime.now()
        processedRecord.status shouldBe OutboxRecordStatus.PROCESSED
        verifyProducedRecord(record)
    }

    @Test
    fun `processRecords - producer fails - marks record as FAILED`() {
        val record = newRecord(topic = failingTestTopic)

        outboxProcessor.processRecords()

        val failedRecord = outboxRepository.get(record.id)!!
        failedRecord.status shouldBe OutboxRecordStatus.FAILED
    }

    @Test
    fun `processRecords - previous record failed - skips new record for same aggregate`() {
        val key = UUID.randomUUID()

        val recordToFail = newRecord(key = key, topic = failingTestTopic)
        val recordToIgnore = newRecord(key = key, topic = failingTestTopic)

        outboxProcessor.processRecords()
        outboxRepository.get(recordToFail.id)!!.status shouldBe OutboxRecordStatus.FAILED
        outboxRepository.get(recordToIgnore.id)!!.status shouldBe OutboxRecordStatus.PENDING
    }

    @Test
    fun `processRecords - multiple records - all processRecords successfully`() {
        val records = newRecords(5)

        outboxProcessor.processRecords()

        records.forEach { record ->
            val processedRecord = outboxRepository.get(record.id)!!
            processedRecord.status shouldBe OutboxRecordStatus.PROCESSED
            processedRecord.processedAt!! shouldBeCloseTo LocalDateTime.now()
            verifyProducedRecord(record)
        }
    }

    @Test
    fun `processRecords - mixed success and failure - handles both correctly`() {
        val successRecords = newRecords(3, testTopic)
        val failRecords = newRecords(2, failingTestTopic)

        outboxProcessor.processRecords()

        successRecords.forEach { record ->
            val processedRecord = outboxRepository.get(record.id)!!
            processedRecord.status shouldBe OutboxRecordStatus.PROCESSED
            verifyProducedRecord(record)
        }

        failRecords.forEach { record ->
            val failedRecord = outboxRepository.get(record.id)!!
            failedRecord.status shouldBe OutboxRecordStatus.FAILED
        }
    }

    @Test
    fun `processRecords - failed records are retried on next run`() {
        val record = newRecord(topic = failingTestTopic)

        outboxProcessor.processRecords()
        val firstResult = outboxRepository.get(record.id)!!
        firstResult.status shouldBe OutboxRecordStatus.FAILED
        val firstRetryCount = firstResult.retryCount

        outboxProcessor.processRecords()
        val secondResult = outboxRepository.get(record.id)!!
        secondResult.status shouldBe OutboxRecordStatus.FAILED
        secondResult.retryCount shouldBe (firstRetryCount + 1)
    }

    @Test
    fun `process - same aggregate different topics - processRecords independently`() {
        val key = UUID.randomUUID()

        val failingRecord = newRecord(key = key, topic = failingTestTopic)
        val successRecord = newRecord(key = key, topic = testTopic)

        outboxProcessor.processRecords()

        outboxRepository.get(failingRecord.id)!!.status shouldBe OutboxRecordStatus.FAILED
        outboxRepository.get(successRecord.id)!!.status shouldBe OutboxRecordStatus.PROCESSED
        verifyProducedRecord(successRecord)
    }

    @Test
    fun `processRecords - records processed in creation order`() {
        val records = newRecords(3)

        outboxProcessor.processRecords()

        val processedRecords = records.map { outboxRepository.get(it.id)!! }
        processedRecords.forEach {
            it.status shouldBe OutboxRecordStatus.PROCESSED
        }
        val sortedByProcessedAt = processedRecords.sortedBy { it.processedAt!! }
        val sortedById = processedRecords.sortedBy { it.id.value }

        sortedByProcessedAt.map { it.id } shouldBe sortedById.map { it.id }
    }

    private data class Value(
        val name: String = "Test",
        val values: List<Int> = listOf(1, 2, 3),
    )

    private fun newRecord(
        value: Any = Value(),
        key: UUID = UUID.randomUUID(),
        topic: String = testTopic,
    ) = outboxService.insertRecord(
        key = key,
        value = value,
        topic = topic,
    )

    private fun newRecords(
        count: Int,
        topic: String = testTopic,
        key: UUID? = null,
    ) = (1..count).map {
        newRecord(
            value = Value("Test-$it"),
            key = key ?: UUID.randomUUID(),
            topic = topic,
        )
    }

    private fun verifyProducedRecord(record: OutboxRecord, topic: String = testTopic) = assertProduced(topic) {
        AsyncUtils.eventually {
            val value = objectMapper.readTree(it[record.key])

            value shouldBe record.value
        }
    }
}
