package no.nav.amt.lib.kafka

import kotlinx.coroutines.Job

interface Consumer<K, V> {
    suspend fun consume(key: K, value: V)

    @Deprecated("Use start() instead.", ReplaceWith("start()"))
    fun run(): Job

    fun start()
}
