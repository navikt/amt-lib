package no.nav.amt.lib.models.tiltakskoordinator.response

import no.nav.amt.lib.models.deltaker.DeltakerStatus
import java.time.LocalDateTime
import java.util.UUID

data class EndringFraTiltakskoordinatorResponse(
    val id: UUID,
    val status: DeltakerStatus,
    val sistEndret: LocalDateTime,
)
