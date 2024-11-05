package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import java.time.LocalDate

class Deltakelsesmengder(
    mengder: List<Deltakelsesmengde>,
) : List<Deltakelsesmengde> {
    private val deltakelsesmengder = finnGyldigeDeltakelsesmengder(mengder.sortedByDescending { it.opprettet })

    val gjeldende = deltakelsesmengder.lastOrNull { it.gyldigFra <= LocalDate.now() }

    fun periode(fraOgMed: LocalDate, tilOgMed: LocalDate?): List<Deltakelsesmengde> {
        val initialDeltakelsesmengde = deltakelsesmengder
            .filter { it.gyldigFra <= fraOgMed }
            .maxByOrNull { it.gyldigFra }

        val endringerIPerioden = deltakelsesmengder
            .filter {
                val mengdeErIPerioden = if (tilOgMed == null) {
                    it.gyldigFra > fraOgMed
                } else {
                    it.gyldigFra in fraOgMed..tilOgMed
                }

                it != initialDeltakelsesmengde && mengdeErIPerioden
            }

        return listOfNotNull(initialDeltakelsesmengde) + endringerIPerioden
    }

    fun validerNyDeltakelsesmengde(deltakelsesmengde: Deltakelsesmengde): Boolean {
        val nyeDeltakelsesmengder = Deltakelsesmengder(deltakelsesmengder + deltakelsesmengde)
        return nyeDeltakelsesmengder != this
    }

    private fun finnGyldigeDeltakelsesmengder(
        deltakelsesmengder: List<Deltakelsesmengde>,
        gyldigeDeltakelsesmengder: MutableList<Deltakelsesmengde> = mutableListOf(),
    ): List<Deltakelsesmengde> {
        if (deltakelsesmengder.isEmpty()) return gyldigeDeltakelsesmengder

        val forsteGyldigePeriode = deltakelsesmengder.minByOrNull { it.gyldigFra } ?: return gyldigeDeltakelsesmengder

        val nesteGyldigePerioder = deltakelsesmengder.subList(0, deltakelsesmengder.indexOf(forsteGyldigePeriode))

        return finnGyldigeDeltakelsesmengder(
            deltakelsesmengder = nesteGyldigePerioder,
            gyldigeDeltakelsesmengder = mergePerioder(forsteGyldigePeriode, gyldigeDeltakelsesmengder),
        )
    }

    private fun mergePerioder(
        periode: Deltakelsesmengde,
        deltakelsesmengder: MutableList<Deltakelsesmengde>,
    ): MutableList<Deltakelsesmengde> {
        val forrige = deltakelsesmengder.lastOrNull()

        if (forrige != null && forrige.deltakelsesprosent == periode.deltakelsesprosent && forrige.dagerPerUke == periode.dagerPerUke) {
            return deltakelsesmengder
        }
        deltakelsesmengder.add(periode)

        return deltakelsesmengder
    }

    override fun equals(other: Any?): Boolean = if (other != null && other is Deltakelsesmengder) {
        this.deltakelsesmengder == other.deltakelsesmengder
    } else {
        false
    }

    override fun hashCode(): Int = this.deltakelsesmengder.hashCode()

    override val size: Int = deltakelsesmengder.size

    override fun contains(element: Deltakelsesmengde) = deltakelsesmengder.contains(element)

    override fun containsAll(elements: Collection<Deltakelsesmengde>) = deltakelsesmengder.containsAll(elements)

    override fun get(index: Int) = deltakelsesmengder[index]

    override fun isEmpty() = deltakelsesmengder.isEmpty()

    override fun iterator() = deltakelsesmengder.iterator()

    override fun indexOf(element: Deltakelsesmengde) = deltakelsesmengder.indexOf(element)

    override fun listIterator(): ListIterator<Deltakelsesmengde> = deltakelsesmengder.listIterator()

    override fun listIterator(index: Int): ListIterator<Deltakelsesmengde> = deltakelsesmengder.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = deltakelsesmengder.subList(fromIndex, toIndex)

    override fun lastIndexOf(element: Deltakelsesmengde) = deltakelsesmengder.lastIndexOf(element)
}

fun List<DeltakerHistorikk>.toDeltakelsesmengder() = Deltakelsesmengder(
    this.mapNotNull {
        when (it) {
            is DeltakerHistorikk.Endring -> it.endring.toDeltakelsesmengde()
            is DeltakerHistorikk.EndringFraArrangor -> null
            is DeltakerHistorikk.Forslag -> null
            is DeltakerHistorikk.ImportertFraArena -> it.importertFraArena.toDeltakelsesmengde()
            is DeltakerHistorikk.Vedtak -> it.vedtak.toDeltakelsesmengde()
        }
    },
)
