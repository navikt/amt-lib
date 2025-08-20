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

    enum class ArenaKode {
        ARBFORB,
        ARBRRHDAG,
        AVKLARAG,
        DIGIOPPARB,
        INDOPPFAG,
        GRUFAGYRKE,
        GRUPPEAMO,
        JOBBK,
        VASV,
    }

    enum class Tiltakskode {
        ARBEIDSFORBEREDENDE_TRENING,
        ARBEIDSRETTET_REHABILITERING,
        AVKLARING,
        DIGITALT_OPPFOLGINGSTILTAK,
        GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        GRUPPE_FAG_OG_YRKESOPPLAERING,
        JOBBKLUBB,
        OPPFOLGING,
        VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        ;

        @Deprecated("Utrygg sjekk av kurstiltak. Må erstattes med å sjekke oppstartstype på tiltak")
        fun erKurs() = this in kursTiltak

        fun toArenaKode() = when (this) {
            ARBEIDSFORBEREDENDE_TRENING -> ArenaKode.ARBFORB
            ARBEIDSRETTET_REHABILITERING -> ArenaKode.ARBRRHDAG
            AVKLARING -> ArenaKode.AVKLARAG
            DIGITALT_OPPFOLGINGSTILTAK -> ArenaKode.DIGIOPPARB
            GRUPPE_ARBEIDSMARKEDSOPPLAERING -> ArenaKode.GRUPPEAMO
            GRUPPE_FAG_OG_YRKESOPPLAERING -> ArenaKode.GRUFAGYRKE
            JOBBKLUBB -> ArenaKode.JOBBK
            OPPFOLGING -> ArenaKode.INDOPPFAG
            VARIG_TILRETTELAGT_ARBEID_SKJERMET -> ArenaKode.VASV
        }
    }

    companion object {
        val kursTiltak = setOf(
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.JOBBKLUBB,
        )
    }

    @Deprecated("Utrygg sjekk av kurstiltak. Må erstattes med å sjekke oppstartstype på tiltak")
    fun erKurs() = this.tiltakskode.erKurs()

    val harDeltakelsesmengde = tiltakskode in setOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET)
}
