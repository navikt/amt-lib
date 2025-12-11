package no.nav.amt.lib.models.deltakerliste.kafka

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GjennomforingV2KafkaPayload.Gruppe::class, name = "Gruppe"),
    JsonSubTypes.Type(value = GjennomforingV2KafkaPayload.Enkeltplass::class, name = "Enkeltplass"),
)
sealed class GjennomforingV2KafkaPayload {
    abstract val id: UUID
    abstract val type: Type
    abstract val opprettetTidspunkt: Instant
    abstract val oppdatertTidspunkt: Instant
    abstract val tiltakskode: Tiltakskode
    abstract val arrangor: Arrangor

    enum class Type {
        Gruppe,
        Enkeltplass,
    }

    data class Arrangor(
        val organisasjonsnummer: String,
    )

    data class Gruppe(
        override val id: UUID,
        override val opprettetTidspunkt: Instant,
        override val oppdatertTidspunkt: Instant,
        override val tiltakskode: Tiltakskode,
        override val arrangor: Arrangor,
        val navn: String,
        val startDato: LocalDate,
        val sluttDato: LocalDate?,
        val status: GjennomforingStatusType,
        val oppstart: GjennomforingOppstartstype,
        val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
        val apentForPamelding: Boolean,
        val antallPlasser: Int,
        val deltidsprosent: Double,
        val oppmoteSted: String?,
        override val type: Type = Type.Gruppe,
    ) : GjennomforingV2KafkaPayload()

    data class Enkeltplass(
        override val id: UUID,
        override val opprettetTidspunkt: Instant,
        override val oppdatertTidspunkt: Instant,
        override val tiltakskode: Tiltakskode,
        override val arrangor: Arrangor,
        override val type: Type = Type.Enkeltplass,
    ) : GjennomforingV2KafkaPayload()

    enum class GjennomforingStatusType(
        val beskrivelse: String,
    ) {
        GJENNOMFORES("Gjennomføres"),
        AVSLUTTET("Avsluttet"),
        AVBRUTT("Avbrutt"),
        AVLYST("Avlyst"),
    }

    enum class GjennomforingOppstartstype {
        LOPENDE,
        FELLES,
    }
}
