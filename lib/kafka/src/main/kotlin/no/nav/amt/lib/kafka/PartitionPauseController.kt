package no.nav.amt.lib.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer

/**
 * Controls pausing and resuming of Kafka partitions based on backoff state.
 *
 * Works together with [PartitionBackoffManager] to temporarily pause partitions
 * that have experienced consecutive failures, preventing tight retry loops.
 *
 * Periodically called in the consumer loop to adjust which partitions are paused.
 */
internal class PartitionPauseController(
    private val backoffManager: PartitionBackoffManager,
) {
    /**
     * Updates the paused state of partitions in the given Kafka consumer.
     *
     * - Partitions currently in backoff that are not already paused are paused.
     * - Partitions previously paused but no longer in backoff are resumed.
     *
     * @param consumer the KafkaConsumer whose partition pause state is to be updated
     */
    fun update(consumer: KafkaConsumer<*, *>) {
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
