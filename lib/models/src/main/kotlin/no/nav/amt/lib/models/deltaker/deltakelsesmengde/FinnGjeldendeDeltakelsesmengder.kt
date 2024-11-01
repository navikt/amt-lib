package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import java.time.LocalDate

fun finnGjeldendeDeltakelsesmengder(endringer: List<DeltakerHistorikk>): List<Deltakelsesmengde> {
    val deltakelsesmengder = endringer.mapNotNull {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toDeltakelsesmengde()
            is DeltakerHistorikk.EndringFraArrangor -> null
            is DeltakerHistorikk.Forslag -> null
            is DeltakerHistorikk.ImportertFraArena -> it.importertFraArena.toDeltakelsesmengde()
            is DeltakerHistorikk.Vedtak -> it.vedtak.toDeltakelsesmengde()
        }
    }

    return finnGjeldendeDeltakelsesmengder(deltakelsesmengder.sortedByDescending { it.opprettet })
}

fun finnGjeldendeDeltakelsesmengderInnenfor(
    endringer: List<DeltakerHistorikk>,
    fraOgMed: LocalDate,
    tilOgMed: LocalDate?,
): List<Deltakelsesmengde> = finnDeltakelsesmengderInnenfor(
    deltakelsesmengder = finnGjeldendeDeltakelsesmengder(endringer),
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

private fun finnGjeldendeDeltakelsesmengder(
    deltakelsesmengder: List<Deltakelsesmengde>,
    gjeldendeDeltakelsesmengder: MutableList<Deltakelsesmengde> = mutableListOf(),
): List<Deltakelsesmengde> {
    if (deltakelsesmengder.isEmpty()) return gjeldendeDeltakelsesmengder

    val forsteGyldigePeriode = deltakelsesmengder.minByOrNull { it.gyldigFra } ?: return gjeldendeDeltakelsesmengder

    gjeldendeDeltakelsesmengder.add(forsteGyldigePeriode)

    val nesteGjeldendePerioder = deltakelsesmengder.subList(0, deltakelsesmengder.indexOf(forsteGyldigePeriode))

    return finnGjeldendeDeltakelsesmengder(
        deltakelsesmengder = nesteGjeldendePerioder,
        gjeldendeDeltakelsesmengder = gjeldendeDeltakelsesmengder,
    )
}
