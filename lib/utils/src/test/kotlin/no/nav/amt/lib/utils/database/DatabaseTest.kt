package no.nav.amt.lib.utils.database

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.database.Database.withTransaction
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

class DatabaseTest {
    private var testRepository: DatabaseTestRepository

    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
        testRepository = DatabaseTestRepository()
    }

    @Test
    fun `transaksjon rulles tilbake ved feil`() = runTest {
        val n = 1

        try {
            withTransaction {
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

        withTransaction {
            testRepository.insert(n)
        }

        testRepository.get(n) shouldBe n
    }

    @Test
    fun `oppretter ny session hvis en ikke finnes fra for`() = runTest {
        val n = 998

        testRepository.insert(n)

        testRepository.get(n) shouldBe n
    }

    @Test
    fun `det er ikke mulig a bruke nostede transaksjoner`() = runTest {
        val illegalStateException = shouldThrow<IllegalStateException> {
            withTransaction {
                withTransaction {
                    testRepository.insert(43)
                }
            }
        }

        illegalStateException.message shouldBe "Nested transactions are not supported"
    }

    @Test
    fun `skal feile nar session#transaction benyttes direkte`(): Unit = runTest {
        val throwable = shouldThrow<PSQLException> {
            withTransaction {
                Database.query { session ->
                    session.transaction {
                        runBlocking {
                            testRepository.insert(43)
                        }
                    }
                }
            }
        }

        throwable.message shouldBe "Cannot commit when autoCommit is enabled."
    }
}
