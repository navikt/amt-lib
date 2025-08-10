package no.nav.amt.lib.viewmodels.person

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import no.nav.amt.lib.models.person.Oppfolgingsperiode
import no.nav.amt.lib.models.person.address.Adresse
import no.nav.amt.lib.models.person.address.Adressebeskyttelse
import no.nav.amt.lib.models.person.address.Bostedsadresse
import no.nav.amt.lib.models.person.address.Kontaktadresse
import no.nav.amt.lib.models.person.address.Matrikkeladresse
import no.nav.amt.lib.models.person.address.Oppholdsadresse
import no.nav.amt.lib.models.person.address.Postboksadresse
import no.nav.amt.lib.models.person.address.Vegadresse
import java.time.LocalDateTime
import java.util.UUID

class NavBrukerDtoTest :
    StringSpec({

        "Skal mappe NavBrukerDto til NavBruker når alle verdier er satt" {
            val navBruker = brukerDtoInTest.toModel()

            assertSoftly(navBruker) {
                personId shouldBe brukerDtoInTest.personId
                personident shouldBe brukerDtoInTest.personident
                fornavn shouldBe brukerDtoInTest.fornavn
                mellomnavn shouldBe brukerDtoInTest.mellomnavn
                etternavn shouldBe brukerDtoInTest.etternavn
                navVeilederId shouldBe brukerDtoInTest.navVeilederId
                navEnhetId shouldBe navEnhetInTest.id
                telefon shouldBe brukerDtoInTest.telefon
                epost shouldBe brukerDtoInTest.epost
                erSkjermet shouldBe brukerDtoInTest.erSkjermet
                adresse shouldBe brukerDtoInTest.adresse
                adressebeskyttelse shouldBe brukerDtoInTest.adressebeskyttelse
                oppfolgingsperioder shouldBe brukerDtoInTest.oppfolgingsperioder
                innsatsgruppe shouldBe brukerDtoInTest.innsatsgruppe
            }
        }

        "Skal mappe NavBrukerDto til NavBruker når kun påkrevde felter er satt" {
            val navBruker = brukerDtoInTest
                .copy(
                    mellomnavn = null,
                    navVeilederId = null,
                    navEnhet = null,
                    telefon = null,
                    epost = null,
                    adresse = null,
                    adressebeskyttelse = null,
                    oppfolgingsperioder = emptyList(),
                    innsatsgruppe = null,
                ).toModel()

            assertSoftly(navBruker) {
                mellomnavn shouldBe null
                navVeilederId shouldBe null
                navEnhetId shouldBe null
                telefon shouldBe null
                epost shouldBe null
                adresse shouldBe null
                adressebeskyttelse shouldBe null
                oppfolgingsperioder shouldBe emptyList()
                innsatsgruppe shouldBe null
            }
        }
    }) {
    companion object {
        private val navEnhetInTest = NavEnhetDto(
            id = UUID.randomUUID(),
            enhetId = "~enhetId~",
            navn = "~enhet-navn~",
        )

        private val adresseInTest = Adresse(
            bostedsadresse = Bostedsadresse(
                coAdressenavn = "~bostedsadresse.coAdressenavn~",
                vegadresse = Vegadresse(
                    husnummer = "~bostedsadresse.vegadresse.husnummer~",
                    husbokstav = "~bostedsadresse.vegadresse.husbokstav~",
                    adressenavn = "~bostedsadresse.vegadresse.adressenavn~",
                    tilleggsnavn = "~bostedsadresse.vegadresse.tilleggsnavn~",
                    postnummer = "~bostedsadresse.vegadresse.postnummer~",
                    poststed = "~bostedsadresse.vegadresse.poststed~",
                ),
                matrikkeladresse = Matrikkeladresse(
                    tilleggsnavn = "~bostedsadresse.matrikkeladresse.tilleggsnavn~",
                    postnummer = "~bostedsadresse.matrikkeladresse.postnummer~",
                    poststed = "~bostedsadresse.matrikkeladresse.poststed~",
                ),
            ),
            oppholdsadresse = Oppholdsadresse(
                coAdressenavn = "~oppholdsadresse.coAdressenavn~",
                vegadresse = Vegadresse(
                    husnummer = "~oppholdsadresse.vegadresse.husnummer~",
                    husbokstav = "~oppholdsadresse.vegadresse.husbokstav~",
                    adressenavn = "~oppholdsadresse.vegadresse.adressenavn~",
                    tilleggsnavn = "~oppholdsadresse.vegadresse.tilleggsnavn~",
                    postnummer = "~oppholdsadresse.vegadresse.postnummer~",
                    poststed = "~oppholdsadresse.vegadresse.poststed~",
                ),
                matrikkeladresse = Matrikkeladresse(
                    tilleggsnavn = "~oppholdsadresse.matrikkeladresse.tilleggsnavn~",
                    postnummer = "~oppholdsadresse.matrikkeladresse.postnummer~",
                    poststed = "~oppholdsadresse.matrikkeladresse.poststed~",
                ),
            ),
            kontaktadresse = Kontaktadresse(
                coAdressenavn = "~kontaktadresse.coAdressenavn~",
                vegadresse = Vegadresse(
                    husnummer = "~kontaktadresse.vegadresse.husnummer~",
                    husbokstav = "~kontaktadresse.vegadresse.husbokstav~",
                    adressenavn = "~kontaktadresse.vegadresse.adressenavn~",
                    tilleggsnavn = "~kontaktadresse.vegadresse.tilleggsnavn~",
                    postnummer = "~kontaktadresse.vegadresse.postnummer~",
                    poststed = "~kontaktadresse.vegadresse.poststed~",
                ),
                postboksadresse = Postboksadresse(
                    postboks = "~kontaktadresse.postboksadresse.postboks~",
                    postnummer = "~kontaktadresse.postboksadresse.postnummer~",
                    poststed = "~kontaktadresse.postboksadresse.poststed~",
                ),
            ),
        )

        private val oppfolgingsperiodeInTest = Oppfolgingsperiode(
            id = UUID.randomUUID(),
            startdato = LocalDateTime.now(),
            sluttdato = null,
        )

        private val brukerDtoInTest = NavBrukerDto(
            personId = UUID.randomUUID(),
            personident = "~personident~",
            fornavn = "~fornavn~",
            mellomnavn = "~mellomnavn~",
            etternavn = "~etternavn~",
            navVeilederId = UUID.randomUUID(),
            navEnhet = navEnhetInTest,
            telefon = "~telefon~",
            epost = "~epost~",
            erSkjermet = true,
            adresse = adresseInTest,
            adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG,
            oppfolgingsperioder = listOf(oppfolgingsperiodeInTest),
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
        )
    }
}
