package no.nav.amt.lib.kafka

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PartitionPauseControllerTest {
    private val consumer = mockk<KafkaConsumer<String, String>>(relaxed = true)
    private lateinit var backoffManager: PartitionBackoffManager
    private lateinit var sut: PartitionPauseController

    private val tp1 = TopicPartition("topic", 0)
    private val tp2 = TopicPartition("topic", 1)

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { consumer.assignment() } returns setOf(tp1, tp2)
        every { consumer.paused() } returns emptySet()

        backoffManager = PartitionBackoffManager()
        sut = PartitionPauseController(backoffManager)
    }

    @Test
    fun `pauses partitions that are in backoff`() {
        every { consumer.paused() } returns emptySet()

        backoffManager.incrementRetryCount(tp1)

        sut.update(consumer)

        verify { consumer.pause(listOf(tp1)) }
        verify(exactly = 0) { consumer.resume(any()) }
    }

    @Test
    fun `resumes paused partitions that are no longer in backoff`() {
        every { consumer.paused() } returns setOf(tp1)

        sut.update(consumer)

        verify { consumer.resume(listOf(tp1)) }
        verify(exactly = 0) { consumer.pause(any()) }
    }

    @Test
    fun `does not pause partitions that are already paused`() {
        every { consumer.paused() } returns setOf(tp1)
        backoffManager.incrementRetryCount(tp1)

        sut.update(consumer)

        verify(exactly = 0) { consumer.pause(any()) }
    }

    @Test
    fun `does not resume partitions that are not paused`() {
        every { consumer.paused() } returns emptySet()

        sut.update(consumer)

        verify(exactly = 0) { consumer.resume(any()) }
    }

    @Test
    fun `handles mixed paused and backoff state`() {
        every { consumer.paused() } returns setOf(tp1)
        backoffManager.incrementRetryCount(tp2)

        sut.update(consumer)

        verify {
            consumer.pause(listOf(tp2))
            consumer.resume(listOf(tp1))
        }
    }

    @Test
    fun `does nothing when all partitions are processable`() {
        every { consumer.paused() } returns emptySet()

        sut.update(consumer)

        verify(exactly = 0) {
            consumer.pause(any())
            consumer.resume(any())
        }
    }
}
