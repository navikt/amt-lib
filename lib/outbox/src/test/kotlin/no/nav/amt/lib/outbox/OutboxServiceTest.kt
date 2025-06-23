package no.nav.amt.lib.outbox

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

data class Nested(
    val inner: String,
)

data class LargeValue(
    val list: List<Int>,
    val nested: Nested,
)

class OutboxServiceTest {
    init {
        SingletonPostgres16Container
    }

    private lateinit var repository: OutboxRepository
    private lateinit var service: OutboxService

    @BeforeEach
    fun setup() {
        repository = OutboxRepository()
        service = OutboxService()
    }

    data class TestValue(
        val foo: String,
        val bar: Int,
    )

    @Test
    fun `newEvent creates and persists an event with correct fields`() {
        val value = TestValue("hello", 42)
        val key = UUID.randomUUID()
        val topic = "test-topic"

        val event = service.newEvent(key, value, topic)

        event.id shouldNotBe null
        event.key shouldBe key.toString()
        event.valueType shouldBe TestValue::class.simpleName
        event.topic shouldBe topic
        event.value["foo"].asText() shouldBe value.foo
        event.value["bar"].asInt() shouldBe value.bar

        val persisted = repository.get(event.id)!!
        persisted.key shouldBe key.toString()
        persisted.valueType shouldBe TestValue::class.simpleName
        persisted.topic shouldBe topic
        persisted.value.get("foo")?.asText() shouldBe value.foo
        persisted.value.get("bar")?.asInt() shouldBe value.bar
    }

    @Test
    fun `newEvent handles special characters in value fields`() {
        data class SpecialCharValue(
            val foo: String,
        )

        val specialString = "Hello, 世界! \"quotes\" \n new line"
        val value = SpecialCharValue(specialString)
        val key = UUID.randomUUID()
        val topic = "special-char-topic"

        val event = service.newEvent(key, value, topic)
        event.value["foo"].asText() shouldBe specialString
    }

    @Test
    fun `newEvent works with large and nested values`() {
        val value = LargeValue(List(1000) { it }, Nested("deep"))
        val key = UUID.randomUUID()
        val topic = "large-topic"

        val event = service.newEvent(key, value, topic)
        event.value["list"].size() shouldBe 1000
        event.value["nested"]["inner"].asText() shouldBe "deep"
    }
}
