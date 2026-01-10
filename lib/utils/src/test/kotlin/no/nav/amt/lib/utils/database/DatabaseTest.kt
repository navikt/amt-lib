package no.nav.amt.lib.utils.database

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

class DatabaseTest {
    private var testRepository: DatabaseTestRepository

    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
        testRepository = DatabaseTestRepository()
    }

    @BeforeEach
    fun setup() {
        testRepository.cleanUp()
    }

    @Test
    fun `transaksjon rulles tilbake ved feil`() = runTest {
        val n = 1

        try {
            Database.transaction {
                testRepository.insert(n)
                throw IllegalStateException("Skal rulle tilbake insert")
            }
        } catch (_: IllegalStateException) {
            println("Feil etter insert")
        }

        testRepository.get(n) shouldBe null
    }

    @Test
    fun `transaksjon committes uten feil`() = runTest {
        val n = 999

        Database.transaction {
            testRepository.insert(n)
        }

        testRepository.get(n) shouldBe n
    }

    @Test
    fun `det er ikke mulig å bruke transaksjoner i en transaksjon`() = runTest {
        shouldThrow<PSQLException> {
            Database.transaction {
                Database.query { s ->
                    s.transaction {
                        testRepository.insert(43)
                    }
                }
            }
        }
    }
}
