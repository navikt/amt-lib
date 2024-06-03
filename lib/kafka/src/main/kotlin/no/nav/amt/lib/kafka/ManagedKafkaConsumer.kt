package no.nav.amt.lib.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val consume: suspend (key: K, value: V) -> Unit,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var running = false

    val status: ConsumerStatus = ConsumerStatus()

    fun run() = scope.launch {
        log.info("Started consumer for topic: $topic")
        running = true
        KafkaConsumer<K, V>(config).use { consumer ->
            consumer.subscribe(listOf(topic))
            while (running) {
                poll(consumer)
            }
        }
    }

    private suspend fun poll(consumer: KafkaConsumer<K, V>) {
        val offsetsToCommit = mutableMapOf<TopicPartition, OffsetAndMetadata>()

        if (status.isFailure) {
            delay(status.backoffDuration)
        }

        try {
            val records = consumer.poll(Duration.ofMillis(1000))
            seekToEarliestOffsets(records, consumer)

            records.forEach { record ->
                process(record)

                val partition = TopicPartition(record.topic(), record.partition())
                val offset = OffsetAndMetadata(record.offset() + 1)
                offsetsToCommit[partition] = offset
            }
            status.success()
        } catch (e: WakeupException) {
            // Consumeren skal avsluttes...
            stop()
        } catch (t: Throwable) {
            status.failure()
            log.error("Failed to process records for topic $topic", t)
        } finally {
            offsetsToCommit.forEach { (partition, offset) -> consumer.seek(partition, offset) }
            consumer.commitSync(offsetsToCommit)
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
            log.error(
                "Failed to consume record for " +
                    "topic=${record.topic()} " +
                    "key=${record.key()} " +
                    "partition=${record.partition()} " +
                    "offset=${record.offset()}",
                t,
            )
            throw t
        }
    }

    fun stop() {
        running = false
        job.cancel()
    }
}
