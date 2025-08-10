package no.nav.amt.lib.viewmodels.person

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class NavEnhetDtoTest :
    StringSpec({

        "Skal mappe NavEnhetDto til NavEnhet" {
            val dto = NavEnhetDto(
                id = UUID.randomUUID(),
                enhetId = "~enhetId~",
                navn = "~navn~",
            )

            val enhet = dto.toModel()

            assertSoftly(enhet) {
                id shouldBe dto.id
                enhetsnummer shouldBe dto.enhetId
                navn shouldBe dto.navn
            }
        }
    })
