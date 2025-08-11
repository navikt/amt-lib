package no.nav.amt.lib.ktor.clients

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.lib.ktor.clients.ClientTestUtils.createMockHttpClient
import no.nav.amt.lib.models.person.dto.NavBrukerFodselsarDto
import no.nav.amt.lib.testing.testdata.person.PersonDtoTestsData.brukerDtoInTest
import no.nav.amt.lib.testing.testdata.person.PersonDtoTestsData.enhetDtoInTest
import no.nav.amt.lib.testing.testdata.person.PersonModelsTestData.ansattInTest
import no.nav.amt.lib.testing.testdata.person.PersonModelsTestData.enhetInTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Year

class AmtPersonServiceClientTest {
    @Nested
    inner class HentNavAnsattByNavIdent {
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-ansatt"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-ansatt fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansattInTest.navIdent)
            }
        }

        @Test
        fun `skal returnere NavAnsatt`() {
            runHappyPathTest(expectedUrl, ansattInTest) { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansattInTest.navIdent)
            }
        }
    }

    @Nested
    inner class HentNavAnsattById {
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-ansatt/${ansattInTest.id}"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-ansatt fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansattInTest.id)
            }
        }

        @Test
        fun `skal returnere NavAnsatt`() {
            runHappyPathTest(expectedUrl, ansattInTest) { personServiceClient ->
                personServiceClient.hentNavAnsatt(ansattInTest.id)
            }
        }
    }

    @Nested
    inner class HentNavEnhetByNavEnhetsnummer {
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-enhet"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-enhet fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavEnhet(enhetInTest.enhetsnummer)
            }
        }

        @Test
        fun `skal returnere NavEnhet`() {
            val expected = enhetDtoInTest.toModel()

            runHappyPathTest(expectedUrl, expected, enhetDtoInTest) { personServiceClient ->
                personServiceClient.hentNavEnhet(expected.enhetsnummer)
            }
        }
    }

    @Nested
    inner class HentNavEnhetById {
        val expected = enhetDtoInTest.toModel()
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-enhet/${expected.id}"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente NAV-enhet fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavEnhet(expected.id)
            }
        }

        @Test
        fun `skal returnere NavEnhet`() {
            runHappyPathTest(expectedUrl, expected, enhetDtoInTest) { personServiceClient ->
                personServiceClient.hentNavEnhet(expected.id)
            }
        }
    }

    @Nested
    inner class HentNavBruker {
        val expectedUrl = "$PERSON_SVC_BASE_URL/api/nav-bruker"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(expectedUrl, "Kunne ikke hente nav-bruker fra amt-person-service") { personServiceClient ->
                personServiceClient.hentNavBruker("~personident~")
            }
        }

        @Test
        fun `skal returnere NavBruker`() {
            val expectedBruker = brukerDtoInTest.toModel()

            runHappyPathTest(expectedUrl, expectedBruker, brukerDtoInTest) { personServiceClient ->
                personServiceClient.hentNavBruker("~personident~")
            }
        }
    }

    @Nested
    inner class HentNavBrukerFodselsar {
        val expectedUrl = "${PERSON_SVC_BASE_URL}/api/nav-bruker-fodselsar"

        @Test
        fun `skal kaste feil hvis respons har feilkode`() {
            runFailureTest(
                expectedUrl,
                "Kunne ikke hente fodselsar for nav-bruker fra amt-person-service",
            ) { personServiceClient ->
                personServiceClient.hentNavBrukerFodselsar("~personident~")
            }
        }

        @Test
        fun `skal returnere fodselsar`() {
            val brukerFodselsarDto = NavBrukerFodselsarDto(Year.now().value - 20)
            runHappyPathTest(
                expectedUrl,
                brukerFodselsarDto.fodselsar,
                brukerFodselsarDto,
            ) { personServiceClient ->
                personServiceClient.hentNavBrukerFodselsar("~personident~")
            }
        }
    }

    companion object {
        private const val PERSON_SVC_BASE_URL = "http://amt-person-svc"

        private fun runFailureTest(
            expectedUrl: String,
            expectedError: String,
            block: suspend (AmtPersonServiceClient) -> Unit,
        ) {
            val thrown = runBlocking {
                shouldThrow<RuntimeException> {
                    block(createPersonServiceClient(expectedUrl, HttpStatusCode.Unauthorized))
                }
            }
            thrown.message shouldStartWith expectedError
        }

        private fun <T : Any> runHappyPathTest(
            expectedUrl: String,
            expectedResponse: T,
            responseBody: Any = expectedResponse,
            block: suspend (AmtPersonServiceClient) -> T,
        ) = runBlocking {
            val personServiceClient = createPersonServiceClient(expectedUrl, HttpStatusCode.OK, responseBody)
            block(personServiceClient) shouldBe expectedResponse
        }

        private fun createPersonServiceClient(
            expectedUrl: String,
            statusCode: HttpStatusCode = HttpStatusCode.OK,
            responseBody: Any? = null,
        ) = AmtPersonServiceClient(
            baseUrl = PERSON_SVC_BASE_URL,
            scope = "scope",
            httpClient = createMockHttpClient(expectedUrl, responseBody, statusCode),
            azureAdTokenClient = mockk(relaxed = true),
        )
    }
}
