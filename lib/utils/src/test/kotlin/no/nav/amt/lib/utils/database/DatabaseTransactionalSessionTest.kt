package no.nav.amt.lib.utils.database

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotliquery.TransactionalSession
import no.nav.amt.lib.testing.SingletonPostgres16Container
import no.nav.amt.lib.utils.database.Database.TxContext
import no.nav.amt.lib.utils.database.Database.withTransaction
import org.junit.jupiter.api.Test

class DatabaseTransactionalSessionTest {
    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
    }

    @Test
    fun `transactionalSession skal vare null uten en transaksjonsblokk`() = runTest {
        currentCoroutineContext()[TxContext].shouldBeNull()
    }

    @Test
    fun `transactionalSession skal ikke vare null i en transaksjonsblokk og fjernes etter blokken`() = runTest {
        withTransaction {
            currentCoroutineContext()[TxContext].shouldNotBeNull()
        }
        currentCoroutineContext()[TxContext].shouldBeNull()
    }

    @Test
    fun `transaksjoner skal ikke gjenbruke session`() = runTest {
        var firstSession: TransactionalSession? = null
        var secondSession: TransactionalSession? = null

        withTransaction {
            firstSession = Database.currentTransactionalSession()
        }
        withTransaction {
            secondSession = Database.currentTransactionalSession()
        }

        firstSession shouldNotBe null
        secondSession shouldNotBe null
        firstSession shouldNotBe secondSession
    }

    @Test
    fun `TxContext propagerer til child coroutines`() = runTest {
        var innerTxSession: TransactionalSession? = null

        withTransaction {
            val parentContext = coroutineContext
            val txContext = currentCoroutineContext()[TxContext]!!

            launch(parentContext + txContext) {
                innerTxSession = Database.currentTransactionalSession()
            }.join()

            innerTxSession shouldBe Database.currentTransactionalSession()
        }
    }

    @Test
    fun `concurrent transaksjoner gir separate sessions`() = runTest {
        val sessions = mutableSetOf<TransactionalSession>()

        val jobs = List(10) {
            launch {
                withTransaction {
                    sessions.add(Database.currentTransactionalSession())
                }
            }
        }
        jobs.joinAll()

        sessions.size shouldBe 10
    }
}
