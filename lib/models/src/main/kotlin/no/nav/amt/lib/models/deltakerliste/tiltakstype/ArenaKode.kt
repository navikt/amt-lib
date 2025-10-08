package no.nav.amt.lib.models.deltakerliste.tiltakstype

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.AVKLARING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.HOYERE_UTDANNING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.JOBBKLUBB
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.OPPFOLGING
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET

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
    ENKELAMO,
    ENKFAGYRKE,
    HOYEREUTD;

    fun toTiltaksKode() = when (this) {
        ARBFORB -> ARBEIDSFORBEREDENDE_TRENING
        ARBRRHDAG -> ARBEIDSRETTET_REHABILITERING
        AVKLARAG -> AVKLARING
        DIGIOPPARB -> DIGITALT_OPPFOLGINGSTILTAK
        GRUPPEAMO -> GRUPPE_ARBEIDSMARKEDSOPPLAERING
        GRUFAGYRKE -> GRUPPE_FAG_OG_YRKESOPPLAERING
        JOBBK -> JOBBKLUBB
        INDOPPFAG -> OPPFOLGING
        VASV -> VARIG_TILRETTELAGT_ARBEID_SKJERMET
        ENKELAMO -> Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
        ENKFAGYRKE -> Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING
        HOYEREUTD -> HOYERE_UTDANNING
    }
}