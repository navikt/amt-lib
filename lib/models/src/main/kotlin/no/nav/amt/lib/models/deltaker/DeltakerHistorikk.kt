package no.nav.amt.lib.models.deltaker

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class DeltakerHistorikk {
    val sistEndret
        get() = when (this) {
            is Endring -> endring.endret
            is Vedtak -> vedtak.sistEndret
            is Forslag -> forslag.sistEndret
            is EndringFraArrangor -> endringFraArrangor.opprettet
            is ImportertFraArena -> importertFraArena.importertDato
        }

    data class ImportertFraArena(
        val importertFraArena: no.nav.amt.lib.models.deltaker.ImportertFraArena
    ) : DeltakerHistorikk()

    data class Endring(
        val endring: DeltakerEndring,
    ) : DeltakerHistorikk()

    data class Vedtak(
        val vedtak: no.nav.amt.lib.models.deltaker.Vedtak,
    ) : DeltakerHistorikk()

    data class Forslag(
        val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag,
    ) : DeltakerHistorikk()

    data class EndringFraArrangor(
        val endringFraArrangor: no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor,
    ) : DeltakerHistorikk()

    fun navAnsatte() = when (this) {
        is Endring -> listOf(this.endring.endretAv)
        is Vedtak -> listOfNotNull(this.vedtak.sistEndretAv, this.vedtak.opprettetAv)
        is Forslag -> listOfNotNull(this.forslag.getNavAnsatt()?.id)
        is EndringFraArrangor -> emptyList()
        is ImportertFraArena -> emptyList()
    }

    fun navEnheter() = when (this) {
        is Endring -> listOf(this.endring.endretAvEnhet)
        is Vedtak -> listOfNotNull(this.vedtak.sistEndretAvEnhet, this.vedtak.opprettetAvEnhet)
        is Forslag -> listOfNotNull(this.forslag.getNavAnsatt()?.enhetId)
        is EndringFraArrangor -> emptyList()
        is ImportertFraArena -> emptyList()
    }
}