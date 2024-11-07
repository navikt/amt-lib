package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import java.time.LocalDate

/**
 * Deltakelsesmengder er en liste av alle gyldige deltakelsesmengder, både frem og tilbake i tid, den er sortert på gyldig-fra stigende.
 *
 * Gitt en liste med flere overlappende endringen via konstruktøren eller `List<DeltakerHistorikk>.toDeltakelsesmengder()`
 * produserer denne en sortert liste hvor de endringene som har blitt invalidert av andre endringer er filtrert vekk.
 */
class Deltakelsesmengder(
    mengder: List<Deltakelsesmengde>,
) : List<Deltakelsesmengde> {
    private val deltakelsesmengder = finnGyldigeDeltakelsesmengder(sorterMengder(mengder))

    val gjeldende = deltakelsesmengder.lastOrNull { it.gyldigFra <= LocalDate.now() }

    val nesteGjeldende: Deltakelsesmengde?
        get() {
            val nesteGjeldendeIndex = deltakelsesmengder.indexOf(gjeldende) + 1

            if (deltakelsesmengder.size > nesteGjeldendeIndex) {
                return deltakelsesmengder[nesteGjeldendeIndex]
            }

            return null
        }

    /**
     * Finner hvilke deltakelsesmengder som var gjeldende for perioden f.o.m. t.o.m.
     */
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

    /**
     * Validerer om ny deltakelsesmengde fører til en endring av gjeldende deltakelsesmengder for hele deltakelsen eller ikke.
     */
    fun validerNyDeltakelsesmengde(deltakelsesmengde: Deltakelsesmengde): Boolean {
        val siste = deltakelsesmengder.lastOrNull() ?: return true

        return if (siste.dagerPerUke != deltakelsesmengde.dagerPerUke || siste.deltakelsesprosent != deltakelsesmengde.deltakelsesprosent) {
            true
        } else {
            deltakelsesmengde.gyldigFra < siste.gyldigFra
        }
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

    /**
     * Hvis deltakelsesmengden er lik forrige, men har en senere gyldig-fra så trenger vi ikke å beholde den.
     *
     * Dette burde ikke være nødvendig om man validerer deltakelsesmengdene riktig.
     */
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

    /**
     * Man må sorterer på opprettet her for at `finnGyldigeDeltakelsesmengde` skal gi riktig svar.
     */
    private fun sorterMengder(mengder: List<Deltakelsesmengde>): List<Deltakelsesmengde> = mengder.sortedWith(
        compareByDescending<Deltakelsesmengde> { it.opprettet }
            .thenByDescending { it.gyldigFra },
    )

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