package no.nav.amt.lib.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer

internal object KafkaPartitionUtils {
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
}
