package no.nav.amt.lib.models.arrangor.melding

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime
import java.util.UUID

data class EndringFraArrangor(
    val id: UUID,
    val deltakerId: UUID,
    val opprettetAvArrangorAnsattId: UUID,
    val opprettet: LocalDateTime,
    val endring: Endring,
) : Melding {
    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed interface Endring

    data class LeggTilOppstartsdato(
        val startdato: LocalDateTime,
        val sluttdato: LocalDateTime?,
    ) : Endring
}
