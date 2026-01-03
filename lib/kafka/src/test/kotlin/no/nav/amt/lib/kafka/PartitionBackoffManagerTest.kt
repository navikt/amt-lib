package no.nav.amt.lib.kafka

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.testing.eventually
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PartitionBackoffManagerTest {
    companion object {
        private lateinit var sut: PartitionBackoffManager

        private val tp1 = TopicPartition("test", 0)
        private val tp2 = TopicPartition("test", 1)
    }

    @BeforeEach
    fun setup() {
        sut = PartitionBackoffManager()
    }

    @Test
    fun `incrementRetryCount håndterer flere partisjoner uavhengig`() {
        sut.incrementRetryCount(tp1)
        sut.incrementRetryCount(tp2)
        sut.incrementRetryCount(tp2)

        sut.getRetryCount(tp1) shouldBe 1
        sut.getRetryCount(tp2) shouldBe 2
    }

    @Test
    fun `resetRetryCount pa en uregistrert partisjon kaster ikke feil`() {
        shouldNotThrowAny {
            sut.resetRetryCount(tp1)
        }
    }

    @Nested
    inner class IsInBackoffTests {
        @Test
        fun `skal returnere false hvis partisjon ikke er i backoff`() {
            sut.isInBackoff(tp1) shouldBe false
        }

        @Test
        fun `skal returnere true etter incrementRetryCount`() {
            sut.incrementRetryCount(tp1)
            sut.isInBackoff(tp1) shouldBe true
        }

        @Test
        fun `skal returnere false etter at backoff er over`() {
            sut.incrementRetryCount(tp1)
            sut.isInBackoff(tp1) shouldBe true

            eventually {
                sut.isInBackoff(tp1) shouldBe false
            }
        }

        @Test
        fun `skal returnere false etter resetRetryCount`() {
            sut.incrementRetryCount(tp1)
            sut.isInBackoff(tp1) shouldBe true

            sut.resetRetryCount(tp1)
            sut.isInBackoff(tp1) shouldBe false
        }
    }

    @Nested
    inner class BackoffTimingTests {
        @Test
        fun `backoff varer minst BASE_DELAY`() {
            sut.incrementRetryCount(tp1)

            val backoffUntil = sut.calculateBackoffUntilMs(1)

            backoffUntil - System.currentTimeMillis() shouldBeGreaterThanOrEqual 1_000L
        }

        @Test
        fun `store retryCounts gir MAX_DELAY`() {
            val backoffUntil = sut.calculateBackoffUntilMs(10_000)

            backoffUntil - System.currentTimeMillis() shouldBeLessThanOrEqual PartitionBackoffManager.MAX_DELAY
        }

        @Test
        fun `calculateBackoffUntilMs aldri storre enn MAX_DELAY`() {
            val backoff = sut.calculateBackoffUntilMs(1000)
            val now = System.currentTimeMillis()

            backoff - now shouldBeLessThanOrEqual PartitionBackoffManager.MAX_DELAY
        }

        @Test
        fun `calculateBackoffUntilMs vokser med retryCount`() {
            val backoff1 = sut.calculateBackoffUntilMs(1)
            val backoff2 = sut.calculateBackoffUntilMs(2)

            backoff2 shouldBeGreaterThan backoff1
        }
    }
}
