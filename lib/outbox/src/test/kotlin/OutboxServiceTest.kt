import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.lib.OutboxRepository
import no.nav.amt.lib.OutboxService
import no.nav.amt.lib.testing.SingletonPostgres16Container
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

data class Nested(
    val inner: String,
)

data class LargeAggregate(
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
        service = OutboxService(repository)
    }

    data class TestAggregate(
        val foo: String,
        val bar: Int,
    )

    @Test
    fun `newEvent creates and persists an event with correct fields`() {
        val aggregate = TestAggregate("hello", 42)
        val aggregateId = UUID.randomUUID()
        val topic = "test-topic"

        val event = service.newEvent(aggregateId, aggregate, topic)

        event.id shouldNotBe null
        event.aggregateId shouldBe aggregateId.toString()
        event.aggregateType shouldBe TestAggregate::class.simpleName
        event.topic shouldBe topic
        event.payload["foo"].asText() shouldBe aggregate.foo
        event.payload["bar"].asInt() shouldBe aggregate.bar

        val persisted = repository.get(event.id!!)!!
        persisted.aggregateId shouldBe aggregateId.toString()
        persisted.aggregateType shouldBe TestAggregate::class.simpleName
        persisted.topic shouldBe topic
        persisted.payload.get("foo")?.asText() shouldBe aggregate.foo
        persisted.payload.get("bar")?.asInt() shouldBe aggregate.bar
    }

    @Test
    fun `newEvent handles special characters in aggregate fields`() {
        data class SpecialCharAggregate(
            val foo: String,
        )

        val specialString = "Hello, 世界! \"quotes\" \n new line"
        val aggregate = SpecialCharAggregate(specialString)
        val aggregateId = UUID.randomUUID()
        val topic = "special-char-topic"

        val event = service.newEvent(aggregateId, aggregate, topic)
        event.payload["foo"].asText() shouldBe specialString
    }

    @Test
    fun `newEvent works with large and nested aggregates`() {
        val aggregate = LargeAggregate(List(1000) { it }, Nested("deep"))
        val aggregateId = UUID.randomUUID()
        val topic = "large-topic"

        val event = service.newEvent(aggregateId, aggregate, topic)
        event.payload["list"].size() shouldBe 1000
        event.payload["nested"]["inner"].asText() shouldBe "deep"
    }
}
