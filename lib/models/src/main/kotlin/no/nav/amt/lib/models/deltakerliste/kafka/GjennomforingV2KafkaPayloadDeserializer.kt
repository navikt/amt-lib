package no.nav.amt.lib.models.deltakerliste.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.common.serialization.Deserializer

class GjennomforingV2KafkaPayloadDeserializer : Deserializer<GjennomforingV2KafkaPayload> {
    private val mapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

    override fun deserialize(topic: String?, data: ByteArray?): GjennomforingV2KafkaPayload? {
        if (data == null) return null

        val node: JsonNode = mapper.readTree(data)
        val typeText = node.get("type")?.asText() ?: return null
        val type = GjennomforingV2KafkaPayload.Type.valueOf(typeText)

        return when (type) {
            GjennomforingV2KafkaPayload.Type.Gruppe ->
                mapper.treeToValue(node, GjennomforingV2KafkaPayload.Gruppe::class.java)
            GjennomforingV2KafkaPayload.Type.Enkeltplass ->
                mapper.treeToValue(node, GjennomforingV2KafkaPayload.Enkeltplass::class.java)
        }
    }
}