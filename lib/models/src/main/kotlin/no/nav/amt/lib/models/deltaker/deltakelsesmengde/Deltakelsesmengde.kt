package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.ImportertFraArena
import no.nav.amt.lib.models.deltaker.Vedtak
import java.time.LocalDate
import java.time.LocalDateTime

data class Deltakelsesmengde(
    val deltakelsesprosent: Float,
    val dagerPerUke: Float?,
    val gyldigFra: LocalDate,
    val opprettet: LocalDateTime,
)

fun DeltakerEndring.toDeltakelsesmengde(): Deltakelsesmengde? {
    val endring = this.endring
    if (endring is DeltakerEndring.Endring.EndreDeltakelsesmengde) {
        return Deltakelsesmengde(
            deltakelsesprosent = endring.deltakelsesprosent ?: 100F,
            dagerPerUke = endring.dagerPerUke,
            gyldigFra = endring.gyldigFra ?: this.endret.toLocalDate(),
            opprettet = this.endret,
        )
    }
    return null
}

fun Vedtak.toDeltakelsesmengde() = Deltakelsesmengde(
    deltakelsesprosent = this.deltakerVedVedtak.deltakelsesprosent ?: 100F,
    dagerPerUke = this.deltakerVedVedtak.dagerPerUke,
    gyldigFra = this.fattet?.toLocalDate() ?: this.opprettet.toLocalDate(),
    opprettet = this.fattet ?: this.opprettet,
)

fun ImportertFraArena.toDeltakelsesmengde() = Deltakelsesmengde(
    deltakelsesprosent = this.deltakerVedImport.deltakelsesprosent ?: 100F,
    dagerPerUke = this.deltakerVedImport.dagerPerUke,
    gyldigFra = this.deltakerVedImport.innsoktDato,
    opprettet = this.deltakerVedImport.innsoktDato.atStartOfDay(),
)
