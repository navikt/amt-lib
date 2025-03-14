package no.nav.amt.lib.models.tiltakskoordinator

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDateTime
import java.util.UUID

data class EndringFraTiltakskoordinator(
    val id: UUID,
    val deltakerId: UUID,
    val endring: Endring,
    val endretAv: UUID,
    val endret: LocalDateTime,
) {
    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    sealed interface Endring

    data object DelMedArrangor : Endring
}
