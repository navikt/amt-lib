package no.nav.amt.lib.kafka

import no.nav.amt.lib.kafka.config.MAX_POLL_INTERVAL_MS
import org.apache.kafka.common.TopicPartition
import kotlin.math.min

internal class PartitionBackoffManager {
    private data class PartitionState(
        val retryCount: Int,
        val backoffUntil: Long,
    )

    private val state = mutableMapOf<TopicPartition, PartitionState>()

    fun incrementRetryCount(tp: TopicPartition) {
        val newRetryCount = (state[tp]?.retryCount ?: 0) + 1

        state[tp] = PartitionState(
            retryCount = newRetryCount,
            backoffUntil = calculateBackoffUntilMs(newRetryCount),
        )
    }

    fun resetRetryCount(tp: TopicPartition) {
        state.remove(tp)
    }

    fun isInBackoff(tp: TopicPartition): Boolean {
        val until = state[tp]?.backoffUntil ?: return false
        return System.currentTimeMillis() < until
    }

    // only used in tests outside this class
    internal fun calculateBackoffUntilMs(retryCount: Int): Long =
        System.currentTimeMillis() + min(COEFFICIENT * retryCount * retryCount + BASE_DELAY_MS, MAX_DELAY)

    // only used in tests
    internal fun getRetryCount(tp: TopicPartition) = state[tp]?.retryCount ?: 0

    companion object {
        private const val COEFFICIENT = 500L
        private const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY = MAX_POLL_INTERVAL_MS - 60_000L
    }
}
