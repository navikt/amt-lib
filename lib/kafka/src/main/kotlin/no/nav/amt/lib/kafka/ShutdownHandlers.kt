package no.nav.amt.lib.kafka

data class ShutdownHandlers(
    val shutdownProducers: () -> Unit,
    val shutdownConsumers: suspend () -> Unit,
)
