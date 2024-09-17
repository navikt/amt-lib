package no.nav.amt.lib.models.deltaker

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ImportertFraArena (
    val deltakerId: UUID,
    val importertDato: LocalDateTime,
    val deltakerVedImport: DeltakerVedImport,
)

data class DeltakerVedImport(
    val deltakerId: UUID,
    val innsoktDato: LocalDate,
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val status: DeltakerStatus,
)
