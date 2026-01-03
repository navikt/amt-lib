package no.nav.amt.lib.kafka

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OffsetManagerTest {
    private lateinit var sut: OffsetManager

    private val tp1 = TopicPartition("topic", 0)
    private val tp2 = TopicPartition("topic", 1)

    @BeforeEach
    fun setup() {
        sut = OffsetManager()
    }

    @Test
    fun `markProcessed sets uncommitted offset and clears retry`() {
        sut.markRetry(tp1, 10)
        sut.markProcessed(tp1, 42)

        val offsetsToCommit = sut.getOffsetsToCommit()
        offsetsToCommit shouldBe mapOf(tp1 to OffsetAndMetadata(42))

        sut.getRetryOffsets() shouldBe emptyMap()
    }

    @Test
    fun `markRetry sets or updates retry offset`() {
        sut.markRetry(tp1, 50)
        sut.markRetry(tp1, 30) // skal coerceAtMost

        sut.getRetryOffsets() shouldBe mapOf(tp1 to 30)
    }

    @Test
    fun `clearCommitted removes offsets`() {
        sut.markProcessed(tp1, 100)
        sut.markProcessed(tp2, 200)

        sut.clearCommitted(tp1)

        sut.getOffsetsToCommit() shouldBe mapOf(tp2 to OffsetAndMetadata(200))
    }

    @Test
    fun `clearRetry removes retry`() {
        sut.markRetry(tp1, 10)
        sut.clearRetry(tp1)

        sut.getRetryOffsets() shouldBe emptyMap()
    }

    @Nested
    inner class CommitTests {
        @Test
        fun `commit calls consumer and clears offsets`() {
            val consumer = mockk<KafkaConsumer<Any, Any>>(relaxed = true)

            sut.markProcessed(tp1, 123)
            sut.markProcessed(tp2, 456)

            sut.commit(consumer)

            verify {
                consumer.commitSync(sut.getOffsetsToCommit().mapValues { it.value })
            }

            sut.getOffsetsToCommit() shouldBe emptyMap()
        }

        @Test
        fun `commit handles exception`() {
            val consumer = mockk<KafkaConsumer<Any, Any>>()
            sut.markProcessed(tp1, 123)

            every {
                consumer.commitSync(any<Map<TopicPartition, OffsetAndMetadata>>())
            } throws RuntimeException("fail")

            shouldNotThrowAny {
                sut.commit(consumer)
            }

            sut.getOffsetsToCommit() shouldBe mapOf(tp1 to OffsetAndMetadata(123))
        }
    }

    @Nested
    inner class RetryFailedPartitionsTests {
        val consumer = mockk<KafkaConsumer<Any, Any>>()

        @Test
        fun `retryFailedPartitions seeks correctly`() {
            every { consumer.seek(any<TopicPartition>(), any<Long>()) } just Runs
            every { consumer.position(tp2) } returns 101L

            sut.markRetry(tp2, 100L)
            sut.retryFailedPartitions(consumer)

            verify { consumer.seek(tp2, 100L) }
        }

        @Test
        fun `retryFailedPartitions skips seek when offset is already greater than seek offset`() {
            every { consumer.position(tp2) } returns 101L

            sut.markRetry(tp2, 101L)

            verify(exactly = 0) { consumer.seek(tp2, any<Long>()) }
        }
    }
}
