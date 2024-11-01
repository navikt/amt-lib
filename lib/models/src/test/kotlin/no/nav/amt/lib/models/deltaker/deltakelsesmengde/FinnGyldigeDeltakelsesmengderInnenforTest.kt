package no.nav.amt.lib.models.deltaker.deltakelsesmengde

import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.utils.TestData
import org.junit.jupiter.api.Test

class FinnGyldigeDeltakelsesmengderInnenforTest {
    @Test
    fun `finnGyldigeDeltakelsesmengderInnenfor - kun vedtak - returnerer riktig perioder`() {
        val fraDato = "2024-01-01".toDate()
        val tilDato = "2024-01-31".toDate()

        val vedtak = TestData.lagVedtak(fattet = fraDato.atStartOfDay())
        val historikk = TestData.lagDeltakerHistorikk(listOf(vedtak))

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

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

        val fraDato = importertFraArena.deltakerVedImport.innsoktDato
        val tilDato = fraDato.plusMonths(1)

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

        perioder.size shouldBe 1
        perioder[0].deltakelsesprosent shouldBe importertFraArena.deltakerVedImport.deltakelsesprosent
        perioder[0].dagerPerUke shouldBe importertFraArena.deltakerVedImport.dagerPerUke
        perioder[0].gyldigFra shouldBe importertFraArena.deltakerVedImport.innsoktDato
        perioder[0].opprettet shouldBe importertFraArena.deltakerVedImport.innsoktDato.atStartOfDay()
    }

    @Test
    fun `finnGyldigeDeltakelsesmengderInnenfor - vedtak og endring - returnerer riktig perioder`() {
        val fraDato = "2024-01-01".toDate()
        val tilDato = "2024-01-31".toDate()

        val vedtak = TestData.lagVedtak(fattet = fraDato.atStartOfDay())
        val endring = TestData.lagEndreDeltakelsesmengde(50, fraDato.plusDays(15), fraDato.plusDays(10).atStartOfDay())
        val historikk = TestData.lagDeltakerHistorikk(listOf(vedtak), endringer = listOf(endring))

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

        perioder.size shouldBe 2
        perioder shouldBe listOf(vedtak.toDeltakelsesmengde(), endring.toDeltakelsesmengde())
    }

    @Test
    fun `finnGyldigeDeltakelsesmengderInnenfor - vedtak og endring samme dato - returnerer riktig periode`() {
        val fraDato = "2024-01-01".toDate()
        val tilDato = "2024-01-31".toDate()

        val vedtak = TestData.lagVedtak(fattet = fraDato.atStartOfDay())
        val endring = TestData.lagEndreDeltakelsesmengde(50, fraDato, fraDato.plusDays(10).atStartOfDay())
        val historikk = TestData.lagDeltakerHistorikk(listOf(vedtak), endringer = listOf(endring))

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

        perioder.size shouldBe 1
        perioder shouldBe listOf(endring.toDeltakelsesmengde())
    }

    @Test
    fun `finnGyldigeDeltakelsesmengderInnenfor - flere gyldige og ugyldige endringer - returnerer riktig periode`() {
        val fraDato = "2024-01-15".toDate()
        val tilDato = "2024-01-31".toDate()

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

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

        perioder.size shouldBe 2
        perioder shouldBe listOf(gyldigeDeltakelsesmengder[1], gyldigeDeltakelsesmengder[2]).map { it.toDeltakelsesmengde() }
    }

    @Test
    fun `finnGyldigeDeltakelsesmengderInnenfor - tilDato er null - returnerer riktig periode`() {
        val fraDato = "2024-01-01".toDate()
        val tilDato = null

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

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

        perioder.size shouldBe 3
        perioder shouldBe gyldigeDeltakelsesmengder.map { it.toDeltakelsesmengde() }
    }

    @Test
    fun `finnGyldigeDeltakelsesmengderInnenfor - flere endringer med samme gyldigFra - returnerer riktig periode`() {
        val fraDato = "2024-01-01".toDate()
        val tilDato = "2024-01-31".toDate()

        val ugyldigDeltakelsesmengde = TestData.lagEndreDeltakelsesmengde(
            deltakelsesprosent = 90,
            gyldigFra = "2024-01-01".toDate(),
            opprettet = "2024-01-05".toDateTime(),
        )
        val gyldigDeltakelsesmengde =
            TestData.lagEndreDeltakelsesmengde(
                deltakelsesprosent = 69,
                gyldigFra = "2024-01-01".toDate(),
                opprettet = "2024-01-14".toDateTime(),
            )

        val historikk = TestData.lagDeltakerHistorikk(
            endringer = listOf(gyldigDeltakelsesmengde, ugyldigDeltakelsesmengde),
        )

        val perioder = finnGyldigeDeltakelsesmengderInnenfor(historikk, fraDato, tilDato)

        perioder.size shouldBe 1
        perioder[0] shouldBe gyldigDeltakelsesmengde.toDeltakelsesmengde()
    }
}
