package no.nav.amt.lib.models.deltakerliste.kafka

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GjennomforingV2KafkaPayloadDeserializingTest {

    private val deserializer = GjennomforingV2KafkaPayloadDeserializer()

    @Test
    fun `deserializes Gruppe payload`() {
        val id = UUID.randomUUID()
        val opprettet = Instant.parse("2024-01-02T03:04:05Z")
        val oppdatert = Instant.parse("2024-05-06T07:08:09Z")
        val startDato = LocalDate.parse("2024-02-01")
        val sluttDato = LocalDate.parse("2024-03-01")

        val json = """
            {
              "type": "Gruppe",
              "id": "${id}",
              "opprettetTidspunkt": "${opprettet}",
              "oppdatertTidspunkt": "${oppdatert}",
              "tiltakskode": "ARBEIDSFORBEREDENDE_TRENING",
              "arrangor": { "organisasjonsnummer": "123456789" },
              "navn": "Gruppe 1",
              "startDato": "${startDato}",
              "sluttDato": "${sluttDato}",
              "status": "GJENNOMFORES",
              "oppstart": "LOPENDE",
              "tilgjengeligForArrangorFraOgMedDato": null,
              "apentForPamelding": true,
              "antallPlasser": 25,
              "deltidsprosent": 50.0,
              "oppmoteSted": "Oslo"
            }
        """.trimIndent()

        val payload = deserializer.deserialize("topic", json.toByteArray())

        payload.shouldBeInstanceOf<GjennomforingV2KafkaPayload.Gruppe>()
        payload.id shouldBe id
        payload.opprettetTidspunkt shouldBe opprettet
        payload.oppdatertTidspunkt shouldBe oppdatert
        payload.tiltakskode shouldBe Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
        payload.arrangor.organisasjonsnummer shouldBe "123456789"
        payload.navn shouldBe "Gruppe 1"
        payload.startDato shouldBe startDato
        payload.sluttDato shouldBe sluttDato
        payload.status shouldBe GjennomforingV2KafkaPayload.GjennomforingStatusType.GJENNOMFORES
        payload.oppstart shouldBe GjennomforingV2KafkaPayload.GjennomforingOppstartstype.LOPENDE
        payload.tilgjengeligForArrangorFraOgMedDato shouldBe null
        payload.apentForPamelding shouldBe true
        payload.antallPlasser shouldBe 25
        payload.deltidsprosent shouldBe 50.0
        payload.oppmoteSted shouldBe "Oslo"
        payload.type shouldBe GjennomforingV2KafkaPayload.Type.Gruppe
    }

    @Test
    fun `deserializes Enkeltplass payload`() {
        val id = UUID.randomUUID()
        val opprettet = Instant.parse("2023-11-10T09:08:07Z")
        val oppdatert = Instant.parse("2024-01-12T11:10:09Z")

        val json = """
            {
              "type": "Enkeltplass",
              "id": "${id}",
              "opprettetTidspunkt": "${opprettet}",
              "oppdatertTidspunkt": "${oppdatert}",
              "tiltakskode": "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
              "arrangor": { "organisasjonsnummer": "987654321" }
            }
        """.trimIndent()

        val payload = deserializer.deserialize("topic", json.toByteArray())

        payload.shouldBeInstanceOf<GjennomforingV2KafkaPayload.Enkeltplass>()
        payload.id shouldBe id
        payload.opprettetTidspunkt shouldBe opprettet
        payload.oppdatertTidspunkt shouldBe oppdatert
        payload.tiltakskode shouldBe Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
        payload.arrangor.organisasjonsnummer shouldBe "987654321"
        payload.type shouldBe GjennomforingV2KafkaPayload.Type.Enkeltplass
    }
}