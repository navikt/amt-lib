package no.nav.amt.lib.models.hendelse

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import java.time.LocalDate
import java.time.ZonedDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface HendelseType {
    sealed interface HendelseMedForslag : HendelseType {
        val begrunnelseFraNav: String?
        val begrunnelseFraArrangor: String?
        val endringFraForslag: Forslag.Endring?
    }

    sealed interface HendelseSystemKanOpprette : HendelseType

    sealed class SettPaaVenteliste : HendelseType

    data class OpprettUtkast(
        val utkast: UtkastDto,
    ) : HendelseType

    data class EndreUtkast(
        val utkast: UtkastDto,
    ) : HendelseType

    data class AvbrytUtkast(
        val utkast: UtkastDto,
    ) : HendelseSystemKanOpprette

    data class InnbyggerGodkjennUtkast(
        val utkast: UtkastDto,
    ) : HendelseType

    data class NavGodkjennUtkast(
        val utkast: UtkastDto,
    ) : HendelseType

    data class EndreBakgrunnsinformasjon(
        val bakgrunnsinformasjon: String?,
    ) : HendelseType

    data class EndreInnhold(
        val innhold: List<InnholdDto>,
    ) : HendelseType

    data class EndreDeltakelsesmengde(
        val deltakelsesprosent: Float?,
        val dagerPerUke: Float?,
        val gyldigFra: LocalDate?,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreStartdato(
        val startdato: LocalDate?,
        val sluttdato: LocalDate? = null,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreSluttdato(
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class IkkeAktuell(
        val aarsak: DeltakerEndring.Aarsak,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class AvsluttDeltakelse(
        val aarsak: DeltakerEndring.Aarsak,
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreSluttarsak(
        val aarsak: DeltakerEndring.Aarsak,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class FjernOppstartsdato(
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class DeltakerSistBesokt(
        val sistBesokt: ZonedDateTime,
    ) : HendelseType

    data class ReaktiverDeltakelse(
        val utkast: UtkastDto,
        val begrunnelseFraNav: String,
    ) : HendelseType

    data class LeggTilOppstartsdato(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
    ) : HendelseType
}

data class UtkastDto(
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<InnholdDto>?,
)

data class InnholdDto(
    val tekst: String,
    val innholdskode: String,
    val beskrivelse: String?,
)

fun DeltakerEndring.toHendelseEndring(utkast: UtkastDto? = null) = when (val endring = this.endring) {
    is DeltakerEndring.Endring.AvsluttDeltakelse -> HendelseType.AvsluttDeltakelse(
        aarsak = endring.aarsak,
        sluttdato = endring.sluttdato,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.EndreBakgrunnsinformasjon -> HendelseType.EndreBakgrunnsinformasjon(
        endring.bakgrunnsinformasjon,
    )

    is DeltakerEndring.Endring.EndreDeltakelsesmengde -> HendelseType.EndreDeltakelsesmengde(
        endring.deltakelsesprosent,
        endring.dagerPerUke,
        endring.gyldigFra,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.EndreInnhold -> HendelseType.EndreInnhold(
        endring.innhold.map { InnholdDto(it.tekst, it.innholdskode, it.beskrivelse) },
    )

    is DeltakerEndring.Endring.EndreSluttarsak -> HendelseType.EndreSluttarsak(
        endring.aarsak,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.EndreSluttdato -> HendelseType.EndreSluttdato(
        endring.sluttdato,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.EndreStartdato -> HendelseType.EndreStartdato(
        endring.startdato,
        endring.sluttdato,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.ForlengDeltakelse -> HendelseType.ForlengDeltakelse(
        sluttdato = endring.sluttdato,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.IkkeAktuell -> HendelseType.IkkeAktuell(
        aarsak = endring.aarsak,
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.FjernOppstartsdato -> HendelseType.FjernOppstartsdato(
        begrunnelseFraNav = endring.begrunnelse,
        begrunnelseFraArrangor = forslag?.begrunnelse,
        endringFraForslag = forslag?.endring,
    )

    is DeltakerEndring.Endring.ReaktiverDeltakelse -> utkast?.let {
        HendelseType.ReaktiverDeltakelse(
            utkast,
            endring.begrunnelse,
        )
    } ?: throw IllegalStateException("Mangler utkast for reaktivert deltakelse")
}

fun EndringFraArrangor.toHendelseEndring() = when (val endring = this.endring) {
    is EndringFraArrangor.LeggTilOppstartsdato ->
        HendelseType.LeggTilOppstartsdato(
            startdato = endring.startdato,
            sluttdato = endring.sluttdato,
        )
}
