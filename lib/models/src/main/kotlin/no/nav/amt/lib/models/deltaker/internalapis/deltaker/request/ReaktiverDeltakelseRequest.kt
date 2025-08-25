package no.nav.amt.lib.models.deltaker.internalapis.deltaker.request

data class ReaktiverDeltakelseRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    val begrunnelse: String,
) : EndringRequest
