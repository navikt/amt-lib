package no.nav.amt.lib.kafka

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.lib.kafka.KafkaPartitionUtils.updatePartitionPauseState
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KafkaPartitionUtilsTest {
    private val consumer = mockk<KafkaConsumer<String, String>>(relaxed = true)
    private val backoffManager = PartitionBackoffManager()

    private val tp1 = TopicPartition("topic", 0)
    private val tp2 = TopicPartition("topic", 1)

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { consumer.assignment() } returns setOf(tp1, tp2)
    }

    @Test
    fun `updatePartitionPauseState pauses and resumes partitions correctly`() {
        // tp1 already paused
        every { consumer.paused() } returns setOf(tp1)

        // backoff state: tp2 in backoff, tp1 not in backoff
        repeat(20) { backoffManager.incrementRetryCount(tp2) }

        // act
        updatePartitionPauseState(consumer, backoffManager)

        // tp2 should be paused
        verify { consumer.pause(listOf(tp2)) }

        // tp1 should be resumed
        verify { consumer.resume(any()) }
    }
}
