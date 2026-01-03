package no.nav.amt.lib.kafka

import io.kotest.matchers.shouldBe
import no.nav.amt.lib.testing.eventually
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConsumerStatusTest {
    @Test
    fun `retries - oker for hver failure`() {
        val sut = ConsumerStatus()
        val expectedRetries = 42

        repeat(42) { sut.incrementRetryCount(tp1) }

        sut.retryCount(tp1) shouldBe expectedRetries
    }

    @Test
    fun `retries - resettes til 0 etter success`() {
        val sut = ConsumerStatus()
        val expectedRetries = 7

        repeat(7) { sut.incrementRetryCount(tp2) }

        sut.retryCount(tp2) shouldBe expectedRetries
        sut.canProcessPartition(tp2) shouldBe false

        sut.resetRetryCount(tp2)

        sut.retryCount(tp2) shouldBe 0
    }

    @Test
    fun `backoffDuration - skal aldri vare storre enn MAX_DELAY`() {
        val sut = ConsumerStatus()

        sut.backoffDuration(25) shouldBe ConsumerStatus.MAX_DELAY
    }

    @Nested
    inner class CanProcessPartitionTests {
        lateinit var sut: ConsumerStatus

        @BeforeEach
        fun setup() {
            sut = ConsumerStatus()
        }

        @Test
        fun `skal returnere true hvis partisjon ikke er i retry`() {
            sut.canProcessPartition(tp1) shouldBe true
        }

        @Test
        fun `skal returnere false hvis partisjon er i retry`() {
            sut.incrementRetryCount(tp1)
            sut.canProcessPartition(tp1) shouldBe false
        }

        @Test
        fun `skal returnere true nar backoff er over`() {
            sut.incrementRetryCount(tp1)
            sut.canProcessPartition(tp1) shouldBe false

            eventually {
                sut.canProcessPartition(tp1) shouldBe true
            }
        }
    }

    companion object {
        private val tp1 = TopicPartition("test", 0)
        private val tp2 = TopicPartition("test", 1)
    }
}
