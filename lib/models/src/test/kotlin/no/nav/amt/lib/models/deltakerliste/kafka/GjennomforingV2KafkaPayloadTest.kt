package no.nav.amt.lib.models.deltakerliste.kafka

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.deltakerliste.GjennomforingPameldingType
import no.nav.amt.lib.models.deltakerliste.GjennomforingStatusType
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class GjennomforingV2KafkaPayloadTest {
    @ParameterizedTest
    @EnumSource(
        Tiltakskode::class,
        names = [
            "ARBEIDSFORBEREDENDE_TRENING", "ARBEIDSRETTET_REHABILITERING", "AVKLARING",
            "DIGITALT_OPPFOLGINGSTILTAK", "OPPFOLGING", "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
        ],
    )
    fun `direktetiltak skal validere`(tiltakskode: Tiltakskode) {
        shouldNotThrowAny {
            gruppeGjennomforing
                .copy(tiltakskode = tiltakskode)
                .assertPameldingstypeIsValid()
        }
    }

    @Test
    fun `direktetiltak skal ikke validere`() {
        val thrown = shouldThrow<IllegalArgumentException> {
            gruppeGjennomforing
                .copy(
                    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                    pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
                ).assertPameldingstypeIsValid()
        }

        thrown.message shouldBe "ARBEIDSFORBEREDENDE_TRENING krever DIREKTE_VEDTAK"
    }

    @ParameterizedTest
    @EnumSource(
        Tiltakskode::class,
        names = [
            "GRUPPE_ARBEIDSMARKEDSOPPLAERING", "GRUPPE_FAG_OG_YRKESOPPLAERING", "JOBBKLUBB",
        ],
    )
    fun `gruppetiltak, felles oppstart skal validere`(tiltakskode: Tiltakskode) {
        shouldNotThrowAny {
            gruppeGjennomforing
                .copy(
                    tiltakskode = tiltakskode,
                    oppstart = Oppstartstype.FELLES,
                    pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
                ).assertPameldingstypeIsValid()
        }
    }

    @Test
    fun `gruppetiltak, felles oppstart skal ikke validere`() {
        val thrown = shouldThrow<IllegalArgumentException> {
            gruppeGjennomforing
                .copy(
                    tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                    oppstart = Oppstartstype.FELLES,
                    pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
                ).assertPameldingstypeIsValid()
        }

        thrown.message shouldBe "FELLES oppstart for GRUPPE_ARBEIDSMARKEDSOPPLAERING krever TRENGER_GODKJENNING"
    }

    @ParameterizedTest
    @EnumSource(
        Tiltakskode::class,
        names = [
            "GRUPPE_ARBEIDSMARKEDSOPPLAERING", "GRUPPE_FAG_OG_YRKESOPPLAERING", "JOBBKLUBB",
        ],
    )
    fun `gruppetiltak, lopende oppstart skal validere`(tiltakskode: Tiltakskode) {
        shouldNotThrowAny {
            gruppeGjennomforing
                .copy(
                    tiltakskode = tiltakskode,
                    oppstart = Oppstartstype.LOPENDE,
                    pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
                ).assertPameldingstypeIsValid()
        }
    }

    @Test
    fun `gruppetiltak, lopende oppstart skal ikke validere`() {
        val thrown = shouldThrow<IllegalArgumentException> {
            gruppeGjennomforing
                .copy(
                    tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                    oppstart = Oppstartstype.LOPENDE,
                    pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
                ).assertPameldingstypeIsValid()
        }

        thrown.message shouldBe "LOPENDE oppstart for GRUPPE_ARBEIDSMARKEDSOPPLAERING krever DIREKTE_VEDTAK"
    }

    companion object {
        private val gruppeGjennomforing = GjennomforingV2KafkaPayload.Gruppe(
            id = UUID.randomUUID(),
            opprettetTidspunkt = OffsetDateTime.now(),
            oppdatertTidspunkt = OffsetDateTime.now(),
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            arrangor = GjennomforingV2KafkaPayload.Arrangor(organisasjonsnummer = "123456789"),
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            navn = "navn",
            startDato = LocalDate.now(),
            sluttDato = null,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = Oppstartstype.LOPENDE,
            tilgjengeligForArrangorFraOgMedDato = null,
            apentForPamelding = true,
            antallPlasser = 25,
            deltidsprosent = 50.0,
            oppmoteSted = "Oslo",
        )
    }
}
