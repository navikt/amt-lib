package no.nav.amt.lib.models.hendelse

import java.time.LocalDateTime
import java.util.UUID

data class Hendelse(
    val id: UUID,
    val opprettet: LocalDateTime,
    val deltaker: HendelseDeltaker,
    val ansvarlig: HendelseAnsvarlig,
    val payload: HendelseType,
)
