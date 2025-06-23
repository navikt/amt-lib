package no.nav.amt.lib.outbox

import no.nav.amt.lib.utils.objectMapper
import java.util.UUID

class OutboxService {
    private val repository = OutboxRepository()

    fun <T : Any> newEvent(
        aggregateId: UUID,
        aggregate: T,
        topic: String,
    ): OutboxEvent {
        val event = NewOutboxEvent(
            aggregateId = aggregateId.toString(),
            aggregateType = aggregate::class.java.simpleName,
            topic = topic,
            payload = objectMapper.readTree(objectMapper.writeValueAsString(aggregate)),
        )
        return repository.insertNewEvent(event)
    }

    fun findUnprocessedEvents(limit: Int): List<OutboxEvent> = repository.findUnprocessedEvents(limit)

    fun markAsProcessed(id: OutboxEventId) = repository.markAsProcessed(id)

    fun markAsFailed(id: OutboxEventId, errorMessage: String) = repository.markAsFailed(id, errorMessage)
}
