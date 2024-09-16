package no.nav.amt.lib.models.deltaker

data class Deltakelsesinnhold(
    val ledetekst: String?,
    val innhold: List<Innhold>,
)

data class Innhold(
    val tekst: String,
    val innholdskode: String,
    val valgt: Boolean,
    val beskrivelse: String?,
)
