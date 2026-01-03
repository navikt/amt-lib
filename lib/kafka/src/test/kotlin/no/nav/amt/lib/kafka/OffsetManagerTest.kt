package no.nav.amt.lib.kafka

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OffsetManagerTest {
    private lateinit var offsetManager: OffsetManager

    private val tp1 = TopicPartition("topic", 0)
    private val tp2 = TopicPartition("topic", 1)

    @BeforeEach
    fun setup() {
        offsetManager = OffsetManager()
    }

    @Test
    fun `markProcessed sets uncommitted offset and clears retry`() {
        offsetManager.markRetry(tp1, 10)
        offsetManager.markProcessed(tp1, 42)

        val offsetsToCommit = offsetManager.getOffsetsToCommit()
        offsetsToCommit shouldBe mapOf(tp1 to OffsetAndMetadata(42))

        offsetManager.getRetryOffsets() shouldBe emptyMap()
    }

    @Test
    fun `markRetry sets or updates retry offset`() {
        offsetManager.markRetry(tp1, 50)
        offsetManager.markRetry(tp1, 30) // skal coerceAtMost

        offsetManager.getRetryOffsets() shouldBe mapOf(tp1 to 30)
    }

    @Test
    fun `clearCommitted removes offsets`() {
        offsetManager.markProcessed(tp1, 100)
        offsetManager.markProcessed(tp2, 200)

        offsetManager.clearCommitted(tp1)

        offsetManager.getOffsetsToCommit() shouldBe mapOf(tp2 to OffsetAndMetadata(200))
    }

    @Test
    fun `clearRetry removes retry`() {
        offsetManager.markRetry(tp1, 10)
        offsetManager.clearRetry(tp1)

        offsetManager.getRetryOffsets() shouldBe emptyMap()
    }

    @Test
    fun `commit calls consumer and clears offsets`() {
        val consumer = mockk<KafkaConsumer<Any, Any>>(relaxed = true)

        offsetManager.markProcessed(tp1, 123)
        offsetManager.markProcessed(tp2, 456)

        offsetManager.commit(consumer)

        verify {
            consumer.commitSync(offsetManager.getOffsetsToCommit().mapValues { it.value })
        }

        offsetManager.getOffsetsToCommit() shouldBe emptyMap()
    }

    @Test
    fun `commit handles exception`() {
        val consumer = mockk<KafkaConsumer<Any, Any>>()
        offsetManager.markProcessed(tp1, 123)

        every {
            consumer.commitSync(any<Map<TopicPartition, OffsetAndMetadata>>())
        } throws RuntimeException("fail")

        shouldNotThrowAny {
            offsetManager.commit(consumer)
        }

        offsetManager.getOffsetsToCommit() shouldBe mapOf(tp1 to OffsetAndMetadata(123))
    }
}
