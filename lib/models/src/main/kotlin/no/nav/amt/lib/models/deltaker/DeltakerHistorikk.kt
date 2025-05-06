package no.nav.amt.lib.models.deltaker

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DeltakerHistorikk {
    val sistEndret
        get() = when (this) {
            is Endring -> endring.endret
            is Vedtak -> vedtak.sistEndret
            is InnsokPaaFellesOppstart -> data.innsokt
            is Forslag -> forslag.sistEndret
            is EndringFraArrangor -> endringFraArrangor.opprettet
            is ImportertFraArena -> importertFraArena.importertDato
            is VurderingFraArrangor -> data.opprettet
            is EndringFraTiltakskoordinator -> endringFraTiltakskoordinator.endret
        }

    data class VurderingFraArrangor(
        val data: VurderingFraArrangorData,
    ) : DeltakerHistorikk()

    data class ImportertFraArena(
        val importertFraArena: no.nav.amt.lib.models.deltaker.ImportertFraArena,
    ) : DeltakerHistorikk()

    data class Endring(
        val endring: DeltakerEndring,
    ) : DeltakerHistorikk()

    data class Vedtak(
        val vedtak: no.nav.amt.lib.models.deltaker.Vedtak,
    ) : DeltakerHistorikk()

    data class InnsokPaaFellesOppstart(
        val data: no.nav.amt.lib.models.deltaker.InnsokPaaFellesOppstart,
    ) : DeltakerHistorikk()

    data class Forslag(
        val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag,
    ) : DeltakerHistorikk()

    data class EndringFraArrangor(
        val endringFraArrangor: no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor,
    ) : DeltakerHistorikk()

    data class EndringFraTiltakskoordinator(
        val endringFraTiltakskoordinator: no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator,
    ) : DeltakerHistorikk()

    fun navAnsatte() = when (this) {
        is Endring -> listOf(this.endring.endretAv)
        is Vedtak -> listOfNotNull(this.vedtak.sistEndretAv, this.vedtak.opprettetAv)
        is InnsokPaaFellesOppstart -> listOfNotNull(this.data.innsoktAv)
        is Forslag -> listOfNotNull(this.forslag.getNavAnsatt()?.id)
        is EndringFraArrangor -> emptyList()
        is ImportertFraArena -> emptyList()
        is VurderingFraArrangor -> emptyList()
        is EndringFraTiltakskoordinator -> listOf(this.endringFraTiltakskoordinator.endretAv)
    }

    fun navEnheter() = when (this) {
        is Endring -> listOf(this.endring.endretAvEnhet)
        is Vedtak -> listOfNotNull(this.vedtak.sistEndretAvEnhet, this.vedtak.opprettetAvEnhet)
        is InnsokPaaFellesOppstart -> listOfNotNull(this.data.innsoktAvEnhet)
        is Forslag -> listOfNotNull(this.forslag.getNavAnsatt()?.enhetId)
        is EndringFraArrangor -> emptyList()
        is ImportertFraArena -> emptyList()
        is VurderingFraArrangor -> emptyList()
        is EndringFraTiltakskoordinator -> listOf(this.endringFraTiltakskoordinator.endretAvEnhet)
    }
}
