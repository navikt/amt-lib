package no.nav.amt.lib.models.arrangor.melding

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Forslag(
    val id: UUID,
    val deltakerId: UUID,
    val opprettetAvArrangorAnsattId: UUID,
    val opprettet: LocalDateTime,
    val begrunnelse: String,
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

        data object VenterPaSvar : Status
    }

    val sistEndret get(): LocalDateTime {
        return when (status) {
            is Status.VenterPaSvar -> opprettet
            is Status.Avvist -> status.avvist
            is Status.Godkjent -> status.godkjent
            is Status.Tilbakekalt -> status.tilbakekalt
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed interface Endring

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
    ) : Endring

    data class NavAnsatt(
        val id: UUID,
        val enhetId: UUID,
    )
}
