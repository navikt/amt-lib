package no.nav.amt.lib.kafka

import no.nav.amt.lib.kafka.config.MAX_POLL_INTERVAL_MS
import org.apache.kafka.common.TopicPartition
import kotlin.math.min

class ConsumerStatus {
    private val retriesInternal = mutableMapOf<TopicPartition, Int>()
    private val partitionBackoffUntil = mutableMapOf<TopicPartition, Long>()

    private fun backoffDuration(retryCount: Int): Long = min(COEFFICIENT * retryCount * retryCount + BASE_DELAY_MS, MAX_DELAY)

    fun backoffDuration(tp: TopicPartition): Long {
        val retryCount = retriesInternal[tp] ?: 0
        return backoffDuration(retryCount)
    }

    fun retryCount(tp: TopicPartition) = retriesInternal[tp] ?: 0

    fun resetRetryCount(tp: TopicPartition) {
        retriesInternal.remove(tp)
        partitionBackoffUntil.remove(tp)
    }

    fun incrementRetryCount(tp: TopicPartition) {
        val newRetryCount = retriesInternal.compute(tp) { _, current -> (current ?: 0) + 1 }!!
        partitionBackoffUntil[tp] = System.currentTimeMillis() + backoffDuration(newRetryCount)
    }

    fun canProcessPartition(tp: TopicPartition): Boolean {
        val until = partitionBackoffUntil[tp] ?: return true
        return System.currentTimeMillis() >= until
    }

    fun getDelayWhenAllPartitionsAreInRetry(partitions: Collection<TopicPartition>): Long? {
        if (partitions.any { canProcessPartition(it) }) return null

        val now = System.currentTimeMillis()
        val stillInBackoff = partitions.mapNotNull { tp ->
            partitionBackoffUntil[tp]?.takeIf { it > now }
        }

        return stillInBackoff.minOrNull()?.minus(now)
    }

    companion object {
        private const val COEFFICIENT = 500L
        private const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY = MAX_POLL_INTERVAL_MS - 60_000L
    }
}
