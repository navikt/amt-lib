package no.nav.amt.lib.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration

class ManagedKafkaConsumer<K, V>(
    private val topic: String,
    private val config: Map<String, *>,
    private val consume: suspend (key: K, value: V) -> Unit = { _, _ -> },
) : Consumer<K, V> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val offsetsToCommit = mutableMapOf<TopicPartition, OffsetAndMetadata>()

    private var running = false

    val status: ConsumerStatus = ConsumerStatus()
    private lateinit var runState: Deferred<Unit>

    override fun start() {
        log.info("Starting consumer for topic: $topic")
        runState = run()
    }

    override suspend fun consume(key: K, value: V) {
        this.consume.invoke(key, value)
    }

    fun stop() {
        running = false
    }

    override suspend fun close() {
        log.info("Closing consumer for topic: $topic.. ..")
        stop()
        return runState.await().also {
            log.info("Closed consumer for topic: $topic")
        }
    }

    private fun run() = scope.async {
        running = true

        KafkaConsumer<K, V>(config).use { consumer ->
            subscribe(consumer)
        }
    }

    private suspend fun subscribe(consumer: KafkaConsumer<K, V>) {
        try {
            consumer.subscribe(listOf(topic), rebalanceListener(consumer))

            while (running) {
                poll(consumer)
            }
        } catch (_: WakeupException) {
            log.info("Consumer for $topic is exiting")
            stop()
        } catch (t: Throwable) {
            log.error("Something went wrong with consumer for topic $topic", t)
            throw t
        }
    }

    private suspend fun poll(consumer: KafkaConsumer<K, V>) {
        if (status.isFailure) {
            log.info(
                "Consumer status for topic $topic is failure, " +
                        "delaying ${status.backoffDuration}ms before retrying",
            )
            delay(status.backoffDuration)
        }

        try {
            val records = consumer.poll(Duration.ofMillis(1000))

            seekToEarliestOffsets(records, consumer)

            if (!records.isEmpty) {
                log.info("Consumer for $topic polled ${records.count()} records.")
            }

            records.forEach { record ->
                process(record)

                val partition = TopicPartition(record.topic(), record.partition())
                val offset = OffsetAndMetadata(record.offset() + 1)

                offsetsToCommit[partition] = offset
                status.success()
            }
        } catch (t: Throwable) {
            log.error(t.message, t)
            status.failure()
        } finally {
            commitOffsets(consumer)
        }
    }

    private fun commitOffsets(consumer: KafkaConsumer<K, V>) {
        if (offsetsToCommit.isNotEmpty()) {
            offsetsToCommit.forEach { (partition, offset) -> consumer.seek(partition, offset) }
            consumer.commitSync(offsetsToCommit)

            log.info("Committed offsets $offsetsToCommit")
            offsetsToCommit.clear()
        }
    }

    private fun seekToEarliestOffsets(records: ConsumerRecords<K, V>, consumer: KafkaConsumer<K, V>) {
        val offsetMap = mutableMapOf<TopicPartition, OffsetAndMetadata>()

        records.forEach { record ->
            val topicPartition = TopicPartition(record.topic(), record.partition())
            val offsetAndMetadata = OffsetAndMetadata(record.offset())

            val storedOffset = offsetMap[topicPartition]

            if (storedOffset == null || offsetAndMetadata.offset() < storedOffset.offset()) {
                offsetMap[topicPartition] = offsetAndMetadata
            }
        }

        offsetMap.forEach { consumer.seek(it.key, it.value) }
    }

    private suspend fun process(record: ConsumerRecord<K, V>) {
        try {
            consume(record.key(), record.value())
            log.info(
                "Consumed record for " +
                        "topic=${record.topic()} " +
                        "key=${record.key()} " +
                        "partition=${record.partition()} " +
                        "offset=${record.offset()}",
            )
        } catch (t: Throwable) {
            val msg = "Failed to consume record for " +
                    "topic=${record.topic()} " +
                    "key=${record.key()} " +
                    "partition=${record.partition()} " +
                    "offset=${record.offset()}"
            throw ConsumerProcessingException(msg, t)
        }
    }

    class ConsumerProcessingException(
        msg: String,
        cause: Throwable?,
    ) : RuntimeException(msg, cause)

    private fun rebalanceListener(consumer: KafkaConsumer<K, V>) = object : ConsumerRebalanceListener {
        override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>) {
            log.info("Partitions revoked $partitions, committing offsets")
            commitOffsets(consumer)
        }

        override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>) {
            log.info("Partitions assigned $partitions")
        }
    }
}
