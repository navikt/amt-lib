package no.nav.amt.lib.kafka

import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.testing.SingletonKafkaProvider
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.UUID

object KafkaTestUtils {
    const val TOPIC_IN_TEST = "test.topic"

    val stringConsumerConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost()).consumerConfig(
        keyDeserializer = StringDeserializer(),
        valueDeserializer = StringDeserializer(),
        groupId = "test-consumer-${UUID.randomUUID()}",
    )

    val intConsumerConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost()).consumerConfig(
        keyDeserializer = IntegerDeserializer(),
        valueDeserializer = IntegerDeserializer(),
        groupId = "test-consumer-${UUID.randomUUID()}",
    )
}
