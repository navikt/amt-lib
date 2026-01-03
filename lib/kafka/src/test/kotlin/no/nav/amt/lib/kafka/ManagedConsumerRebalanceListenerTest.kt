package no.nav.amt.lib.kafka

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ManagedConsumerRebalanceListenerTest {
    private val consumer: KafkaConsumer<String, String> = mockk(relaxed = true)
    private val offsetManager: OffsetManager = mockk(relaxed = true)
    private val backoffManager: PartitionBackoffManager = mockk(relaxed = true)
    private val listener = ManagedConsumerRebalanceListener(consumer, offsetManager, backoffManager)

    private val tp1 = TopicPartition("topic", 0)
    private val tp2 = TopicPartition("topic", 1)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `onPartitionsRevoked commits offsets and clears state`() {
        val offsetsToCommit = mapOf<TopicPartition, OffsetAndMetadata>(tp1 to mockk(), tp2 to mockk())
        every { offsetManager.getOffsetsToCommit() } returns offsetsToCommit

        listener.onPartitionsRevoked(listOf(tp1, tp2))

        verify { consumer.commitSync(offsetsToCommit) }

        verify { offsetManager.clearCommitted(tp1) }
        verify { offsetManager.clearCommitted(tp2) }

        verify { offsetManager.clearRetry(tp1) }
        verify { offsetManager.clearRetry(tp2) }

        verify { backoffManager.resetRetryCount(tp1) }
        verify { backoffManager.resetRetryCount(tp2) }
    }

    @Test
    fun `onPartitionsAssigned logs assigned partitions`() {
        shouldNotThrowAny {
            listener.onPartitionsAssigned(listOf(tp1, tp2))
        }
    }
}
