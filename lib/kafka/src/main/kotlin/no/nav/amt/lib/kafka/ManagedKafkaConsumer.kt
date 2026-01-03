package no.nav.amt.lib.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import no.nav.amt.lib.kafka.KafkaPartitionUtils.updatePartitionPauseState
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ManagedKafkaConsumer<K, V>(
    private val topic: String,
    private val config: Map<String, Any>,
    private val pollTimeoutMs: Long = 1000L,
    private val consume: suspend (key: K, value: V) -> Unit,
) : Consumer<K, V> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val running = AtomicBoolean(false)
    private lateinit var consumer: KafkaConsumer<K, V>

    private val partitionBackoffManager = PartitionBackoffManager()
    private val offsetManager = OffsetManager()
    private val partitionProcessor = PartitionProcessor(consume, partitionBackoffManager, offsetManager)

    // single-threaded KafkaConsumer dispatcher
    private val dispatcher = Executors
        .newSingleThreadExecutor { r -> Thread(r, "kafka-consumer-$topic") }
        .asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)

    override fun start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Consumer for $topic already started")
            return
        }

        log.info("Starting Kafka consumer for topic $topic")

        consumer = KafkaConsumer<K, V>(config).also { kafkaConsumer ->
            kafkaConsumer.subscribe(
                listOf(topic),
                ManagedConsumerRebalanceListener(
                    consumer = kafkaConsumer,
                    offsetManager = offsetManager,
                    backoffManager = partitionBackoffManager,
                ),
            )

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
            offsetManager.commit(consumer)
        }
    }

    private suspend fun pollOnce(consumer: KafkaConsumer<K, V>) {
        offsetManager.retryFailedPartitions(consumer)
        updatePartitionPauseState(consumer, partitionBackoffManager)

        val records = consumer.poll(Duration.ofMillis(pollTimeoutMs))
        if (records.isEmpty) return

        records.partitions().forEach { topicPartition ->
            partitionProcessor.process(
                topicPartition = topicPartition,
                records = records.records(topicPartition),
            )
        }

        offsetManager.commit(consumer)
    }
}
