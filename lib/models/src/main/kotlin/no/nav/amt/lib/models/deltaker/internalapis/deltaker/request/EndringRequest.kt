package no.nav.amt.lib.models.deltaker.internalapis.deltaker.request

sealed interface EndringRequest {
    val endretAv: String
    val endretAvEnhet: String
}
