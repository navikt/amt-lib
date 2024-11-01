package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.utils.TestData
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FinnGyldigeDeltakelsesmengderTest {
    @Test
    fun `finnGyldigeDeltakelsesmengder - kun vedtak - returnerer riktig perioder`() {
        val vedtak = TestData.lagVedtak(fattet = LocalDateTime.now())
        val historikk = TestData.lagDeltakerHistorikk(listOf(vedtak))

        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder.size shouldBe 1
        perioder[0].deltakelsesprosent shouldBe vedtak.deltakerVedVedtak.deltakelsesprosent
        perioder[0].dagerPerUke shouldBe vedtak.deltakerVedVedtak.dagerPerUke
        perioder[0].gyldigFra shouldBe vedtak.fattet!!.toLocalDate()
        perioder[0].opprettet shouldBe vedtak.fattet!!
    }

    @Test
    fun `finnGyldigeDeltakelsesmengder - kun importert fra arena - returnerer riktig perioder`() {
        val importertFraArena = TestData.lagImportertFraArena()
        val historikk = TestData.lagDeltakerHistorikk(importertFraArena = listOf(importertFraArena))

        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder.size shouldBe 1
        perioder[0].deltakelsesprosent shouldBe importertFraArena.deltakerVedImport.deltakelsesprosent
        perioder[0].dagerPerUke shouldBe importertFraArena.deltakerVedImport.dagerPerUke
        perioder[0].gyldigFra shouldBe importertFraArena.deltakerVedImport.innsoktDato
        perioder[0].opprettet shouldBe importertFraArena.deltakerVedImport.innsoktDato.atStartOfDay()
    }

    @Test
    fun `finnGyldigeDeltakelsesmengder - ingen overlappende endringer`() {
        val endringer = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 50,
                gyldigFra = "2024-01-01".toDate(),
                opprettet = "2024-01-01".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 75,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-09".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 90,
                gyldigFra = "2024-01-20".toDate(),
                opprettet = "2024-01-15".toDateTime(),
            ),
        )

        val historikk = TestData.lagDeltakerHistorikk(endringer = endringer)
        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder.size shouldBe 3
        perioder shouldBe endringer.map { it.toDeltakelsesmengde() }
    }

    @Test
    fun `finnGyldigeDeltakelsesmengder - med flere overlappende endringer`() {
        val ugyldigeDeltakelsesmengder = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 90,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-05".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 80,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-06".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 70,
                gyldigFra = "2024-01-05".toDate(),
                opprettet = "2024-01-10".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 60,
                gyldigFra = "2024-01-15".toDate(),
                opprettet = "2024-01-11".toDateTime(),
            ),
        )

        val gyldigeDeltakelsesmengder = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 69,
                gyldigFra = "2024-01-01".toDate(),
                opprettet = "2024-01-01".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 69,
                gyldigFra = "2024-01-02".toDate(),
                opprettet = "2024-01-14".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 100,
                gyldigFra = "2024-01-15".toDate(),
                opprettet = "2024-01-15".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 90,
                gyldigFra = "2024-01-30".toDate(),
                opprettet = "2024-01-25".toDateTime(),
            ),
        )
        val historikk = TestData.lagDeltakerHistorikk(
            endringer = gyldigeDeltakelsesmengder + ugyldigeDeltakelsesmengder,
        )

        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder.size shouldBe 4
        perioder shouldBe gyldigeDeltakelsesmengder.map { it.toDeltakelsesmengde() }
    }

    @Test
    fun `finnGyldigeDeltakelsesmengder - en overlappende endring`() {
        val gyldigeEndringer = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 60,
                gyldigFra = "2024-01-01".toDate(),
                opprettet = "2024-01-01".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 100,
                gyldigFra = "2024-01-15".toDate(),
                opprettet = "2024-01-15".toDateTime(),
            ),
        )

        val ugyldigeEndringer = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 50,
                gyldigFra = "2024-01-15".toDate(),
                opprettet = "2024-01-12".toDateTime(),
            ),
        )

        val historikk = TestData.lagDeltakerHistorikk(endringer = gyldigeEndringer + ugyldigeEndringer)
        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder shouldBe gyldigeEndringer.map { it.toDeltakelsesmengde() }
    }

    @Test
    fun `finnGyldigeDeltakelsesmengder - nyeste overlappende endring benyttes`() {
        val endringer = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 80,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-05".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 100,
                gyldigFra = "2024-01-05".toDate(),
                opprettet = "2024-01-14".toDateTime(),
            ),
        )

        val historikk = TestData.lagDeltakerHistorikk(endringer = endringer)
        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder.size shouldBe 1
        perioder.first().deltakelsesprosent shouldBe 100
    }

    @Test
    fun `finnGyldigeDeltakelsesmengder - flere endringer på samme dato, siste benyttes`() {
        val endringer = listOf(
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 70,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-08".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 80,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-09".toDateTime(),
            ),
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 90,
                gyldigFra = "2024-01-10".toDate(),
                opprettet = "2024-01-10".toDateTime(),
            ),
        )

        val historikk = TestData.lagDeltakerHistorikk(endringer = endringer)
        val perioder = finnGyldigeDeltakelsesmengder(historikk)

        perioder.size shouldBe 1
        perioder.first().deltakelsesprosent shouldBe 90
    }
}

fun String.toDate(): LocalDate = LocalDate.parse(this)

fun String.toDateTime(): LocalDateTime = this.toDate().atStartOfDay()
