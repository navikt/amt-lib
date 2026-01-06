package no.nav.amt.lib.models.deltakerliste.kafka

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.deltakerliste.GjennomforingPameldingType
import no.nav.amt.lib.models.deltakerliste.GjennomforingStatusType
import no.nav.amt.lib.models.deltakerliste.GjennomforingType
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.kafka.GjennomforingV2KafkaPayload.Companion.ENKELTPLASS_V2_TYPE
import no.nav.amt.lib.models.deltakerliste.kafka.GjennomforingV2KafkaPayload.Companion.GRUPPE_V2_TYPE
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GjennomforingV2KafkaPayload.Gruppe::class, name = GRUPPE_V2_TYPE),
    JsonSubTypes.Type(value = GjennomforingV2KafkaPayload.Enkeltplass::class, name = ENKELTPLASS_V2_TYPE),
)
sealed class GjennomforingV2KafkaPayload {
    abstract val id: UUID
    abstract val opprettetTidspunkt: OffsetDateTime
    abstract val oppdatertTidspunkt: OffsetDateTime
    abstract val tiltakskode: Tiltakskode
    abstract val arrangor: Arrangor
    abstract val pameldingType: GjennomforingPameldingType?

    @get:JsonIgnore
    abstract val gjennomforingType: GjennomforingType

    data class Arrangor(
        val organisasjonsnummer: String,
    )

    data class Gruppe(
        override val id: UUID,
        override val opprettetTidspunkt: OffsetDateTime,
        override val oppdatertTidspunkt: OffsetDateTime,
        override val tiltakskode: Tiltakskode,
        override val arrangor: Arrangor,
        override val pameldingType: GjennomforingPameldingType? = null,
        val navn: String,
        val startDato: LocalDate,
        val sluttDato: LocalDate?,
        val status: GjennomforingStatusType,
        val oppstart: Oppstartstype,
        val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
        val apentForPamelding: Boolean,
        val antallPlasser: Int,
        val deltidsprosent: Double,
        val oppmoteSted: String?,
        override val gjennomforingType: GjennomforingType = GjennomforingType.Gruppe,
    ) : GjennomforingV2KafkaPayload()

    data class Enkeltplass(
        override val id: UUID,
        override val opprettetTidspunkt: OffsetDateTime,
        override val oppdatertTidspunkt: OffsetDateTime,
        override val tiltakskode: Tiltakskode,
        override val arrangor: Arrangor,
        override val pameldingType: GjennomforingPameldingType? = null,
        override val gjennomforingType: GjennomforingType = GjennomforingType.Enkeltplass,
    ) : GjennomforingV2KafkaPayload()

    fun <T : Any> toModel(gruppeMapper: (Gruppe) -> T, enkeltplassMapper: (Enkeltplass) -> T): T = when (this) {
        is Gruppe -> gruppeMapper(this)
        is Enkeltplass -> enkeltplassMapper(this)
    }

    companion object {
        const val GRUPPE_V2_TYPE = "TiltaksgjennomforingV2.Gruppe"
        const val ENKELTPLASS_V2_TYPE = "TiltaksgjennomforingV2.Enkeltplass"
    }
}
