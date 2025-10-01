package no.nav.amt.lib.models.hendelse

import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import java.time.LocalDate
import java.util.UUID

data class HendelseDeltaker(
    val id: UUID,
    val personident: String,
    val deltakerliste: Deltakerliste,
    val forsteVedtakFattet: LocalDate?,
    val opprettetDato: LocalDate?,
) {
    data class Deltakerliste(
        val id: UUID,
        val navn: String,
        val arrangor: Arrangor,
        val tiltak: Tiltak,
        val startdato: LocalDate? = null, //Må være nullable fordi de benyttes som dbo i amt-distribusjon
        val sluttdato: LocalDate? = null,
        val oppstartstype: Oppstartstype? = null, //Må være nullable fordi de benyttes som dbo i amt-distribusjon
    ) {
        data class Arrangor(
            val id: UUID,
            val organisasjonsnummer: String,
            val navn: String,
            val overordnetArrangor: Arrangor?,
        )

        data class Tiltak(
            val navn: String,
            val type: ArenaKode,
            val ledetekst: String?,
            val tiltakskode: Tiltakskode,
        )

        enum class Oppstartstype {
            LOPENDE,
            FELLES,
        }
    }
}
