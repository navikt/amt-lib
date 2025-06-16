package no.nav.amt.lib.utils.database

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotliquery.queryOf
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

class DatabaseTest {
    init {
        SingletonPostgres16Container
    }

    @Test
    fun `transaksjon rulles tilbake ved feil`() = runTest {
        val r = DatabaseTestRepository()
        val n = 1

        try {
            Database.transaction {
                r.insert(n)
                throw IllegalStateException("Skal rulle tilbake insert")
            }
        } catch (_: IllegalStateException) {
            println("Feil etter insert")
        }

        r.get(n) shouldBe null
    }

    @Test
    fun `transaksjon committes uten feil`() = runTest {
        val r = DatabaseTestRepository()
        val n = 999

        Database.transaction {
            r.insert(n)
        }

        r.get(n) shouldBe n
    }

    @Test
    fun `det er ikke mulig å bruke transaksjoner i en transaksjon`() = runTest {
        val r = DatabaseTestRepository()

        shouldThrow<PSQLException> {
            Database.transaction {
                Database.query { s ->
                    s.transaction {
                        r.insert(43)
                    }
                }
            }
        }
    }
}

private class DatabaseTestRepository {
    init {
        try {
            Database.query {
                it.update(queryOf("create table foo (id int primary key)"))
            }
        } catch (_: Throwable) {
        }
    }

    fun insert(n: Int) = Database.query {
        it.update(queryOf("insert into foo (id) values (?)", n))
    }

    fun get(n: Int) = Database.query {
        val q = queryOf("select * from foo where id = ?", n).map { r -> r.int("id") }.asSingle
        it.run(q)
    }
}
