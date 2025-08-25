package no.nav.amt.lib.models.deltaker.internalapis.deltaker.request

import java.time.LocalDate
import java.util.UUID

data class SluttdatoRequest(
    override val endretAv: String,
    override val endretAvEnhet: String,
    override val forslagId: UUID?,
    val sluttdato: LocalDate,
    val begrunnelse: String?,
) : EndringForslagRequest
