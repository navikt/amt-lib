package no.nav.amt.lib.kafka

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

internal class ManagedConsumerRebalanceListener<K, V>(
    private val consumer: KafkaConsumer<K, V>,
    private val offsetManager: OffsetManager,
    private val backoffManager: PartitionBackoffManager,
) : ConsumerRebalanceListener {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onPartitionsRevoked(revokedPartitions: Collection<TopicPartition>) {
        log.info("Partitions revoked: $revokedPartitions")

        // collect offsets for revoked partitions that are pending commit
        val offsetsToCommitDuringRebalance = offsetManager
            .getOffsetsToCommit()
            .filterKeys { it in revokedPartitions }

        // try to commit offsets before losing ownership
        try {
            if (offsetsToCommitDuringRebalance.isNotEmpty()) {
                consumer.commitSync(offsetsToCommitDuringRebalance)
                log.info("Committing offsets before revoke: $offsetsToCommitDuringRebalance")
            }
        } catch (e: Exception) {
            // log but continue; the new owner will retry from last committed offsets
            log.error("Failed to commit offsets during partition revoke for $revokedPartitions: $offsetsToCommitDuringRebalance", e)
        } finally {
            // remove committed/uncommitted state for revoked partitions
            revokedPartitions.forEach { tp ->
                offsetManager.clearCommitted(tp)
                offsetManager.clearRetry(tp)
                backoffManager.resetRetryCount(tp)
            }
        }
    }

    override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
        log.info("Partitions assigned: $partitions")
    }
}
