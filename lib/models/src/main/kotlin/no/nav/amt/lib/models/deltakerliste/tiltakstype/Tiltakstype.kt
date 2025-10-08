package no.nav.amt.lib.models.deltakerliste.tiltakstype

import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import java.util.UUID

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arenaKode: ArenaKode,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val innhold: DeltakerRegistreringInnhold?,
) {
    val visningsnavn get() = if (navn == "Jobbklubb") {
        "Jobbsøkerkurs"
    } else {
        navn
    }

    companion object {
        val kursTiltak = setOf(
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.JOBBKLUBB,
        )
        val enkeltplassTiltak = setOf(
            Tiltakskode.HOYERE_UTDANNING,
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING
        )
    }

    @Deprecated("Utrygg sjekk av kurstiltak. Må erstattes med å sjekke oppstartstype på tiltak")
    fun erKurs() = this.tiltakskode in kursTiltak

    fun erEnkeltplass() = this.tiltakskode in enkeltplassTiltak

    val harDeltakelsesmengde = tiltakskode in setOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET)
}
