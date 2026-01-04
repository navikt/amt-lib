package no.nav.amt.lib.kafka

import kotlinx.coroutines.CancellationException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

internal class PartitionProcessor<K, V>(
    private val consume: suspend (K, V) -> Unit,
    private val backoffManager: PartitionBackoffManager,
    private val offsetManager: OffsetManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun process(topicPartition: TopicPartition, records: List<ConsumerRecord<K, V>>) {
        for (record in records) {
            try {
                consume(record.key(), record.value())

                offsetManager.markProcessed(topicPartition, record.offset() + 1)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                log.warn("Failed processing $topicPartition offset=${record.offset()}", t)
                offsetManager.markRetry(topicPartition, record.offset())
                backoffManager.incrementRetryCount(topicPartition)
                break // stop on first failure in partition
            }
        }

        if (topicPartition !in offsetManager.getRetryOffsets()) {
            backoffManager.resetRetryCount(topicPartition)
        }
    }
}
