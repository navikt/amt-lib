package no.nav.amt.lib.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

internal object KafkaPartitionUtils {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <K, V> updatePartitionPauseState(consumer: KafkaConsumer<K, V>, backoffManager: PartitionBackoffManager) {
        // split partitions into those that can be processed and those that are in backoff
        val (backoffPartitions, processable) = consumer
            .assignment()
            .partition { tp -> backoffManager.isInBackoff(tp) }

        // get current paused partitions
        val paused = consumer.paused()

        // pause partitions that are in backoff and not already paused
        backoffPartitions
            .filterNot { it in paused }
            .takeIf { it.isNotEmpty() }
            ?.let { consumer.pause(it) }

        // resume paused partitions that are no longer in backoff
        processable
            .filter { it in paused }
            .takeIf { it.isNotEmpty() }
            ?.let { consumer.resume(it) }
    }

    fun <K, V> retryFailedPartitions(consumer: KafkaConsumer<K, V>, retryOffsets: Map<TopicPartition, Long>) {
        retryOffsets.forEach { (tp, offset) ->
            try {
                val current = consumer.position(tp)
                if (current != offset) consumer.seek(tp, offset)
                log.debug("Retrying {} from offset {} (was {})", tp, offset, current)
            } catch (e: IllegalStateException) {
                log.warn("Partition $tp not assigned during retry seek", e)
            }
        }
    }
}
