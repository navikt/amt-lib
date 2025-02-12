package no.nav.amt.lib.models.deltakerliste.tiltakstype.kafka

import no.nav.amt.lib.models.deltaker.Innsatsgruppe
import no.nav.amt.lib.models.deltakerliste.tiltakstype.DeltakerRegistreringInnhold
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import java.util.UUID

data class TiltakstypeDto(
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakstype.Tiltakskode,
    val arenaKode: String?,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val deltakerRegistreringInnhold: DeltakerRegistreringInnhold?,
) {
    fun toModel(): Tiltakstype = Tiltakstype(
        id = id,
        navn = navn,
        tiltakskode = tiltakskode,
        arenaKode = arenaKode?.let { Tiltakstype.ArenaKode.valueOf(arenaKode) } ?: tiltakskode.toArenaKode(),
        innsatsgrupper = innsatsgrupper,
        innhold = deltakerRegistreringInnhold,
    )
}
