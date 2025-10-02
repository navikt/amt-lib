package no.nav.amt.lib.models.deltakerliste.tiltakstype

data class Tiltak(
    val navn: String,
    val type: ArenaKode,
    val ledetekst: String?,
    val tiltakskode: Tiltakskode,
)