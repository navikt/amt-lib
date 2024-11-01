package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import java.time.LocalDate

fun finnGyldigeDeltakelsesmengder(endringer: List<DeltakerHistorikk>): List<Deltakelsesmengde> {
    val deltakelsesmengder = endringer.mapNotNull {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toDeltakelsesmengde()
            is DeltakerHistorikk.EndringFraArrangor -> null
            is DeltakerHistorikk.Forslag -> null
            is DeltakerHistorikk.ImportertFraArena -> it.importertFraArena.toDeltakelsesmengde()
            is DeltakerHistorikk.Vedtak -> it.vedtak.toDeltakelsesmengde()
        }
    }

    return finnGyldigeDeltakelsesmengder(deltakelsesmengder.sortedByDescending { it.opprettet })
}

fun finnGyldigeDeltakelsesmengderInnenfor(
    endringer: List<DeltakerHistorikk>,
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
): List<Deltakelsesmengde> = finnDeltakelsesmengderInnenfor(
    deltakelsesmengder = finnGyldigeDeltakelsesmengder(endringer),
    fraOgMed = fraOgMed,
    tilOgMed = tilOgMed,
)

private fun finnDeltakelsesmengderInnenfor(
    deltakelsesmengder: List<Deltakelsesmengde>,
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
): List<Deltakelsesmengde> {
    val initialDeltakelsesmengde = deltakelsesmengder
        .filter { it.gyldigFra <= fraOgMed }
        .maxByOrNull { it.gyldigFra }

    val endringerIPerioden = deltakelsesmengder
        .filter { it != initialDeltakelsesmengde }
        .filter {
            (tilOgMed != null && it.gyldigFra in fraOgMed..tilOgMed) ||
                (tilOgMed == null && it.gyldigFra > fraOgMed)
        }

    return listOfNotNull(initialDeltakelsesmengde) + endringerIPerioden
}

private fun finnGyldigeDeltakelsesmengder(
    deltakelsesmengder: List<Deltakelsesmengde>,
    gyldigeDeltakelsesmengder: MutableList<Deltakelsesmengde> = mutableListOf(),
): List<Deltakelsesmengde> {
    if (deltakelsesmengder.isEmpty()) return gyldigeDeltakelsesmengder

    val forsteGyldigePeriode = deltakelsesmengder.minByOrNull { it.gyldigFra } ?: return gyldigeDeltakelsesmengder

    gyldigeDeltakelsesmengder.add(forsteGyldigePeriode)

    val nesteGyldigePerioder = deltakelsesmengder.subList(0, deltakelsesmengder.indexOf(forsteGyldigePeriode))

    return finnGyldigeDeltakelsesmengder(
        deltakelsesmengder = nesteGyldigePerioder,
        gyldigeDeltakelsesmengder = gyldigeDeltakelsesmengder,
    )
}
