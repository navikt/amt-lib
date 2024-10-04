package no.nav.amt.lib.models.arrangor.melding

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Forslag(
    override val id: UUID,
    override val deltakerId: UUID,
    override val opprettetAvArrangorAnsattId: UUID,
    override val opprettet: LocalDateTime,
    val begrunnelse: String?,
    val endring: Endring,
    val status: Status,
) : Melding {
    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed interface Status {
        data class Godkjent(
            val godkjentAv: NavAnsatt,
            val godkjent: LocalDateTime,
        ) : Status

        data class Avvist(
            val avvistAv: NavAnsatt,
            val avvist: LocalDateTime,
            val begrunnelseFraNav: String,
        ) : Status

        data class Tilbakekalt(
            val tilbakekaltAvArrangorAnsattId: UUID,
            val tilbakekalt: LocalDateTime,
        ) : Status

        data class Erstattet(
            val erstattetMedForslagId: UUID,
            val erstattet: LocalDateTime,
        ) : Status

        data object VenterPaSvar : Status
    }

    val sistEndret get(): LocalDateTime {
        return when (status) {
            is Status.VenterPaSvar -> opprettet
            is Status.Avvist -> status.avvist
            is Status.Godkjent -> status.godkjent
            is Status.Tilbakekalt -> status.tilbakekalt
            is Status.Erstattet -> status.erstattet
        }
    }

    fun getNavAnsatt() = when (val status = this.status) {
        is Status.Avvist -> status.avvistAv
        is Status.Godkjent -> status.godkjentAv
        is Status.Erstattet,
        is Status.Tilbakekalt,
        Status.VenterPaSvar,
        -> null
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed interface Endring

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
    ) : Endring

    data class AvsluttDeltakelse(
        val sluttdato: LocalDate?,
        val aarsak: EndringAarsak,
        val harDeltatt: Boolean?,
    ) : Endring

    data class IkkeAktuell(
        val aarsak: EndringAarsak,
    ) : Endring

    data class Deltakelsesmengde(
        val deltakelsesprosent: Int,
        val dagerPerUke: Int?,
    ) : Endring

    data class Startdato(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
    ) : Endring

    data class Sluttdato(
        val sluttdato: LocalDate,
    ) : Endring

    data class Sluttarsak(
        val aarsak: EndringAarsak,
    ) : Endring

    data class NavAnsatt(
        val id: UUID,
        val enhetId: UUID,
    )
}
