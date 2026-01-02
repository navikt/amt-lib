package no.nav.amt.lib.kafka

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ManagedKafkaConsumer handles Kafka consumption with:
 *  - per-partition retries
 *  - offset tracking
 *  - safe commit on shutdown or rebalance
 *
 * uncommittedOffsets tracks the highest successfully processed offset per partition.
 * retryOffsets tracks the earliest offset that must be retried.
 *
 * A partition may temporarily exist in both maps if some records were processed
 * successfully before a failure occurred.
 */
class ManagedKafkaConsumer<K, V>(
    private val topic: String,
    private val config: Map<String, Any>,
    private val pollTimeoutMs: Long = 1000L,
    private val consume: suspend (key: K, value: V) -> Unit,
) : Consumer<K, V> {
    private val log = LoggerFactory.getLogger(javaClass)

    // single-threaded KafkaConsumer dispatcher
    private val dispatcher = Executors
        .newSingleThreadExecutor { r -> Thread(r, "kafka-consumer-$topic") }
        .asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)

    private val running = AtomicBoolean(false)
    private lateinit var consumer: KafkaConsumer<K, V>

    // tracks offsets for successfully processed records, ready to commit
    private val uncommittedOffsets = mutableMapOf<TopicPartition, OffsetAndMetadata>()

    // tracks partitions that failed to process, to retry from a specific offset
    private val retryOffsets = mutableMapOf<TopicPartition, Long>()

    val status = ConsumerStatus()

    override fun start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Consumer for $topic already started")
            return
        }

        log.info("Starting Kafka consumer for topic $topic")

        consumer = KafkaConsumer<K, V>(config).also { kafkaConsumer ->
            kafkaConsumer.subscribe(listOf(topic), createRebalanceListener(kafkaConsumer))
            scope.launch {
                kafkaConsumer.use { runLoop(it) }
            }
        }
    }

    override suspend fun close() {
        if (!running.compareAndSet(true, false)) return

        log.info("Stopping Kafka consumer for topic $topic")

        consumer.wakeup()
        scope.coroutineContext[Job]?.cancelAndJoin()
        dispatcher.close()
    }

    override suspend fun consume(key: K, value: V) = this.consume.invoke(key, value)

    private suspend fun runLoop(consumer: KafkaConsumer<K, V>) {
        try {
            while (running.get()) pollOnce(consumer)
        } catch (_: WakeupException) {
            log.info("Consumer for $topic shutting down")
        } catch (t: Throwable) {
            log.error("Unexpected error in consumer loop for $topic", t)
        } finally {
            commitOffsets(consumer)
        }
    }

    private suspend fun pollOnce(consumer: KafkaConsumer<K, V>) {
        val delayMs = status.getDelayWhenAllPartitionsAreInRetry(consumer.assignment())
        if (delayMs != null) {
            log.debug("All assigned partitions are in retry, delaying $delayMs ms before next poll")
            delay(delayMs)
            return
        }

        if (retryOffsets.isNotEmpty()) retryFailedPartitions(consumer)

        val records = consumer.poll(Duration.ofMillis(pollTimeoutMs))
        if (records.isEmpty) return

        records.partitions().forEach { tp ->
            if (status.canProcessPartition(tp)) {
                processPartition(tp, records.records(tp))
            } else {
                log.debug("Consumer status for {} is failure, delaying {} ms before retrying", tp, status.backoffDuration(tp))
            }
        }

        commitOffsets(consumer)
    }

    private suspend fun processPartition(topicPartition: TopicPartition, records: List<ConsumerRecord<K, V>>) {
        for (record in records) {
            try {
                consume(record.key(), record.value())

                uncommittedOffsets[topicPartition] = OffsetAndMetadata(record.offset() + 1)
                retryOffsets.remove(topicPartition)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                log.warn("Failed processing $topicPartition offset=${record.offset()}", t)

                // set offset to retry from if not already set
                retryOffsets[topicPartition] = retryOffsets[topicPartition]
                    ?.coerceAtMost(record.offset())
                    ?: record.offset()

                status.incrementRetryCount(topicPartition)
                break // stop on first failure in partition
            }
        }

        if (topicPartition !in retryOffsets) {
            status.resetRetryCount(topicPartition)
        }
    }

    private fun retryFailedPartitions(consumer: KafkaConsumer<K, V>) {
        retryOffsets.forEach { (tp, retryOffset) ->
            try {
                val currentOffset = consumer.position(tp)
                if (currentOffset != retryOffset) {
                    consumer.seek(tp, retryOffset)
                    log.debug("Retrying {} from offset {} (was {})", tp, retryOffset, currentOffset)
                }
            } catch (e: IllegalStateException) {
                log.warn("Partition $tp not assigned during retry seek", e)
            }
        }
    }

    private fun commitOffsets(consumer: KafkaConsumer<K, V>) {
        if (uncommittedOffsets.isEmpty()) return

        try {
            consumer.commitSync(uncommittedOffsets)
            log.info("Offsets committed: $uncommittedOffsets")
            uncommittedOffsets.clear()
        } catch (e: Exception) {
            log.error("Commit failed for offsets $uncommittedOffsets", e)
        }
    }

    private fun createRebalanceListener(consumer: KafkaConsumer<K, V>) = object : ConsumerRebalanceListener {
        override fun onPartitionsRevoked(revokedPartitions: Collection<TopicPartition>) {
            log.info("Partitions revoked: $revokedPartitions")

            // collect offsets for revoked partitions that are pending commit
            val offsetsToCommitDuringRebalance = uncommittedOffsets
                .filterKeys { it in revokedPartitions }

            // try to commit offsets before losing ownership
            try {
                if (offsetsToCommitDuringRebalance.isNotEmpty()) {
                    consumer.commitSync(offsetsToCommitDuringRebalance)
                    log.info("Committed offsets before revoke: $offsetsToCommitDuringRebalance")
                }
            } catch (e: Exception) {
                // log but continue; the new owner will retry from last committed offsets
                log.error("Failed to commit offsets during partition revoke for $revokedPartitions: $offsetsToCommitDuringRebalance", e)
            } finally {
                // remove committed/uncommitted state for revoked partitions
                revokedPartitions.forEach { tp ->
                    uncommittedOffsets.remove(tp)
                    retryOffsets.remove(tp) // always remove, even if the commit failed
                    status.resetRetryCount(tp)
                }
            }
        }

        override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
            log.info("Partitions assigned: $partitions")
        }
    }
}
