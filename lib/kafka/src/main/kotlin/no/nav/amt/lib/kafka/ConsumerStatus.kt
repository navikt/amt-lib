package no.nav.amt.lib.kafka

import no.nav.amt.lib.kafka.config.MAX_POLL_INTERVAL_MS
import org.apache.kafka.common.TopicPartition
import kotlin.math.min

class ConsumerStatus {
    private val retriesInternal = mutableMapOf<TopicPartition, Int>()
    private val partitionBackoffUntil = mutableMapOf<TopicPartition, Long>()

    fun incrementRetryCount(tp: TopicPartition) {
        val newRetryCount = retriesInternal.compute(tp) { _, current -> (current ?: 0) + 1 }!!
        partitionBackoffUntil[tp] = System.currentTimeMillis() + backoffDuration(newRetryCount)
    }

    fun resetRetryCount(tp: TopicPartition) {
        retriesInternal.remove(tp)
        partitionBackoffUntil.remove(tp)
    }

    fun canProcessPartition(tp: TopicPartition): Boolean {
        val until = partitionBackoffUntil[tp] ?: return true
        return System.currentTimeMillis() >= until
    }

    fun backoffDuration(retryCount: Int): Long = min(COEFFICIENT * retryCount * retryCount + BASE_DELAY_MS, MAX_DELAY)

    // only used in tests
    fun retryCount(tp: TopicPartition) = retriesInternal[tp] ?: 0

    companion object {
        private const val COEFFICIENT = 500L
        private const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY = MAX_POLL_INTERVAL_MS - 60_000L
    }
}
