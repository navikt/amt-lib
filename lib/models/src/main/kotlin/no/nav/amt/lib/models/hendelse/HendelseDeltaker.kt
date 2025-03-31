package no.nav.amt.lib.models.hendelse

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.time.LocalDate
import java.util.UUID

data class HendelseDeltaker(
    val id: UUID,
    val personident: String,
    val deltakerliste: Deltakerliste,
    val forsteVedtakFattet: LocalDate?,
) {
    data class Deltakerliste(
        val id: UUID,
        val navn: String,
        val arrangor: Arrangor,
        val tiltak: Tiltak,
        val oppstart: Oppstartstype,
    ) {
        data class Arrangor(
            val id: UUID,
            val organisasjonsnummer: String,
            val navn: String,
            val overordnetArrangor: Arrangor?,
        )

        data class Tiltak(
            val navn: String,
            val type: Tiltakstype.ArenaKode,
            val ledetekst: String?,
            val tiltakskode: Tiltakstype.Tiltakskode,
        )

        enum class Oppstartstype {
            LOPENDE,
            FELLES,
        }
    }
}
