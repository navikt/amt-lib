package no.nav.amt.lib.viewmodels.person

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

class NavAnsattDtoTest :
    StringSpec({

        "Skal kunne mappe en NavAnsattDto til NavAnsatt når alle verdier er satt" {
            val navAnsatt = ansattDtoInTest.toModel()

            assertSoftly(navAnsatt) {
                id shouldBe ansattDtoInTest.id
                navIdent shouldBe ansattDtoInTest.navident
                navn shouldBe ansattDtoInTest.navn
                epost shouldBe ansattDtoInTest.epost
                telefon shouldBe ansattDtoInTest.telefon
                navEnhetId shouldBe ansattDtoInTest.navEnhetId
            }
        }

        "Skal kunne mappe en NavAnsattDto til NavAnsatt når epost og telefon er null" {
            val navAnsatt = ansattDtoInTest
                .copy(
                    epost = null,
                    telefon = null,
                ).toModel()

            assertSoftly(navAnsatt) {
                epost.shouldBeNull()
                telefon.shouldBeNull()
            }
        }
    }) {
    companion object {
        private val ansattDtoInTest = NavAnsattDto(
            id = UUID.randomUUID(),
            navident = "~navident~",
            navn = "~navn~",
            epost = "~epost~",
            telefon = "~telefon~",
            navEnhetId = UUID.randomUUID(),
        )
    }
}
