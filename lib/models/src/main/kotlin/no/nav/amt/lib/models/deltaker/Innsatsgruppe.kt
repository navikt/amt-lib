package no.nav.amt.lib.models.deltaker

enum class Innsatsgruppe {
    STANDARD_INNSATS,
    SITUASJONSBESTEMT_INNSATS,
    SPESIELT_TILPASSET_INNSATS,
    VARIG_TILPASSET_INNSATS,
    GRADERT_VARIG_TILPASSET_INNSATS,
}

/**
 * Dette er innsatsgruppene som vi får fra OBO, og fra Valp (på tiltakstyper) i dag, men vi har ennå ikke migrert over til å bruke disse,
 * så man må huske å konvertere enumene til første versjon `Innsatsgruppe` foreløpig.
 */
enum class InnsatsgruppeV2 {
    GODE_MULIGHETER,
    TRENGER_VEILEDNING,
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
    JOBBE_DELVIS,
    LITEN_MULIGHET_TIL_A_JOBBE,
}

fun InnsatsgruppeV2.toV1() = when (this) {
    InnsatsgruppeV2.GODE_MULIGHETER -> Innsatsgruppe.STANDARD_INNSATS
    InnsatsgruppeV2.TRENGER_VEILEDNING -> Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
    InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE -> Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
    InnsatsgruppeV2.JOBBE_DELVIS -> Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS
    InnsatsgruppeV2.LITEN_MULIGHET_TIL_A_JOBBE -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
}

fun Innsatsgruppe.toV2() = when (this) {
    Innsatsgruppe.STANDARD_INNSATS -> InnsatsgruppeV2.GODE_MULIGHETER
    Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> InnsatsgruppeV2.TRENGER_VEILEDNING
    Innsatsgruppe.SPESIELT_TILPASSET_INNSATS -> InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE
    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS -> InnsatsgruppeV2.JOBBE_DELVIS
    Innsatsgruppe.VARIG_TILPASSET_INNSATS -> InnsatsgruppeV2.LITEN_MULIGHET_TIL_A_JOBBE
}
