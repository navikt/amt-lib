package no.nav.amt.lib.models.deltaker.internalapis.deltaker.request

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface EndringRequest {
    val endretAv: String
    val endretAvEnhet: String
}
