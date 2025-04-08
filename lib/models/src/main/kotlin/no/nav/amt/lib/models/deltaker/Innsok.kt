package no.nav.amt.lib.models.deltaker

import java.time.LocalDateTime
import java.util.UUID

data class Innsok(
    val id: UUID,
    val deltakerId: UUID,
    val innsokt: LocalDateTime,
    val innsoktAv: UUID,
    val innsoktAvEnhet: UUID,
    val deltakelsesinnhold: Deltakelsesinnhold?,
    val utkastDelt: LocalDateTime?,
    val utkastGodkjentAvNav: Boolean,
)
