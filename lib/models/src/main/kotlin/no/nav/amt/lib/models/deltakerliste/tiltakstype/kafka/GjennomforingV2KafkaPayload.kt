package no.nav.amt.lib.models.deltakerliste.tiltakstype.kafka

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

sealed class GjennomforingV2KafkaPayload {
    abstract val id: UUID
    abstract val type: GjennomforingType
    abstract val opprettetTidspunkt: Instant
    abstract val oppdatertTidspunkt: Instant
    abstract val tiltakskode: Tiltakskode
    abstract val arrangor: Arrangor

    enum class GjennomforingType {
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
        override val type: GjennomforingType = GjennomforingType.Gruppe,
    ) : GjennomforingV2KafkaPayload()

    data class Enkeltplass(
        override val id: UUID,
        override val opprettetTidspunkt: Instant,
        override val oppdatertTidspunkt: Instant,
        override val tiltakskode: Tiltakskode,
        override val arrangor: Arrangor,
        override val type: GjennomforingType = GjennomforingType.Enkeltplass,
    ) : GjennomforingV2KafkaPayload()

    enum class GjennomforingStatusType(val beskrivelse: String) {
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