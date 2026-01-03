package no.nav.amt.lib.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

internal class OffsetManager {
    private val log = LoggerFactory.getLogger(javaClass)

    private val uncommittedOffsets = mutableMapOf<TopicPartition, OffsetAndMetadata>()
    private val retryOffsets = mutableMapOf<TopicPartition, Long>()

    fun markProcessed(tp: TopicPartition, offset: Long) {
        uncommittedOffsets[tp] = OffsetAndMetadata(offset)
        retryOffsets.remove(tp)
    }

    fun markRetry(tp: TopicPartition, offset: Long) {
        retryOffsets[tp] = retryOffsets[tp]
            ?.coerceAtMost(offset)
            ?: offset
    }

    fun getRetryOffsets(): Map<TopicPartition, Long> = retryOffsets.toMap()

    fun getOffsetsToCommit(): Map<TopicPartition, OffsetAndMetadata> = uncommittedOffsets.toMap()

    fun clearCommitted(tp: TopicPartition) = uncommittedOffsets.remove(tp)

    fun clearRetry(tp: TopicPartition) = retryOffsets.remove(tp)

    fun commit(consumer: KafkaConsumer<*, *>) {
        if (uncommittedOffsets.isEmpty()) return
        try {
            consumer.commitSync(uncommittedOffsets)
            log.info("Offsets committed: $uncommittedOffsets")
            uncommittedOffsets.clear()
        } catch (e: Exception) {
            log.error("Commit failed for offsets $uncommittedOffsets", e)
        }
    }
}
