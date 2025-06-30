package no.nav.amt.lib.kafka

interface Consumer<K, V> {
    suspend fun consume(key: K, value: V)

    fun start()
}
